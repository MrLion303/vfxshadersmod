package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxGeometry;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Copia de las barras musicales originales, impulsadas por los sonidos que
 * comienzan a reproducirse en este cliente y no por un pulso global compartido.
 */
public final class MusicSyncBarsEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "music_sync_bars");

    private static final int BAR_COUNT = 160;
    private static final double RADIUS = 42.0D;
    private static final double BASE_OFFSET = 0.12D;
    private static final float[] SOUND_ENERGY = new float[BAR_COUNT];
    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    /**
     * Forge llama a esto cuando el cliente empieza a emitir un sonido.
     * El tono distribuye energía entre las barras y el volumen determina
     * la intensidad inicial del impulso.
     */
    public static synchronized void onLocalSound(SoundInstance sound) {
        if (sound == null) {
            return;
        }
        float volume = Math.max(0.0F, Math.min(1.0F, sound.getVolume()));
        float pitch = Math.max(0.5F, Math.min(2.0F, sound.getPitch()));
        if (volume < 0.015F) {
            return;
        }

        double tone = (pitch - 0.5D) / 1.5D;
        int center = Math.floorMod(Math.round((float) (tone * (BAR_COUNT - 1))), BAR_COUNT);
        int spread = 5 + Math.round(volume * 12.0F);
        float impulse = Math.min(1.0F, 0.22F + volume * 0.9F);

        for (int offset = -spread; offset <= spread; offset++) {
            int index = Math.floorMod(center + offset, BAR_COUNT);
            float distance = Math.abs(offset) / (float) (spread + 1);
            float contribution = impulse * (1.0F - distance) * (1.0F - distance);
            SOUND_ENERGY[index] = Math.min(1.0F,
                    SOUND_ENERGY[index] + contribution);
        }

        // Cada sonido también produce una onda más amplia, para que los sonidos
        // graves y los efectos cortos sean visibles sin perder la distribución tonal.
        int stride = Math.max(1, Math.round(8.0F / pitch));
        for (int i = 0; i < BAR_COUNT; i++) {
            double harmonic = 0.5D + 0.5D * Math.sin((i - center) * Math.PI / stride);
            SOUND_ENERGY[i] = Math.min(1.0F,
                    SOUND_ENERGY[i] + impulse * 0.12F * (float) harmonic);
        }
    }

    @Override
    public void tick(EffectContext context) {
        age++;
        synchronized (MusicSyncBarsEffect.class) {
            for (int i = 0; i < SOUND_ENERGY.length; i++) {
                SOUND_ENERGY[i] *= 0.91F;
                if (SOUND_ENERGY[i] < 0.008F) {
                    SOUND_ENERGY[i] = 0.0F;
                }
            }
        }
    }

    @Override
    public void onStop(EffectContext context) {
        synchronized (MusicSyncBarsEffect.class) {
            java.util.Arrays.fill(SOUND_ENERGY, 0.0F);
        }
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        Vec3 playerPosition = player.getPosition(partialTick);
        Vec3 cameraPosition = event.getCamera().getPosition();
        double centerX = playerPosition.x - cameraPosition.x;
        double centerY = playerPosition.y - cameraPosition.y;
        double centerZ = playerPosition.z - cameraPosition.z;
        double time = age + partialTick;

        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType renderType = VfxRenderTypes.colorQuads();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        for (int i = 0; i < BAR_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / BAR_COUNT;
            float energy;
            synchronized (MusicSyncBarsEffect.class) {
                energy = SOUND_ENERGY[i];
            }

            // Se conserva el movimiento orgánico original; la señal local de audio
            // añade el impulso principal y no depende de un reloj de sonido global.
            double idle = 0.12D + 0.12D * (0.5D + 0.5D * Math.sin(angle * 4.0D + time * 0.13D));
            double height = 0.22D + idle + energy * (2.0D + 8.5D * energy);

            float hue = (float) (angle / (Math.PI * 2.0D)) + (float) time * 0.0012F;
            int rgb = VfxGeometry.rainbowRgb(hue);

            drawBar(vertices, pose, centerX, centerY, centerZ, angle, height, rgb, 1.85D, 0.02F, 0.12F);
            drawBar(vertices, pose, centerX, centerY, centerZ, angle, height, rgb, 1.28D, 0.07F, 0.28F);
            drawBar(vertices, pose, centerX, centerY, centerZ, angle, height, rgb, 0.82D, 0.32F, 0.96F);
        }

        buffers.endBatch(renderType);
    }

    private static void drawBar(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            double centerX,
            double centerY,
            double centerZ,
            double angle,
            double height,
            int rgb,
            double width,
            float bottomAlpha,
            float topAlpha
    ) {
        double radialX = Math.cos(angle);
        double radialZ = Math.sin(angle);
        double tangentX = -radialZ;
        double tangentZ = radialX;
        double cx = centerX + radialX * RADIUS;
        double cz = centerZ + radialZ * RADIUS;
        double halfWidth = width * 0.5D;
        double bottom = centerY + BASE_OFFSET;
        double top = bottom + height;

        double leftX = cx - tangentX * halfWidth;
        double leftZ = cz - tangentZ * halfWidth;
        double rightX = cx + tangentX * halfWidth;
        double rightZ = cz + tangentZ * halfWidth;

        VfxGeometry.gradientQuad(
                vertices, pose,
                leftX, bottom, leftZ,
                rightX, bottom, rightZ,
                rightX, top, rightZ,
                leftX, top, leftZ,
                rgb, bottomAlpha, topAlpha
        );
    }
}
