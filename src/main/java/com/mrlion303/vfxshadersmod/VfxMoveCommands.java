package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Efectos de movimiento por jugador que se controlan desde el servidor. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxMoveCommands {
    private static final double EMPUJON_HORIZONTAL = 1.65D;
    private static final double EMPUJON_VERTICAL = 1.0D;
    private static final Set<UUID> MOONJUMP = new HashSet<>();
    private static final Set<UUID> WATER_WALK = new HashSet<>();

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
                                        .executes(context -> setEffect(context, false, false))))));
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
                if (!moonjump) {
                    player.fallDistance = 0.0F;
                }
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.level().isClientSide) {
            return;
        }

        UUID id = player.getUUID();
        if (MOONJUMP.contains(id) && !player.getAbilities().flying && !player.isFallFlying()
                && !player.onGround()) {
            Vec3 velocity = player.getDeltaMovement();
            // Compensa parte de la gravedad normal para ampliar el salto y ralentizar la caída.
            double adjustedY = velocity.y + (velocity.y < 0.0D ? 0.055D : 0.045D);
            player.setDeltaMovement(velocity.x, adjustedY, velocity.z);
            player.fallDistance *= 0.94F;
            player.hurtMarked = true;
        }

        if (WATER_WALK.contains(id)) {
            keepPlayerOnWater(player);
        }
    }

    /** Mantiene los pies justo encima de la superficie y elimina el hundimiento del agua. */
    private static void keepPlayerOnWater(ServerPlayer player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.08D, player.getZ());
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
        if (player.getY() < waterPos.getY() - 0.15D || player.getY() > surfaceY + 0.42D) {
            return;
        }

        player.setPos(player.getX(), surfaceY + 0.035D, player.getZ());
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.0D, motion.z);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        MOONJUMP.remove(id);
        WATER_WALK.remove(id);
    }
}
