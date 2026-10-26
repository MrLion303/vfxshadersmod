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

/** Cielo teñido con estelas estelares rápidas y un resplandor que nace en el horizonte. */
public final class StarsspeedEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "starsspeed");
    private static final int SEGMENTS = 96;
    private static final int BANDS = 38;
    private static final int STAR_COUNT = 210;
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
        RenderType quadsType = VfxRenderTypes.colorQuads();
        VertexConsumer sky = buffers.getBuffer(quadsType);

        // La cúpula conserva un único color en todas las direcciones del cielo.
        for (int band = 0; band < BANDS; band++) {
            double f0 = (double) band / BANDS;
            double f1 = (double) (band + 1) / BANDS;
            double latA = -0.18D + (Math.PI * 0.5D + 0.18D) * f0;
            double latB = -0.18D + (Math.PI * 0.5D + 0.18D) * f1;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a = Math.PI * 2.0D * segment / SEGMENTS;
                double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                Vec3 p0 = point(a, latA, SKY_RADIUS);
                Vec3 p1 = point(b, latA, SKY_RADIUS);
                Vec3 p2 = point(b, latB, SKY_RADIUS);
                Vec3 p3 = point(a, latB, SKY_RADIUS);
                VfxGeometry.quad(sky, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, color, 0.94F);
            }
        }
        buffers.endBatch(quadsType);

        // Bandas luminosas en el horizonte: el centro se aclara y se desvanece hacia arriba.
        VertexConsumer glow = buffers.getBuffer(quadsType);
        int bright = brighten(color, 0.76F);
        for (int band = 0; band < 14; band++) {
            double latA = -0.035D + band * 0.018D;
            double latB = latA + 0.021D;
            float alpha = 0.12F * (1.0F - band / 17.0F);
            int rgb = band < 4 ? bright : color;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a = Math.PI * 2.0D * segment / SEGMENTS;
                double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                Vec3 p0 = point(a, latA, SKY_RADIUS - 0.35D);
                Vec3 p1 = point(b, latA, SKY_RADIUS - 0.35D);
                Vec3 p2 = point(b, latB, SKY_RADIUS - 0.35D);
                Vec3 p3 = point(a, latB, SKY_RADIUS - 0.35D);
                VfxGeometry.quad(glow, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
            }
        }
        buffers.endBatch(quadsType);

        // Las estrellas se mueven sobre una esfera celeste; las estelas se alargan en el sentido del viaje.
        RenderType lineType = VfxRenderTypes.colorLines();
        VertexConsumer lines = buffers.getBuffer(lineType);
        double time = age + partialTick;
        for (int i = 0; i < STAR_COUNT; i++) {
            double seed = fract(Math.sin((i + 1) * 127.1D) * 43758.5453D);
            double seed2 = fract(Math.sin((i + 1) * 311.7D) * 24634.6345D);
            double latitude = -0.06D + seed2 * 1.57D;
            double angle = seed * Math.PI * 2.0D + time * (0.028D + seed * 0.012D);
            Vec3 tail = point(angle - 0.055D, latitude + 0.012D, SKY_RADIUS * 0.995D);
            Vec3 head = point(angle, latitude, SKY_RADIUS * 0.995D);
            emit(lines, pose, tail, 0xFFFFFF, 0.78F);
            emit(lines, pose, head, 0xFFFFFF, 0.92F);
        }
        buffers.endBatch(lineType);
    }

    private static Vec3 point(double azimuth, double latitude, double radius) {
        double horizontal = Math.cos(latitude) * radius;
        return new Vec3(Math.cos(azimuth) * horizontal,
                Math.sin(latitude) * radius,
                Math.sin(azimuth) * horizontal);
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

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(r, g, b, alpha).endVertex();
    }
}
