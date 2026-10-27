package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.postprocess.BlackHoleLensingPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos que solo existen en el cliente. */
@Mod.EventBusSubscriber(
        modid = VfxShadersMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientVfxEvents {
    private ClientVfxEvents() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientVfxCatalog.registerDefaults();
        ClientVfxCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof LocalPlayer player) {
            ClientPlayerMovementEffects.tick(player);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ClientVfxManager.clear();
        } else {
            ClientVfxManager.tick(minecraft.level);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        ClientVfxManager.render(event);
        // La esfera ya no puede envolver la cámara; la pasada de lente puede volver a
        // deformar únicamente el fondo alrededor de los agujeros negros colocados.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            BlackHoleLensingPass.render(event);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientVfxManager.clear();
    }
}
