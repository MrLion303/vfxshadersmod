package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Set;

/** Renderer cinematográfico de agujeros negros colocados en coordenadas del mundo. */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
    private static final int SPHERE_SEGMENTS = 64;
    private static final int SPHERE_BANDS = 26;
    private static final int DISK_SEGMENTS = 192;
    private static final int DISK_LAYERS = 6;

    @Override
    public Set<RenderLevelStageEvent.Stage> renderStages() {
        return Set.of(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
    }

    @Override
    public void render(VfxRenderContext context, WorldVfxInstance instance) {
        if (!context.isWithinDistance(instance.position(), 192.0D)) {
            return;
        }

        double diameter = instance.parameters().getOrDefault(
                "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
        double requestedRadius = Math.max(0.5D, diameter * 0.5D);
        double distanceToCenter = instance.position().distanceTo(context.cameraPosition());
        // Limita el tamaño angular del horizonte para que una aparición cercana no cubra toda la vista.
        double radius = Math.min(requestedRadius, Math.max(0.025D, distanceToCenter * 0.40D));
        Vec3 center = context.toCameraRelative(instance.position());
        Vec3 direction = instance.position().subtract(context.cameraPosition());
        if (direction.lengthSqr() < 1.0E-6D) {
            var look = context.camera().getLookVector();
            direction = new Vec3(look.x, look.y, look.z);
        } else {
            direction = direction.normalize();
        }

        var cameraLeft = context.camera().getLeftVector();
        var cameraUp = context.camera().getUpVector();
        Vec3 screenRight = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z).normalize();
        Vec3 screenUp = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z).normalize();

        PoseStack.Pose pose = context.poseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        // El horizonte conserva un tamaño angular limitado incluso cuando la cámara se acerca.
        if (distanceToCenter > 0.05D) {
            RenderType coreType = VfxRenderTypes.colorOpaqueQuads();
            VertexConsumer core = buffers.getBuffer(coreType);
            drawSphere(core, pose, center, radius);
            buffers.endBatch(coreType);
        }

        double time = context.gameTime() + context.partialTick();
        RenderType diskType = VfxRenderTypes.colorQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);
        drawAccretionDisk(disk, pose, center, radius, direction, screenRight, screenUp, time);
        buffers.endBatch(diskType);

        RenderType linesType = VfxRenderTypes.colorLines();
        VertexConsumer lines = buffers.getBuffer(linesType);
        drawLensingArcs(lines, pose, center, radius, direction, screenRight, screenUp, time);
        buffers.endBatch(linesType);
    }

    private static void drawSphere(VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double radius) {
        for (int band = 0; band < SPHERE_BANDS; band++) {
            double latitudeA = -Math.PI * 0.5D + Math.PI * band / SPHERE_BANDS;
            double latitudeB = -Math.PI * 0.5D + Math.PI * (band + 1) / SPHERE_BANDS;
            for (int segment = 0; segment < SPHERE_SEGMENTS; segment++) {
                double angleA = Math.PI * 2.0D * segment / SPHERE_SEGMENTS;
                double angleB = Math.PI * 2.0D * (segment + 1) / SPHERE_SEGMENTS;
                Vec3 a = spherePoint(center, radius, angleA, latitudeA);
                Vec3 b = spherePoint(center, radius, angleB, latitudeA);
                Vec3 c = spherePoint(center, radius, angleB, latitudeB);
                Vec3 d = spherePoint(center, radius, angleA, latitudeB);
                VfxGeometry.quad(vertices, pose,
                        a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z,
                        0x000000, 1.0F);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 center, double radius, double angle, double latitude) {
        double horizontal = Math.cos(latitude) * radius;
        return new Vec3(center.x + Math.cos(angle) * horizontal,
                center.y + Math.sin(latitude) * radius,
                center.z + Math.sin(angle) * horizontal);
    }

    private static void drawAccretionDisk(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius,
            Vec3 direction, Vec3 screenRight, Vec3 screenUp, double time
    ) {
        for (int layer = 0; layer < DISK_LAYERS; layer++) {
            double innerRadius = coreRadius * (1.04D + layer * 0.28D);
            double outerRadius = innerRadius + coreRadius * (0.17D + layer * 0.018D);
            float layerAlpha = switch (layer) {
                case 0 -> 0.99F;
                case 1 -> 0.86F;
                case 2 -> 0.67F;
                case 3 -> 0.48F;
                case 4 -> 0.32F;
                default -> 0.21F;
            };
            int rgb = switch (layer) {
                case 0 -> 0xFFFFF3;
                case 1 -> 0xFFFFD8;
                case 2 -> 0xFFFFB4;
                case 3 -> 0xFFFF8A;
                case 4 -> 0xFFE16A;
                default -> 0xFFAD52;
            };

            for (int i = 0; i < DISK_SEGMENTS; i++) {
                double angleA = Math.PI * 2.0D * i / DISK_SEGMENTS;
                double angleB = Math.PI * 2.0D * (i + 1) / DISK_SEGMENTS;
                double waveA = Math.sin(angleA * 3.0D + time * 0.055D + layer * 1.1D)
                        * coreRadius * 0.035D;
                double waveB = Math.sin(angleB * 3.0D + time * 0.055D + layer * 1.1D)
                        * coreRadius * 0.035D;

                Vec3 innerA = diskPoint(center, innerRadius, angleA, waveA, direction, screenRight, screenUp);
                Vec3 outerA = diskPoint(center, outerRadius, angleA, waveA, direction, screenRight, screenUp);
                Vec3 outerB = diskPoint(center, outerRadius, angleB, waveB, direction, screenRight, screenUp);
                Vec3 innerB = diskPoint(center, innerRadius, angleB, waveB, direction, screenRight, screenUp);
                double turbulence = 0.76D
                        + 0.17D * Math.sin((angleA + angleB) * 3.5D + time * 0.12D + layer)
                        + 0.07D * Math.sin((angleA + angleB) * 8.0D - time * 0.08D);
                float segmentAlpha = (float) (layerAlpha * Math.max(0.42D, turbulence));
                VfxGeometry.quad(vertices, pose,
                        innerA.x, innerA.y, innerA.z, outerA.x, outerA.y, outerA.z,
                        outerB.x, outerB.y, outerB.z, innerB.x, innerB.y, innerB.z, rgb, segmentAlpha);
            }
        }
    }

    private static Vec3 diskPoint(
            Vec3 center, double radius, double angle, double wobble,
            Vec3 direction, Vec3 screenRight, Vec3 screenUp
    ) {
        return center.add(
                screenRight.scale(Math.cos(angle) * radius)
                        .add(screenUp.scale(Math.sin(angle) * radius * 0.36D))
                        .add(direction.scale(wobble))
        );
    }

    private static void drawLensingArcs(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius,
            Vec3 direction, Vec3 screenRight, Vec3 screenUp, double time
    ) {
        final int segments = 144;
        final double[] horizontalScale = {1.12D, 1.36D, 1.66D, 1.92D};
        final double[] verticalScale = {0.28D, 0.40D, 0.55D, 0.72D};
        final int[] colors = {0xFFFFF0, 0xFFFFD8, 0xFFF0D0, 0xFFD0A0};
        final float[] alpha = {0.98F, 0.72F, 0.43F, 0.22F};

        for (int layer = 0; layer < horizontalScale.length; layer++) {
            Vec3 previous = arcPoint(center, coreRadius, 0.0D, layer, time,
                    horizontalScale[layer], verticalScale[layer], direction, screenRight, screenUp);
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0D * i / segments;
                Vec3 current = arcPoint(center, coreRadius, angle, layer, time,
                        horizontalScale[layer], verticalScale[layer], direction, screenRight, screenUp);
                emitLineVertex(vertices, pose, previous, colors[layer], alpha[layer]);
                emitLineVertex(vertices, pose, current, colors[layer], alpha[layer]);
                previous = current;
            }
        }
    }

    private static Vec3 arcPoint(
            Vec3 center, double radius, double angle, int layer, double time,
            double horizontalScale, double verticalScale, Vec3 direction, Vec3 screenRight, Vec3 screenUp
    ) {
        double wobble = Math.sin(angle * 3.0D + time * 0.025D + layer) * radius * 0.012D;
        return center.add(
                screenRight.scale(Math.cos(angle) * radius * horizontalScale)
                        .add(screenUp.scale(Math.sin(angle) * radius * verticalScale))
                        .add(direction.scale(wobble))
        );
    }

    private static void emitLineVertex(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 point, int rgb, float alpha
    ) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
