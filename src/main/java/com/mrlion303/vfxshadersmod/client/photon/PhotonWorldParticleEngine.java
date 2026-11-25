package com.mrlion303.vfxshadersmod.client.photon;

import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.gameobject.emitter.PhotonParticleRenderType;
import com.mrlion303.vfxshadersmod.VfxShadersMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Motor de partículas de Photon aislado del ParticleEngine de Minecraft.
 * Solo recibe objetos creados por Photon y nunca procesa partículas de otros mods.
 */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PhotonWorldParticleEngine {
    private static ParticleEngine engine;
    private static ClientLevel activeLevel;

    private PhotonWorldParticleEngine() {
    }

    public static void addParticle(Particle particle) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            particle.remove();
            return;
        }

        ensureEngine(minecraft, level);
        engine.add(particle);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || engine == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            engine.setLevel(null);
            activeLevel = null;
            engine = null;
            clearEffectCaches();
            return;
        }

        ensureEngine(minecraft, level);
        engine.tick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (engine == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != activeLevel) {
            return;
        }

        RenderLevelStageEvent.Stage stage = event.getStage();
        boolean opaquePass = stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
        boolean translucentPass = stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        if (!opaquePass && !translucentPass) {
            return;
        }

        if (opaquePass) {
            PhotonParticleRenderType.prepareForParticleRendering(event.getFrustum());
        }

        engine.render(
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                minecraft.gameRenderer.lightTexture(),
                event.getCamera(),
                event.getPartialTick(),
                event.getFrustum()
        );
        PhotonParticleRenderType.finishRender();
    }

    private static void ensureEngine(Minecraft minecraft, ClientLevel level) {
        if (engine == null) {
            engine = new ParticleEngine(level, minecraft.getTextureManager());
            activeLevel = level;
            return;
        }

        if (activeLevel != level) {
            engine.setLevel(level);
            activeLevel = level;
            clearEffectCaches();
        }
    }

    private static void clearEffectCaches() {
        EntityEffect.CACHE.clear();
        BlockEffect.CACHE.clear();
    }
}
