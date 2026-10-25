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
    public static final double SKY_ELEVATION_DEGREES = 50.0D;

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

        RenderType coreType = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer core = buffers.getBuffer(coreType);
        drawSphere(core, pose, center, radius);
        buffers.endBatch(coreType);

        RenderType diskType = VfxRenderTypes.colorQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);
        drawAccretionDisk(disk, pose, center, radius, time);
        buffers.endBatch(diskType);

        RenderType linesType = VfxRenderTypes.colorLines();
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
     * Disco casi de canto: el eje radial se extiende hacia y desde la cámara,
     * por lo que se ve como la franja luminosa horizontal de Gargantúa.
     */
    private static void drawAccretionDisk(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius, double time
    ) {
        final int segments = 192;
        final int layers = 6;
        for (int layer = 0; layer < layers; layer++) {
            double inner = coreRadius * (1.04D + layer * 0.28D);
            double outer = inner + coreRadius * (0.17D + layer * 0.018D);
            float alpha = switch (layer) {
                case 0 -> 0.96F;
                case 1 -> 0.82F;
                case 2 -> 0.63F;
                case 3 -> 0.45F;
                case 4 -> 0.31F;
                default -> 0.20F;
            };
            int rgb = switch (layer) {
                case 0 -> 0xFFFFF3;
                case 1 -> 0xFFFFD8;
                case 2 -> 0xFFFFB4;
                case 3 -> 0xFFFF8A;
                case 4 -> 0xFFE16A;
                default -> 0xFFAD52;
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
                VfxGeometry.quad(vertices, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
            }
        }
    }

    private static Vec3 diskPoint(Vec3 center, double radius, double angle, double wobble) {
        return center.add(
                planeRight.scale(Math.cos(angle) * radius)
                        .add(direction.scale(Math.sin(angle) * radius))
                        .add(planeUp.scale(wobble))
        );
    }

    /** Halos finos, cálidos y simétricos; ya no son anillos arcoíris. */
    private static void drawLensingArcs(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double radius, double time
    ) {
        final int segments = 144;
        final double[] horizontalScale = {1.12D, 1.36D, 1.66D, 1.92D};
        final double[] verticalScale = {0.78D, 0.98D, 1.18D, 1.38D};
        final int[] colors = {0xFFFFF0, 0xFFFFD8, 0xFFF0D0, 0xFFD0A0};
        final float[] alpha = {0.98F, 0.72F, 0.43F, 0.22F};

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

    private static void emitLine(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
