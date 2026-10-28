package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Comandos de cámara de una sola orden, animados en el cliente de cada objetivo. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxCameraCommands {
    private static final float MAX_SECONDS = 30.0F;
    private static final Map<UUID, CameraMode> MODES = new HashMap<>();
    private VfxCameraCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxcamara")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hombro")
                        .executes(ctx -> toggleShoulder(ctx, self(ctx)))
                        .then(Commands.argument("objetivos", EntityArgument.entities())
                                .executes(ctx -> toggleShoulder(ctx, EntityArgument.getEntities(ctx, "objetivos")))))
                .then(Commands.literal("fija")
                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                .then(Commands.argument("tiempo", StringArgumentType.word())
                                        .then(Commands.argument("direccion", StringArgumentType.string())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("arriba", "abajo", "norte", "sur", "este", "oeste",
                                                                "noreste", "noroeste", "sureste", "suroeste",
                                                                "abajo,sur", "arriba,norte"), builder))
                                                .executes(ctx -> fix(ctx, self(ctx)))
                                                .then(Commands.argument("objetivos", EntityArgument.entities())
                                                        .executes(ctx -> fix(ctx,
                                                                EntityArgument.getEntities(ctx, "objetivos"))))))))
                .then(Commands.literal("fijadesactivar")
                        .then(Commands.argument("tiempo", StringArgumentType.word())
                                .executes(ctx -> deactivate(ctx, self(ctx)))
                                .then(Commands.argument("objetivos", EntityArgument.entities())
                                        .executes(ctx -> deactivate(ctx,
                                                EntityArgument.getEntities(ctx, "objetivos")))))));
    }

    private static Collection<? extends Entity> self(CommandContext<CommandSourceStack> ctx) {
        Entity source = ctx.getSource().getEntity();
        return source instanceof ServerPlayer ? List.of(source) : List.of();
    }

    private static Float transition(CommandContext<CommandSourceStack> ctx) {
        String value = StringArgumentType.getString(ctx, "tiempo").toLowerCase(Locale.ROOT);
        if ("false".equals(value)) return -1.0F;
        try {
            float seconds = Float.parseFloat(value);
            return Float.isFinite(seconds) && seconds >= 0.0F && seconds <= MAX_SECONDS
                    ? seconds : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int toggleShoulder(CommandContext<CommandSourceStack> ctx,
                                      Collection<? extends Entity> targets) {
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Indica jugadores; desde el chat puedes usar /vfxcamara hombro @s."));
            return 0;
        }
        int count = 0, enabled = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof ServerPlayer player)) continue;
            if (MODES.get(player.getUUID()) == CameraMode.SHOULDER) {
                MODES.remove(player.getUUID());
                send(player, "return", 0.42F, player.position(), "");
            } else {
                MODES.put(player.getUUID(), CameraMode.SHOULDER);
                send(player, "shoulder", 0.42F, player.position(), "");
                enabled++;
            }
            count++;
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal("El selector no encontró jugadores."));
            return 0;
        }
        String state = enabled == count ? "activada" : enabled == 0 ? "desactivada" : "alternada";
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Vista de hombro " + state + " para " + count + (count == 1 ? " jugador." : " jugadores.")), true);
        return count;
    }

    private static int fix(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) {
        Vec3 position = Vec3Argument.getVec3(ctx, "posicion");
        String direction = StringArgumentType.getString(ctx, "direccion");
        if (VfxCameraDirection.parse(direction).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(
                    "Dirección inválida. Usa arriba, abajo, un rumbo cardinal o una combinación como \"abajo, sur\"."));
            return 0;
        }
        Float seconds = transition(ctx);
        if (seconds == null) {
            ctx.getSource().sendFailure(Component.literal("El tiempo debe ser false o un número entre 0 y 30 segundos."));
            return 0;
        }
        int count = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof ServerPlayer player)) continue;
            MODES.put(player.getUUID(), CameraMode.FIXED);
            send(player, "fixed", seconds, position, direction);
            count++;
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal("Indica uno o más jugadores con el selector."));
            return 0;
        }
        String timing = seconds < 0.0F || seconds == 0.0F ? "instantáneamente" : "en " + seconds + " segundos";
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Cámara fijada en (" + fmt(position.x) + ", " + fmt(position.y) + ", " + fmt(position.z)
                        + "), mirando hacia " + direction + ", " + timing + ". Objetivos: " + count + "."), true);
        return count;
    }

    private static int deactivate(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) {
        Float seconds = transition(ctx);
        if (seconds == null) {
            ctx.getSource().sendFailure(Component.literal("El tiempo debe ser false o un número entre 0 y 30 segundos."));
            return 0;
        }
        int count = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof ServerPlayer player)) continue;
            MODES.remove(player.getUUID());
            send(player, "return", seconds, player.position(), "");
            count++;
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal("Indica uno o más jugadores con el selector."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Cámara devuelta al jugador para "
                + count + (count == 1 ? " jugador." : " jugadores.")), true);
        return count;
    }

    private static void send(ServerPlayer player, String mode, float seconds, Vec3 position, String option) {
        VfxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new VfxCommandPacket(VfxCommandPacket.CAMERA_CONTROL, mode,
                        player.serverLevel().dimension().location(), seconds, position, option));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MODES.remove(event.getEntity().getUUID());
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
    private enum CameraMode { SHOULDER, FIXED }
}
