package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Efectos de movimiento por jugador que se controlan desde el servidor. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxMoveCommands {
    private static final double EMPUJON_HORIZONTAL = 1.65D;
    private static final double EMPUJON_VERTICAL = 1.0D;
    private static final double GRAVEDAD_LUNAR_COMPENSACION = 0.052D;
    private static final Set<UUID> MOONJUMP = new HashSet<>();
    private static final Set<UUID> WATER_WALK = new HashSet<>();
    private static final Map<UUID, Long> VOLAR_HASTA = new HashMap<>();

    private VfxMoveCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("vfxmove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("empujon")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(VfxMoveCommands::empujon)))
                .then(Commands.literal("moonjump")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, true, true))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, true, false)))))
                .then(Commands.literal("waterwalk")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, false, true))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, false, false)))))
                .then(Commands.literal("caminar_agua")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, false, true))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setEffect(context, false, false)))))
                .then(Commands.literal("volar")
                        .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.1F, 3600.0F))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(VfxMoveCommands::volar)))));
    }

    private static int empujon(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("No se encontraron jugadores válidos."));
            return 0;
        }

        for (ServerPlayer player : targets) {
            Vec3 look = player.getLookAngle();
            Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
            if (horizontalLook.lengthSqr() < 1.0E-6D) {
                horizontalLook = Vec3.directionFromRotation(0.0F, player.getYRot());
                horizontalLook = new Vec3(horizontalLook.x, 0.0D, horizontalLook.z);
            }

            Vec3 backward = horizontalLook.normalize().scale(-EMPUJON_HORIZONTAL);
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(
                    current.x + backward.x,
                    Math.max(0.0D, current.y) + EMPUJON_VERTICAL,
                    current.z + backward.z
            );
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("Empujón aplicado a " + count + (count == 1 ? " jugador." : " jugadores.")),
                true
        );
        return count;
    }

    private static int setEffect(CommandContext<CommandSourceStack> context, boolean moonjump, boolean enabled) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("No se encontraron jugadores válidos."));
            return 0;
        }

        Set<UUID> selected = moonjump ? MOONJUMP : WATER_WALK;
        for (ServerPlayer player : targets) {
            if (enabled) {
                selected.add(player.getUUID());
            } else {
                selected.remove(player.getUUID());
            }
            sendClientState(player, moonjump ? "moonjump" : "waterwalk", enabled);
            if (!enabled && !moonjump) {
                player.fallDistance = 0.0F;
            }
        }

        String effect = moonjump ? "salto lunar" : "caminar sobre el agua";
        String action = enabled ? "activado" : "desactivado";
        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("Efecto de " + effect + " " + action + " para "
                        + count + (count == 1 ? " jugador." : " jugadores.")),
                true
        );
        return count;
    }

    private static void sendClientState(ServerPlayer player, String effect, boolean enabled) {
        VfxNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new VfxCommandPacket(
                        VfxCommandPacket.PLAYER_EFFECT,
                        effect,
                        player.level().dimension().location(),
                        0.0F,
                        player.position(),
                        enabled ? "enable" : "disable"
                )
        );
    }

    private static int volar(CommandContext<CommandSourceStack> context) {
        float seconds = FloatArgumentType.getFloat(context, "seconds");
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("No se encontraron jugadores válidos."));
            return 0;
        }

        long now = context.getSource().getServer().overworld().getGameTime();
        long durationTicks = Math.max(2L, (long) Math.ceil(seconds * 20.0D));
        for (ServerPlayer player : targets) {
            VOLAR_HASTA.put(player.getUUID(), now + durationTicks);
            sendVolar(player, true, seconds);
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("Animación de vuelo activada durante " + seconds + " s para "
                        + count + (count == 1 ? " jugador." : " jugadores.")),
                true
        );
        return count;
    }

    private static void sendVolar(ServerPlayer player, boolean enabled, float seconds) {
        sendVolarToLevel(player.serverLevel(), player, enabled, seconds);
    }

    private static void sendVolarToLevel(ServerLevel level, ServerPlayer player, boolean enabled, float seconds) {
        VfxNetwork.sendToDimension(level, new VfxCommandPacket(
                VfxCommandPacket.PLAYER_ANIMATION, "volar", level.dimension().location(), seconds,
                new Vec3(player.getId(), 0.0D, 0.0D), enabled ? "enable" : "disable"));
    }

    private static void sendVolarToClient(ServerPlayer recipient, ServerPlayer target, float seconds) {
        VfxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient),
                new VfxCommandPacket(VfxCommandPacket.PLAYER_ANIMATION, "volar",
                        target.serverLevel().dimension().location(), seconds,
                        new Vec3(target.getId(), 0.0D, 0.0D), "enable"));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.level().isClientSide) {
            return;
        }

        UUID id = player.getUUID();
        Long flyingUntil = VOLAR_HASTA.get(id);
        long serverTime = player.getServer().overworld().getGameTime();
        if (flyingUntil != null && serverTime >= flyingUntil) {
            VOLAR_HASTA.remove(id);
            sendVolar(player, false, 0.0F);
        }

        if (MOONJUMP.contains(id) && !player.getAbilities().flying && !player.isFallFlying()
                && !player.onGround()) {
            Vec3 velocity = player.getDeltaMovement();
            // Solo se modifica el eje vertical. No se fuerza una actualización de velocidad cada tick.
            player.setDeltaMovement(
                    velocity.x,
                    velocity.y + GRAVEDAD_LUNAR_COMPENSACION,
                    velocity.z
            );
        }

        if (WATER_WALK.contains(id)) {
            keepPlayerOnWater(player);
        }
    }

    /** Mantiene los pies en la superficie sin cancelar el salto ni reiniciar la velocidad horizontal. */
    private static void keepPlayerOnWater(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > 0.06D) {
            return;
        }

        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.02D, player.getZ());
        BlockPos waterPos = feet;
        FluidState fluid = player.level().getFluidState(waterPos);
        if (!fluid.is(FluidTags.WATER)) {
            waterPos = feet.below();
            fluid = player.level().getFluidState(waterPos);
        }
        if (!fluid.is(FluidTags.WATER)) {
            return;
        }

        double surfaceY = waterPos.getY() + fluid.getHeight(player.level(), waterPos);
        if (player.getY() > surfaceY + 0.025D || player.getY() < waterPos.getY() - 0.15D) {
            return;
        }

        // Corrige solo el hundimiento; si ya está sobre la superficie no lo teletransporta cada tick.
        if (player.getY() < surfaceY - 0.025D) {
            player.setPos(player.getX(), surfaceY, player.getZ());
        }
        if (motion.y < 0.0D) {
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining) || joining.getServer() == null) return;
        long now = joining.getServer().overworld().getGameTime();
        for (ServerPlayer target : joining.getServer().getPlayerList().getPlayers()) {
            if (target.serverLevel() != joining.serverLevel()) continue;
            Long expires = VOLAR_HASTA.get(target.getUUID());
            if (expires != null && expires > now) {
                sendVolarToClient(joining, target, (expires - now) / 20.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
        Long expires = VOLAR_HASTA.get(player.getUUID());
        long now = player.getServer().overworld().getGameTime();
        if (expires != null && expires > now) sendVolar(player, true, (expires - now) / 20.0F);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
        ServerLevel oldLevel = player.getServer().getLevel(event.getFrom());
        if (oldLevel != null) sendVolarToLevel(oldLevel, player, false, 0.0F);
        Long expires = VOLAR_HASTA.get(player.getUUID());
        long now = player.getServer().overworld().getGameTime();
        if (expires != null && expires > now) sendVolar(player, true, (expires - now) / 20.0F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer player && VOLAR_HASTA.remove(id) != null) {
            sendVolar(player, false, 0.0F);
        }
        MOONJUMP.remove(id);
        WATER_WALK.remove(id);
    }
}
