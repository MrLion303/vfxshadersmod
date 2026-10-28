package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import com.mrlion303.vfxshadersmod.util.VfxColors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Efectos de una sola ejecución, incluidos los pulsos físicos temporales del terreno. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxOnceCommands {
    private static final float DEFAULT_TRAMPOLIN_POWER = 12.0F;
    private static final float DEFAULT_TRAMPOLIN_RADIUS = 32.0F;
    private static final float DEFAULT_TRAMPOLIN_SPEED = 0.65F;
    private static final List<ActiveTrampolineWave> ACTIVE_TRAMPOLIN_WAVES = new ArrayList<>();

    private VfxOnceCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxonce")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("scan")
                        .then(Commands.argument("position", Vec3Argument.vec3())
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                        VfxColors.names(), builder))
                                        .executes(VfxOnceCommands::scan))))
                .then(Commands.literal("trampolin")
                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                .executes(VfxOnceCommands::trampolineDefault)
                                .then(Commands.argument("potencia", FloatArgumentType.floatArg(1.0F, 64.0F))
                                        .executes(VfxOnceCommands::trampolinePower)
                                        .then(Commands.argument("radio", FloatArgumentType.floatArg(4.0F, 64.0F))
                                                .executes(VfxOnceCommands::trampolinePowerAndRadius)
                                                .then(Commands.argument("velocidad", FloatArgumentType.floatArg(0.15F, 2.0F))
                                                        .executes(VfxOnceCommands::trampolineCustom))))))
                .then(Commands.literal("confeti")
                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                .executes(context -> photonBurst(context, "confeti"))))
                .then(Commands.literal("llamas")
                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                .executes(context -> photonBurst(context, "llamas")))));
    }

    private static int photonBurst(CommandContext<CommandSourceStack> context, String effect) {
        Vec3 position = Vec3Argument.getVec3(context, "posicion");
        ServerLevel level = context.getSource().getLevel();
        VfxNetwork.sendToDimension(level, new VfxCommandPacket(
                VfxCommandPacket.PHOTON_ONCE, effect, level.dimension().location(), 0.0F, position));
        String label = "confeti".equals(effect) ? "Confeti" : "Llamarada de chispas";
        context.getSource().sendSuccess(() -> Component.literal(
                label + " Photon lanzado desde (" + fmt(position.x) + ", "
                        + fmt(position.y) + ", " + fmt(position.z) + ")."), true);
        return 1;
    }

    private static int scan(CommandContext<CommandSourceStack> context) {
        String colorName = StringArgumentType.getString(context, "color");
        if (VfxColors.parse(colorName) == null) {
            context.getSource().sendFailure(Component.literal("Color inválido. " + VfxColors.describeOptions()));
            return 0;
        }

        Vec3 position = Vec3Argument.getVec3(context, "position");
        ServerLevel level = context.getSource().getLevel();
        VfxNetwork.sendToDimension(level, new VfxCommandPacket(
                VfxCommandPacket.SCAN_ONCE,
                "scan",
                level.dimension().location(),
                0.0F,
                position,
                colorName
        ));
        context.getSource().sendSuccess(() -> Component.literal(
                "Escaneo iniciado desde (" + fmt(position.x) + ", " + fmt(position.y) + ", "
                        + fmt(position.z) + ") en color " + colorName + "."), true);
        return 1;
    }

    private static int trampolineDefault(CommandContext<CommandSourceStack> context) {
        return startTrampoline(context, DEFAULT_TRAMPOLIN_POWER,
                DEFAULT_TRAMPOLIN_RADIUS, DEFAULT_TRAMPOLIN_SPEED);
    }

    private static int trampolinePower(CommandContext<CommandSourceStack> context) {
        return startTrampoline(context, FloatArgumentType.getFloat(context, "potencia"),
                DEFAULT_TRAMPOLIN_RADIUS, DEFAULT_TRAMPOLIN_SPEED);
    }

    private static int trampolinePowerAndRadius(CommandContext<CommandSourceStack> context) {
        return startTrampoline(context, FloatArgumentType.getFloat(context, "potencia"),
                FloatArgumentType.getFloat(context, "radio"), DEFAULT_TRAMPOLIN_SPEED);
    }

    private static int trampolineCustom(CommandContext<CommandSourceStack> context) {
        return startTrampoline(context, FloatArgumentType.getFloat(context, "potencia"),
                FloatArgumentType.getFloat(context, "radio"),
                FloatArgumentType.getFloat(context, "velocidad"));
    }

    private static int startTrampoline(
            CommandContext<CommandSourceStack> context, float power, float radius, float speed
    ) {
        Vec3 origin = Vec3Argument.getVec3(context, "posicion");
        ServerLevel level = context.getSource().getLevel();

        // La potencia indica la altura aproximada del salto en bloques.
        ACTIVE_TRAMPOLIN_WAVES.add(new ActiveTrampolineWave(level, origin, power, radius, speed));
        String clientOptions = Float.toString(radius) + ";" + Float.toString(speed);
        VfxNetwork.sendToDimension(level, new VfxCommandPacket(
                VfxCommandPacket.TRAMPOLIN_ONCE,
                "trampolin",
                level.dimension().location(),
                power,
                origin,
                clientOptions
        ));

        context.getSource().sendSuccess(() -> Component.literal(String.format(
                java.util.Locale.ROOT,
                "Onda de trampolín iniciada en (%.1f, %.1f, %.1f): altura %.1f bloques, radio %.1f, velocidad %.2f bloques/tick.",
                origin.x, origin.y, origin.z, power, radius, speed)), true);
        return 1;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        Iterator<ActiveTrampolineWave> iterator = ACTIVE_TRAMPOLIN_WAVES.iterator();
        while (iterator.hasNext()) {
            ActiveTrampolineWave wave = iterator.next();
            if (wave.level != level) {
                continue;
            }
            if (wave.tick()) {
                iterator.remove();
            }
        }
    }

    private static boolean isGroundedNearSurface(ServerPlayer player, ServerLevel level) {
        if (player.isSpectator() || player.getAbilities().flying
                || player.isFallFlying() || player.isPassenger()) {
            return false;
        }
        if (player.onGround()) {
            return true;
        }

        BlockPos blockPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        if (!level.hasChunkAt(blockPos) || player.getDeltaMovement().y > 0.0D) {
            return false;
        }
        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos.getX(), blockPos.getZ());
        return player.getY() >= surfaceY - 0.35D && player.getY() <= surfaceY + 1.1D;
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static final class ActiveTrampolineWave {
        private static final double MINECRAFT_GRAVITY = 0.08D;
        private final ServerLevel level;
        private final Vec3 origin;
        private final double power;
        private final double radius;
        private final double speed;
        private final Set<UUID> launchedPlayers = new HashSet<>();
        private long age;

        private ActiveTrampolineWave(
                ServerLevel level, Vec3 origin, double power, double radius, double speed
        ) {
            this.level = level;
            this.origin = origin;
            this.power = power;
            this.radius = radius;
            this.speed = speed;
        }

        /** Devuelve true cuando el frente ha rebasado el radio configurado. */
        private boolean tick() {
            double previousFront = age * speed;
            age++;
            double currentFront = age * speed;
            double waveThickness = Math.max(0.9D, speed * 1.35D);
            double launchVelocity = Math.sqrt(2.0D * MINECRAFT_GRAVITY * power);

            for (ServerPlayer player : level.players()) {
                UUID playerId = player.getUUID();
                if (launchedPlayers.contains(playerId)) {
                    continue;
                }

                double dx = player.getX() - origin.x;
                double dz = player.getZ() - origin.z;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                if (horizontalDistance > radius
                        || horizontalDistance > currentFront + waveThickness
                        || horizontalDistance < Math.max(0.0D, previousFront - waveThickness)) {
                    continue;
                }
                if (!isGroundedNearSurface(player, level)) {
                    continue;
                }

                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, Math.max(motion.y, launchVelocity), motion.z);
                player.fallDistance = 0.0F;
                player.hurtMarked = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                launchedPlayers.add(playerId);
            }

            return currentFront >= radius + waveThickness;
        }
    }
}
