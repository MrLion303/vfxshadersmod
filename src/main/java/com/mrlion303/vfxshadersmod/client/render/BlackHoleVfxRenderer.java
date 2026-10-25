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

/**
 * Renderer espacial del agujero negro. Dibuja un horizonte opaco y un disco
 * de acreción que anima el color y la ondulación. La lente gravitacional de
 * pantalla se ejecuta en BlackHoleLensingPass después de renderizar el nivel.
 */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
    private static final int SPHERE_SEGMENTS = 48;
    private static final int SPHERE_BANDS = 20;
    private static final int DISK_SEGMENTS = 192;
    private static final int DISK_LAYERS = 4;

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
        double radius = Math.max(0.5D, diameter * 0.5D);
        Vec3 center = context.toCameraRelative(instance.position());
        PoseStack.Pose pose = context.poseStack().last();
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        RenderType coreType = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer core = buffers.getBuffer(coreType);
        drawSphere(core, pose, center, radius);
        buffers.endBatch(coreType);

        RenderType diskType = VfxRenderTypes.colorQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);
        drawAccretionDisk(disk, pose, center, radius, context.gameTime() + context.partialTick());
        buffers.endBatch(diskType);

        RenderType linesType = VfxRenderTypes.colorLines();
        VertexConsumer lines = buffers.getBuffer(linesType);
        drawLensingArcs(lines, pose, center, radius, context.gameTime() + context.partialTick());
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
        return new Vec3(
                center.x + Math.cos(angle) * horizontal,
                center.y + Math.sin(latitude) * radius,
                center.z + Math.sin(angle) * horizontal
        );
    }

    private static void drawAccretionDisk(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 center,
            double coreRadius,
            double time
    ) {
        for (int layer = 0; layer < DISK_LAYERS; layer++) {
            double innerRadius = coreRadius * (1.02D + layer * 0.12D);
            double outerRadius = innerRadius + coreRadius * (0.13D + layer * 0.015D);
            float layerAlpha = switch (layer) {
                case 0 -> 0.72F;
                case 1 -> 0.46F;
                case 2 -> 0.30F;
                default -> 0.19F;
            };

            for (int i = 0; i < DISK_SEGMENTS; i++) {
                double angleA = Math.PI * 2.0D * i / DISK_SEGMENTS;
                double angleB = Math.PI * 2.0D * (i + 1) / DISK_SEGMENTS;
                double waveA = Math.sin(angleA * 3.0D + time * 0.055D + layer * 1.4D) * coreRadius * 0.028D;
                double waveB = Math.sin(angleB * 3.0D + time * 0.055D + layer * 1.4D) * coreRadius * 0.028D;

                Vec3 innerA = diskPoint(center, innerRadius, angleA, waveA);
                Vec3 outerA = diskPoint(center, outerRadius, angleA, waveA);
                Vec3 outerB = diskPoint(center, outerRadius, angleB, waveB);
                Vec3 innerB = diskPoint(center, innerRadius, angleB, waveB);

                float hue = (float) (angleA / (Math.PI * 2.0D) + time * 0.0018D + layer * 0.13D);
                int rgb = VfxGeometry.rainbowRgb(hue);
                VfxGeometry.quad(vertices, pose,
                        innerA.x, innerA.y, innerA.z,
                        outerA.x, outerA.y, outerA.z,
                        outerB.x, outerB.y, outerB.z,
                        innerB.x, innerB.y, innerB.z,
                        rgb, layerAlpha);
            }
        }
    }

    private static Vec3 diskPoint(Vec3 center, double radius, double angle, double yOffset) {
        return new Vec3(
                center.x + Math.cos(angle) * radius,
                center.y + yOffset,
                center.z + Math.sin(angle) * radius
        );
    }

    private static void drawLensingArcs(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 center,
            double coreRadius,
            double time
    ) {
        final int segments = 96;
        for (int plane = 0; plane < 3; plane++) {
            Vec3 previous = arcPoint(center, coreRadius * 1.13D, 0.0D, plane, time);
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0D * i / segments;
                Vec3 current = arcPoint(center, coreRadius * 1.13D, angle, plane, time);
                float hue = (float) (angle / (Math.PI * 2.0D) + time * 0.002D + plane * 0.22D);
                int rgb = VfxGeometry.rainbowRgb(hue);
                emitLineVertex(vertices, pose, previous, rgb, 0.9F);
                emitLineVertex(vertices, pose, current, rgb, 0.9F);
                previous = current;
            }
        }
    }

    private static Vec3 arcPoint(Vec3 center, double radius, double angle, int plane, double time) {
        double wobble = Math.sin(angle * 4.0D + time * 0.04D + plane) * radius * 0.025D;
        return switch (plane) {
            case 1 -> new Vec3(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius, center.z + wobble);
            case 2 -> new Vec3(center.x + wobble, center.y + Math.sin(angle) * radius, center.z + Math.cos(angle) * radius);
            default -> new Vec3(center.x + Math.cos(angle) * radius, center.y + wobble, center.z + Math.sin(angle) * radius);
        };
    }

    private static void emitLineVertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 point,
            int rgb,
            float alpha
    ) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
