package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Efectos visuales ligados a jugadores concretos y sincronizados a los clientes de su dimensión. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxPlayerCommands {
    private static final Map<UUID, Set<String>> ACTIVE = new HashMap<>();

    private VfxPlayerCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxplayer")
                .requires(source -> source.hasPermission(2))
                .then(effectBranch("ondas"))
                .then(effectBranch("aura")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> effectBranch(String effect) {
        return Commands.literal(effect)
                .then(Commands.literal("enable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setEffect(context, effect, true))))
                .then(Commands.literal("disable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setEffect(context, effect, false))));
    }

    private static int setEffect(CommandContext<CommandSourceStack> context, String effect, boolean enabled) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("No se encontraron jugadores válidos."));
            return 0;
        }

        for (ServerPlayer target : targets) {
            Set<String> effects = ACTIVE.computeIfAbsent(target.getUUID(), ignored -> new HashSet<>());
            if (enabled) effects.add(effect);
            else effects.remove(effect);
            if (effects.isEmpty()) ACTIVE.remove(target.getUUID());
            sendToDimension(target.serverLevel(), target, effect, enabled);
        }

        int count = targets.size();
        String action = enabled ? "activado" : "desactivado";
        String label = effect.equals("ondas") ? "Ondas" : "Aura";
        context.getSource().sendSuccess(
                () -> Component.literal(label + " " + action + " para "
                        + count + (count == 1 ? " jugador." : " jugadores.")),
                true
        );
        return count;
    }

    private static VfxCommandPacket packet(ServerLevel destination, ServerPlayer target, String effect, boolean enabled) {
        return new VfxCommandPacket(VfxCommandPacket.PLAYER_VFX, effect, destination.dimension().location(),
                0.0F, new Vec3(target.getId(), 0.0D, 0.0D), enabled ? "enable" : "disable");
    }

    private static void sendToDimension(ServerLevel destination, ServerPlayer target, String effect, boolean enabled) {
        VfxNetwork.sendToDimension(destination, packet(destination, target, effect, enabled));
    }

    private static void sendToClient(ServerPlayer recipient, ServerPlayer target, String effect, boolean enabled) {
        VfxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient),
                packet(target.serverLevel(), target, effect, enabled));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining) || joining.getServer() == null) return;
        for (ServerPlayer target : joining.getServer().getPlayerList().getPlayers()) {
            if (target.serverLevel() != joining.serverLevel()) continue;
            for (String effect : ACTIVE.getOrDefault(target.getUUID(), Set.of())) {
                sendToClient(joining, target, effect, true);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        for (String effect : ACTIVE.getOrDefault(target.getUUID(), Set.of())) {
            sendToDimension(target.serverLevel(), target, effect, true);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target) || target.getServer() == null) return;
        ServerLevel oldLevel = target.getServer().getLevel(event.getFrom());
        Set<String> effects = ACTIVE.getOrDefault(target.getUUID(), Set.of());
        if (oldLevel != null) {
            for (String effect : effects) sendToDimension(oldLevel, target, effect, false);
        }
        for (String effect : effects) sendToDimension(target.serverLevel(), target, effect, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            ACTIVE.remove(event.getEntity().getUUID());
            return;
        }
        Set<String> effects = ACTIVE.remove(target.getUUID());
        if (effects != null) {
            for (String effect : effects) sendToDimension(target.serverLevel(), target, effect, false);
        }
    }
}
