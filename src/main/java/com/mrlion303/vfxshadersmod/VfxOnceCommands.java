package com.mrlion303.vfxshadersmod;

import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import com.mrlion303.vfxshadersmod.util.VfxColors;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos de una sola ejecución; no dejan un efecto global activo. */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxOnceCommands {
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
                                        .executes(VfxOnceCommands::scan)))));
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

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
