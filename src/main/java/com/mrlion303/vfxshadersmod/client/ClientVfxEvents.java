package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.photon.PhotonEffectBridge;
import com.mrlion303.vfxshadersmod.client.effect.MusicSyncBarsEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
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
    public static void onLocalSoundStarted(PlaySoundSourceEvent event) {
        // Este evento se dispara en el cliente que realmente reproduce el sonido;
        // no se transmite como un pulso global a los demás jugadores.
        MusicSyncBarsEffect.onLocalSound(event.getSound());
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
            ClientVfxCameraController.reset();
            ClientVfxManager.clear();
        } else {
            ClientVfxCameraController.tick(minecraft.player);
            ClientVfxManager.tick(minecraft.level);
            ClientPlayerVfxRenderer.tick(minecraft.level);
            PhotonEffectBridge.tickOneShots();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientVfxCameraController.applyCameraAngles(event);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientVfxCameraController.shouldHideHand()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        ClientVfxManager.render(event);
        ClientPlayerVfxRenderer.render(event);
        // La lente antigua copiaba la pantalla completa y podía dejar el framebuffer negro.
        // El agujero negro ahora usa una superficie local y no procesa toda la imagen.
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPlayerVfxRenderer.clear();
        ClientVfxCameraController.reset();
        PhotonEffectBridge.clearOneShots();
        ClientVfxManager.clear();
    }
}
