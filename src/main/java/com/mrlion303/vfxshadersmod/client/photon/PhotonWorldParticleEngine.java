package com.mrlion303.vfxshadersmod.client.photon;

import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.gameobject.emitter.ParticleQueueRenderType;
import com.lowdragmc.photon.client.gameobject.emitter.PhotonParticleRenderType;
import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Ejecuta únicamente los objetos Particle de Photon.
 *
 * No utiliza Minecraft.particleEngine ni el ParticleEngine de otros mods.
 * Los emisores y sus partículas se actualizan y dibujan en este ciclo privado.
 */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PhotonWorldParticleEngine {
    private static final Set<Particle> PHOTON_OBJECTS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static ClientLevel activeLevel;

    private PhotonWorldParticleEngine() {
    }

    /**
     * Punto de entrada adaptado de IFXObject.emmit().
     * Photon registra aquí sus emisores sin insertarlos en el motor vanilla.
     */
    public static void addParticle(Particle particle) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            particle.remove();
            return;
        }

        switchLevel(level);
        PHOTON_OBJECTS.add(particle);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        switchLevel(level);
        for (Particle particle : new ArrayList<>(PHOTON_OBJECTS)) {
            if (!particle.isAlive()) {
                PHOTON_OBJECTS.remove(particle);
                continue;
            }

            particle.tick();
            if (!particle.isAlive()) {
                PHOTON_OBJECTS.remove(particle);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        boolean opaquePass = stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
        boolean translucentPass = stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        if ((!opaquePass && !translucentPass) || PHOTON_OBJECTS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != activeLevel) {
            return;
        }

        if (opaquePass) {
            PhotonParticleRenderType.prepareForParticleRendering(event.getFrustum());
        }

        BufferBuilder builder = new BufferBuilder(256);
        ParticleQueueRenderType queueRenderer = ParticleQueueRenderType.INSTANCE;
        try {
            // La cola pertenece exclusivamente a Photon: no se solicita ni se vacía
            // el buffer de partículas de Minecraft.
            queueRenderer.begin(builder);
            List<Particle> snapshot = new ArrayList<>(PHOTON_OBJECTS);
            for (Particle particle : snapshot) {
                if (particle.isAlive()) {
                    particle.render(builder, event.getCamera(), event.getPartialTick());
                }
            }
            queueRenderer.end(builder);
        } finally {
            PhotonParticleRenderType.finishRender();
            // Ningún pase de Photon debe dejar un target o shader activo para el siguiente renderer.
            minecraft.getMainRenderTarget().bindWrite(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void switchLevel(ClientLevel level) {
        if (activeLevel == level) {
            return;
        }
        clear();
        activeLevel = level;
    }

    public static void clear() {
        for (Particle particle : PHOTON_OBJECTS) {
            particle.remove();
        }
        PHOTON_OBJECTS.clear();
        activeLevel = null;
        EntityEffect.CACHE.clear();
        BlockEffect.CACHE.clear();
    }
}
