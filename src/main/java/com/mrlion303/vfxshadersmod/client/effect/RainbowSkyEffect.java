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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Superpone una cúpula arcoíris translúcida sobre el cielo. El matiz se desplaza
 * continuamente y todos los armónicos se cierran en 2 PI para evitar un borde visible.
 */
public final class RainbowSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "rainbow_sky");

    private static final int SEGMENTS = 96;
    private static final int VERTICAL_BANDS = 20;
    private static final double RADIUS = 128.0D;
    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        double time = age + partialTick;
        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType renderType = VfxRenderTypes.colorQuads();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        // Un pequeño margen bajo el horizonte evita que la cúpula termine en una línea dura.
        double lowerLatitude = -0.16D;
        double upperLatitude = 1.46D;

        for (int band = 0; band < VERTICAL_BANDS; band++) {
            double f0 = (double) band / VERTICAL_BANDS;
            double f1 = (double) (band + 1) / VERTICAL_BANDS;
            double latitudeA = lowerLatitude + (upperLatitude - lowerLatitude) * f0;
            double latitudeB = lowerLatitude + (upperLatitude - lowerLatitude) * f1;
            double mid = (f0 + f1) * 0.5D;
            float alpha = (float) (0.15D + 0.19D * Math.sin(mid * Math.PI * 0.5D));

            for (int i = 0; i < SEGMENTS; i++) {
                double angleA = Math.PI * 2.0D * i / SEGMENTS;
                double angleB = Math.PI * 2.0D * (i + 1) / SEGMENTS;
                float hue = (float) (angleA / (Math.PI * 2.0D) + time * 0.0022D
                        + Math.sin(latitudeA * 2.0D + time * 0.012D) * 0.025D);
                int rgb = VfxGeometry.rainbowRgb(hue);

                Vec3 a = domePoint(camera, angleA, latitudeA);
                Vec3 b = domePoint(camera, angleB, latitudeA);
                Vec3 c = domePoint(camera, angleB, latitudeB);
                Vec3 d = domePoint(camera, angleA, latitudeB);

                VfxGeometry.quad(vertices, pose,
                        a.x, a.y, a.z,
                        b.x, b.y, b.z,
                        c.x, c.y, c.z,
                        d.x, d.y, d.z,
                        rgb, alpha);
            }
        }

        buffers.endBatch(renderType);
    }

    private static Vec3 domePoint(Vec3 camera, double azimuth, double latitude) {
        double horizontal = Math.cos(latitude) * RADIUS;
        return new Vec3(
                Math.cos(azimuth) * horizontal,
                Math.sin(latitude) * RADIUS,
                Math.sin(azimuth) * horizontal
        );
    }
}
