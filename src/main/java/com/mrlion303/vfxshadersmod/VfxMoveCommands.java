package com.mrlion303.vfxshadersmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/** Comandos de movimiento que alteran la velocidad de los jugadores desde el servidor. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxMoveCommands {
    private static final double EMPUJON_HORIZONTAL = 1.65D;
    private static final double EMPUJON_VERTICAL = 1.0D;

    private VfxMoveCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("vfxmove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("empujon")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(VfxMoveCommands::empujon))));
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
}
