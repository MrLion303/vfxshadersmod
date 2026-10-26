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

/** Cielo uniforme con estelas rápidas y un resplandor amplio en el horizonte. */
public final class StarsspeedEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "starsspeed");
    private static final int SEGMENTS = 112;
    private static final int BANDS = 46;
    private static final int STAR_COUNT = 440;
    private static final double SKY_RADIUS = 176.0D;
    private static volatile int requestedColor = 0x3478FF;

    private int color = requestedColor;
    private long age;

    public static void configureColor(int rgb) {
        requestedColor = rgb & 0xFFFFFF;
    }

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
        color = requestedColor;
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType skyType = VfxRenderTypes.skyQuads();
        VertexConsumer sky = buffers.getBuffer(skyType);

        // Cúpula uniforme: no introduce ningún degradado cromático entre lados del cielo.
        for (int band = 0; band < BANDS; band++) {
            double f0 = (double) band / BANDS;
            double f1 = (double) (band + 1) / BANDS;
            double latA = -0.18D + (Math.PI * 0.5D + 0.18D) * f0;
            double latB = -0.18D + (Math.PI * 0.5D + 0.18D) * f1;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a = Math.PI * 2.0D * segment / SEGMENTS;
                double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                emitQuad(sky, pose,
                        point(a, latA, SKY_RADIUS), point(b, latA, SKY_RADIUS),
                        point(b, latB, SKY_RADIUS), point(a, latB, SKY_RADIUS),
                        color, 0.98F);
            }
        }
        buffers.endBatch(skyType);

        // Glow de gran tamaño que nace en el horizonte y se desvanece hacia el cenit.
        VertexConsumer glow = buffers.getBuffer(skyType);
        int bright = brighten(color, 0.42F);
        for (int band = 0; band < 40; band++) {
            double latA = -0.10D + band * 0.018D;
            double latB = latA + 0.021D;
            double mid = (latA + latB) * 0.5D;
            double profile = Math.exp(-Math.pow((mid - 0.025D) / 0.34D, 2.0D));
            float alpha = (float) (0.50D * profile);
            int rgb = band < 22 ? bright : color;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a = Math.PI * 2.0D * segment / SEGMENTS;
                double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                emitQuad(glow, pose,
                        point(a, latA, SKY_RADIUS - 0.25D), point(b, latA, SKY_RADIUS - 0.25D),
                        point(b, latB, SKY_RADIUS - 0.25D), point(a, latB, SKY_RADIUS - 0.25D),
                        rgb, alpha);
            }
        }
        buffers.endBatch(skyType);

        // Estrellas como estelas gruesas y brillantes; giran por toda la esfera celeste.
        VertexConsumer stars = buffers.getBuffer(skyType);
        double time = age + partialTick;
        for (int i = 0; i < STAR_COUNT; i++) {
            double seed = fract(Math.sin((i + 1) * 127.1D) * 43758.5453D);
            double seed2 = fract(Math.sin((i + 1) * 311.7D) * 24634.6345D);
            double latitude = -0.045D + seed2 * 1.52D;
            double speed = 0.075D + seed * 0.055D;
            double angle = seed * Math.PI * 2.0D + time * speed;
            double trailLength = 0.065D + seed * 0.045D;
            Vec3 tail = point(angle - trailLength, latitude + 0.014D, SKY_RADIUS * 0.992D);
            Vec3 head = point(angle, latitude, SKY_RADIUS * 0.992D);
            Vec3 side = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                    .scale(0.22D + seed * 0.28D);

            emitQuad(stars, pose, tail.subtract(side.scale(2.6D)), tail.add(side.scale(2.6D)),
                    head.add(side.scale(0.9D)), head.subtract(side.scale(0.9D)), 0xFFFFFF, 0.20F);
            emitQuad(stars, pose, tail.subtract(side), tail.add(side),
                    head.add(side.scale(0.24D)), head.subtract(side.scale(0.24D)), 0xFFFFFF, 0.98F);
        }
        buffers.endBatch(skyType);
    }

    private static Vec3 point(double azimuth, double latitude, double radius) {
        double horizontal = Math.cos(latitude) * radius;
        return new Vec3(Math.cos(azimuth) * horizontal,
                Math.sin(latitude) * radius,
                Math.sin(azimuth) * horizontal);
    }

    private static void emitQuad(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, float alpha
    ) {
        VfxGeometry.quad(consumer, pose,
                p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static int brighten(int rgb, float amount) {
        int red = Math.min(255, Math.round(((rgb >> 16) & 255) + (255 - ((rgb >> 16) & 255)) * amount));
        int green = Math.min(255, Math.round(((rgb >> 8) & 255) + (255 - ((rgb >> 8) & 255)) * amount));
        int blue = Math.min(255, Math.round((rgb & 255) + (255 - (rgb & 255)) * amount));
        return (red << 16) | (green << 8) | blue;
    }
}
