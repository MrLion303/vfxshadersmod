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
 * Agujero negro celeste fijo en el cielo a 50 grados sobre el horizonte.
 * El centro y su orientación no dependen de hacia dónde mire el jugador al activarlo.
 */
public final class BlackHoleSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole_sky");
    public static final double SKY_DISTANCE = 512.0D;
    public static final double SKY_DIAMETER = 260.0D;
    public static final double SKY_ELEVATION_DEGREES = 14.0D;

    private static final Vec3 WORLD_DIRECTION = new Vec3(
            0.0D,
            Math.sin(Math.toRadians(SKY_ELEVATION_DEGREES)),
            -Math.cos(Math.toRadians(SKY_ELEVATION_DEGREES))
    ).normalize();

    private static Vec3 centerOffset = WORLD_DIRECTION.scale(SKY_DISTANCE);
    private static Vec3 planeRight = new Vec3(1.0D, 0.0D, 0.0D);
    private static Vec3 planeUp = new Vec3(0.0D, 1.0D, 0.0D);
    private static Vec3 direction = WORLD_DIRECTION;

    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
        direction = WORLD_DIRECTION;
        centerOffset = WORLD_DIRECTION.scale(SKY_DISTANCE);

        Vec3 referenceUp = new Vec3(0.0D, 1.0D, 0.0D);
        planeRight = direction.cross(referenceUp).normalize();
        if (planeRight.lengthSqr() < 1.0E-6D) {
            planeRight = new Vec3(1.0D, 0.0D, 0.0D);
        }
        planeUp = planeRight.cross(direction).normalize();
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    /** Vector de mundo relativo a cámara, compartido con el pase de lente. */
    public static Vec3 centerOffset() {
        return centerOffset;
    }

    public static Vec3 planeRight() {
        return planeRight;
    }

    public static Vec3 planeUp() {
        return planeUp;
    }

    public static Vec3 direction() {
        return direction;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Vec3 center = centerOffset;
        double radius = SKY_DIAMETER * 0.5D;
        double time = age + partialTick;
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderType starfieldType = VfxRenderTypes.skyQuads();
        VertexConsumer starfield = buffers.getBuffer(starfieldType);
        drawStarfield(starfield, pose, center, radius, time);
        buffers.endBatch(starfieldType);

        RenderType coreType = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer core = buffers.getBuffer(coreType);
        drawSphere(core, pose, center, radius);
        buffers.endBatch(coreType);

        RenderType diskType = VfxRenderTypes.skyQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);
        drawAccretionDisk(disk, pose, center, radius, time);
        buffers.endBatch(diskType);

        RenderType linesType = VfxRenderTypes.skyLines();
        VertexConsumer lines = buffers.getBuffer(linesType);
        drawLensingArcs(lines, pose, center, radius, time);
        buffers.endBatch(linesType);
    }

    private static void drawSphere(VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double radius) {
        final int segments = 72;
        final int bands = 28;
        for (int band = 0; band < bands; band++) {
            double latA = -Math.PI * 0.5D + Math.PI * band / bands;
            double latB = -Math.PI * 0.5D + Math.PI * (band + 1) / bands;
            for (int i = 0; i < segments; i++) {
                double angleA = Math.PI * 2.0D * i / segments;
                double angleB = Math.PI * 2.0D * (i + 1) / segments;
                Vec3 a = spherePoint(center, radius, angleA, latA);
                Vec3 b = spherePoint(center, radius, angleB, latA);
                Vec3 c = spherePoint(center, radius, angleB, latB);
                Vec3 d = spherePoint(center, radius, angleA, latB);
                VfxGeometry.quad(vertices, pose,
                        a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z,
                        0x000000, 1.0F);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 center, double radius, double angle, double latitude) {
        double horizontal = Math.cos(latitude) * radius;
        return center.add(
                planeRight.scale(Math.cos(angle) * horizontal)
                        .add(planeUp.scale(Math.sin(latitude) * radius))
                        .add(direction.scale(Math.sin(angle) * horizontal))
        );
    }

    /**
     * Disco de acreción colocado en el plano visible del cielo; los dos ejes tangentes
     * conservan el contorno circular y evitan que el disco se colapse en una línea.
     */
    private static void drawAccretionDisk(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius, double time
    ) {
        final int segments = 192;
        final int layers = 6;
        for (int layer = 0; layer < layers; layer++) {
            double inner = coreRadius * (0.99D + layer * 0.16D);
            double outer = inner + coreRadius * (0.15D + layer * 0.006D);
            float layerAlpha = switch (layer) {
                case 0 -> 0.99F;
                case 1 -> 0.92F;
                case 2 -> 0.78F;
                case 3 -> 0.62F;
                case 4 -> 0.44F;
                default -> 0.28F;
            };
            int rgb = switch (layer) {
                case 0 -> 0xFFFFF7;
                case 1 -> 0xFFFFDA;
                case 2 -> 0xFFFFB0;
                case 3 -> 0xFF9A54;
                case 4 -> 0xF65A3F;
                default -> 0x9F4B75;
            };

            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0D * i / segments;
                double b = Math.PI * 2.0D * (i + 1) / segments;
                double wobbleA = Math.sin(a * 3.0D + time * 0.055D + layer * 1.1D)
                        * coreRadius * 0.035D;
                double wobbleB = Math.sin(b * 3.0D + time * 0.055D + layer * 1.1D)
                        * coreRadius * 0.035D;

                Vec3 p0 = diskPoint(center, inner, a, wobbleA);
                Vec3 p1 = diskPoint(center, outer, a, wobbleA);
                Vec3 p2 = diskPoint(center, outer, b, wobbleB);
                Vec3 p3 = diskPoint(center, inner, b, wobbleB);
                double middleAngle = (a + b) * 0.5D;
                double turbulence = 0.72D
                        + 0.18D * Math.sin(middleAngle * 3.5D + time * 0.12D + layer)
                        + 0.10D * Math.sin(middleAngle * 8.0D - time * 0.08D);
                double beaming = Math.max(0.0D, Math.cos(middleAngle + time * 0.018D));
                int segmentColor = mixColor(rgb, 0xFFFFF0,
                        (float) (layer < 2 ? 0.12D + beaming * 0.48D : 0.04D + beaming * 0.22D));
                float segmentAlpha = (float) (layerAlpha * Math.max(0.40D, turbulence)
                        * (0.82D + beaming * 0.18D));
                VfxGeometry.quad(vertices, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, segmentColor, segmentAlpha);
            }
        }
    }

    private static Vec3 diskPoint(Vec3 center, double radius, double angle, double wobble) {
        return center.add(
                planeRight.scale(Math.cos(angle) * radius)
                        .add(planeUp.scale(Math.sin(angle) * radius * 0.28D))
                        .add(direction.scale(wobble))
        );
    }

    /** Halos finos, cálidos y simétricos; ya no son anillos arcoíris. */
    private static void drawLensingArcs(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double radius, double time
    ) {
        final int segments = 144;
        final double[] horizontalScale = {1.12D, 1.36D, 1.66D, 1.92D};
        final double[] verticalScale = {0.28D, 0.40D, 0.55D, 0.72D};
        final int[] colors = {0xFFFFF0, 0xFFFFD8, 0xFFFFBE, 0xFFE18A};
        final float[] alpha = {1.0F, 0.76F, 0.48F, 0.26F};

        for (int layer = 0; layer < horizontalScale.length; layer++) {
            Vec3 previous = arcPoint(center, radius, 0.0D, layer, time,
                    horizontalScale[layer], verticalScale[layer]);
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0D * i / segments;
                Vec3 current = arcPoint(center, radius, angle, layer, time,
                        horizontalScale[layer], verticalScale[layer]);
                emitLine(vertices, pose, previous, colors[layer], alpha[layer]);
                emitLine(vertices, pose, current, colors[layer], alpha[layer]);
                previous = current;
            }
        }
    }

    private static Vec3 arcPoint(
            Vec3 center, double radius, double angle, int layer, double time,
            double horizontalScale, double verticalScale
    ) {
        double wobble = Math.sin(angle * 3.0D + time * 0.025D + layer) * radius * 0.012D;
        return center.add(
                planeRight.scale(Math.cos(angle) * radius * horizontalScale)
                        .add(planeUp.scale(Math.sin(angle) * radius * verticalScale))
                        .add(direction.scale(wobble))
        );
    }

    private static void drawStarfield(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius, double time
    ) {
        for (int i = 0; i < 240; i++) {
            double seed = fract(Math.sin((i + 3) * 127.1D) * 43758.5453D);
            double seed2 = fract(Math.sin((i + 19) * 311.7D) * 24634.6345D);
            double angle = seed * Math.PI * 2.0D;
            double radial = coreRadius * (1.95D + seed2 * 2.0D);
            double wobble = Math.sin(time * 0.018D + i * 1.7D) * coreRadius * 0.025D;
            Vec3 point = center.add(planeRight.scale(Math.cos(angle) * (radial + wobble)))
                    .add(planeUp.scale(Math.sin(angle) * (radial + wobble) * (0.55D + seed2 * 0.28D)))
                    .add(direction.scale(Math.sin(angle * 2.0D + time * 0.012D) * coreRadius * 0.08D));
            double size = coreRadius * (0.009D + seed * 0.013D);
            Vec3 r = planeRight.scale(size);
            Vec3 u = planeUp.scale(size * (1.2D + seed2));
            int starColor = switch (i % 5) {
                case 0 -> 0xB4D9FF;
                case 1 -> 0xD3BCFF;
                case 2 -> 0xFFE6D2;
                case 3 -> 0xFFFFFF;
                default -> 0x8CC9FF;
            };
            float alpha = (float) (0.24D + seed * 0.45D);
            VfxGeometry.quad(vertices, pose,
                    point.x - r.x - u.x, point.y - r.y - u.y, point.z - r.z - u.z,
                    point.x + r.x - u.x, point.y + r.y - u.y, point.z + r.z - u.z,
                    point.x + r.x + u.x, point.y + r.y + u.y, point.z + r.z + u.z,
                    point.x - r.x + u.x, point.y - r.y + u.y, point.z - r.z + u.z, starColor, alpha);
            if (i % 8 == 0) {
                Vec3 longAxis = planeRight.scale(size * 2.8D);
                Vec3 shortAxis = planeUp.scale(size * 0.25D);
                VfxGeometry.quad(vertices, pose,
                        point.x - longAxis.x - shortAxis.x, point.y - longAxis.y - shortAxis.y, point.z - longAxis.z - shortAxis.z,
                        point.x + longAxis.x - shortAxis.x, point.y + longAxis.y - shortAxis.y, point.z + longAxis.z - shortAxis.z,
                        point.x + longAxis.x + shortAxis.x, point.y + longAxis.y + shortAxis.y, point.z + longAxis.z + shortAxis.z,
                        point.x - longAxis.x + shortAxis.x, point.y - longAxis.y + shortAxis.y, point.z - longAxis.z + shortAxis.z,
                        0xFFFFFF, alpha * 0.76F);
            }
        }
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static int mixColor(int a, int b, float weight) {
        float t = Math.max(0.0F, Math.min(1.0F, weight));
        int r = Math.round(((a >> 16) & 255) * (1.0F - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1.0F - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1.0F - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void emitLine(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
