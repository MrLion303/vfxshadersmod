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

/** Campo de estrellas veloces con estelas curvas alrededor de un punto de fuga en el horizonte. */
public final class StarsspeedEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "starsspeed");
    private static final int SEGMENTS = 192;
    private static final int BANDS = 80;
    private static final int STAR_COUNT = 1800;
    private static final double SKY_RADIUS = 512.0D;
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

        double time = age + partialTick;

        for (int band = 0; band < BANDS; band++) {
            double f0 = (double) band / BANDS;
            double f1 = (double) (band + 1) / BANDS;
            // Cúpula completa: desde el horizonte inferior hasta el cenit y el cielo trasero.
            double latA = -Math.PI * 0.5D + 0.006D + (Math.PI - 0.012D) * f0;
            double latB = -Math.PI * 0.5D + 0.006D + (Math.PI - 0.012D) * f1;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a = Math.PI * 2.0D * segment / SEGMENTS;
                double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                emitGradientQuad(sky, pose,
                        point(a, latA, SKY_RADIUS), point(b, latA, SKY_RADIUS),
                        point(b, latB, SKY_RADIUS), point(a, latB, SKY_RADIUS),
                        skyColorAtLatitude(latA), skyColorAtLatitude(latB));
            }
        }
        buffers.endBatch(skyType);

        // Eje fijo de mundo: girar la cámara no reorienta el campo de estrellas.
        Vec3 horizonForward = new Vec3(0.0D, 0.0D, -1.0D);
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 orbitRight = new Vec3(1.0D, 0.0D, 0.0D);

        RenderType starType = VfxRenderTypes.skyAdditiveQuads();
        VertexConsumer stars = buffers.getBuffer(starType);
        int haloColor = brighten(color, 0.82F);
        for (int i = 0; i < STAR_COUNT; i++) {
            double seed = fract(Math.sin((i + 1) * 127.1D) * 43758.5453D);
            double seed2 = fract(Math.sin((i + 1) * 311.7D) * 24634.6345D);
            // El ángulo polar cubre la esfera celeste completa, no solo el horizonte.
            double polar = 0.022D + seed2 * (Math.PI - 0.044D);
            // El movimiento individual ahora es claramente visible incluso a simple vista.
            double angle = seed * Math.PI * 2.0D + time * (0.025D + seed * 0.014D);
            double trailAngle = 0.12D + seed * 0.30D;
            double tailAngle = angle - trailAngle;

            Vec3 radial = worldUp.scale(Math.cos(angle)).add(orbitRight.scale(Math.sin(angle)));
            Vec3 tail = orbitPoint(polar, tailAngle, SKY_RADIUS * 0.992D,
                    horizonForward, worldUp, orbitRight);
            Vec3 head = orbitPoint(polar, angle, SKY_RADIUS * 0.992D,
                    horizonForward, worldUp, orbitRight);
            Vec3 across = horizonForward.scale(-Math.sin(polar))
                    .add(radial.scale(Math.cos(polar))).normalize()
                    .scale(0.46D + seed * 0.88D);
            Vec3 broadAcross = across.scale(2.7D);

            emitQuad(stars, pose,
                    tail.subtract(broadAcross), tail.add(broadAcross),
                    head.add(broadAcross.scale(0.42D)), head.subtract(broadAcross.scale(0.42D)),
                    haloColor, 0.23F);
            emitQuad(stars, pose,
                    tail.subtract(across), tail.add(across),
                    head.add(across.scale(0.24D)), head.subtract(across.scale(0.24D)),
                    0xFFFFFF, 0.88F);

            // Destellos blancos en algunas puntas para que el campo se sienta luminoso.
            if (i % 5 == 0) {
                Vec3 tangent = worldUp.scale(-Math.sin(angle))
                        .add(orbitRight.scale(Math.cos(angle))).normalize()
                        .scale(0.48D + seed * 0.46D);
                Vec3 sparkAcross = across.normalize().scale(0.92D + seed * 0.36D);
                emitQuad(stars, pose,
                        head.subtract(sparkAcross), head.add(tangent.scale(0.20D)),
                        head.add(sparkAcross), head.subtract(tangent.scale(0.20D)),
                        0xFFFFFF, 0.92F);
            }
        }
        buffers.endBatch(starType);
    }

    private static Vec3 orbitPoint(
            double polar, double angle, double radius,
            Vec3 forward, Vec3 worldUp, Vec3 orbitRight
    ) {
        Vec3 radial = worldUp.scale(Math.cos(angle)).add(orbitRight.scale(Math.sin(angle)));
        return forward.scale(Math.cos(polar) * radius).add(radial.scale(Math.sin(polar) * radius));
    }

    private static Vec3 point(double azimuth, double latitude, double radius) {
        double horizontal = Math.cos(latitude) * radius;
        return new Vec3(Math.cos(azimuth) * horizontal,
                Math.sin(latitude) * radius,
                Math.sin(azimuth) * horizontal);
    }

    private int skyColorAtLatitude(double latitude) {
        double height = Math.max(0.0D, Math.min(1.0D,
                Math.abs(latitude) / (Math.PI * 0.5D)));
        int horizon = mixColor(0x25164A, color, 0.30F);
        int middle = mixColor(0x111329, color, 0.12F);
        double smoothHeight = height * height * (3.0D - 2.0D * height);
        if (smoothHeight < 0.52D) {
            return mixColor(horizon, middle, (float) (smoothHeight / 0.52D));
        }
        return mixColor(middle, 0x02040B, (float) ((smoothHeight - 0.52D) / 0.48D));
    }

    private static int mixColor(int first, int second, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int r1 = (first >> 16) & 255;
        int g1 = (first >> 8) & 255;
        int b1 = first & 255;
        int r2 = (second >> 16) & 255;
        int g2 = (second >> 8) & 255;
        int b2 = second & 255;
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void emitGradientQuad(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int lowerRgb, int upperRgb
    ) {
        emitColorVertex(consumer, pose, p0, lowerRgb, 0.99F);
        emitColorVertex(consumer, pose, p1, lowerRgb, 0.99F);
        emitColorVertex(consumer, pose, p2, upperRgb, 0.99F);
        emitColorVertex(consumer, pose, p3, upperRgb, 0.99F);
    }

    private static void emitColorVertex(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha
    ) {
        float red = ((rgb >> 16) & 255) / 255.0F;
        float green = ((rgb >> 8) & 255) / 255.0F;
        float blue = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private static Vec3 rotateAroundY(Vec3 point, double angle) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return new Vec3(point.x * cosine - point.z * sine,
                point.y,
                point.x * sine + point.z * cosine);
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

    private static int scaleColor(int rgb, float factor) {
        int red = Math.max(0, Math.min(255, Math.round(((rgb >> 16) & 255) * factor)));
        int green = Math.max(0, Math.min(255, Math.round(((rgb >> 8) & 255) * factor)));
        int blue = Math.max(0, Math.min(255, Math.round((rgb & 255) * factor)));
        return (red << 16) | (green << 8) | blue;
    }

    private static int brighten(int rgb, float amount) {
        int red = Math.min(255, Math.round(((rgb >> 16) & 255) + (255 - ((rgb >> 16) & 255)) * amount));
        int green = Math.min(255, Math.round(((rgb >> 8) & 255) + (255 - ((rgb >> 8) & 255)) * amount));
        int blue = Math.min(255, Math.round((rgb & 255) + (255 - (rgb & 255)) * amount));
        return (red << 16) | (green << 8) | blue;
    }
}
