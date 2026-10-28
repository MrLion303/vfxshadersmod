package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Agujero negro físico: horizonte esférico de volumen real y disco de acreción
 * en un plano 3D fijo. Ya no usa un cuadrado que mira siempre a la cámara.
 */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
    private static final double HORIZON_SCALE = 0.35D;
    private static final double SHADER_HORIZON_RADIUS = 0.244D;
    private static final int LATITUDE_SEGMENTS = 20;
    private static final int LONGITUDE_SEGMENTS = 48;
    private static final Vec3 WORLD_DISK_NORMAL = new Vec3(0.18D, 0.93D, 0.32D).normalize();

    public static Vec3 diskNormal() {
        return WORLD_DISK_NORMAL;
    }

    @Override
    public java.util.Set<RenderLevelStageEvent.Stage> renderStages() {
        return java.util.Set.of(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
    }

    @Override
    public void render(VfxRenderContext context, WorldVfxInstance instance) {
        if (!context.isWithinDistance(instance.position(), 192.0D)) {
            return;
        }

        ShaderInstance shader = VfxShaderLibrary.blackHoleSurface();
        if (shader == null) {
            return;
        }
        if (shader.getUniform("BlackHoleTime") != null) {
            shader.getUniform("BlackHoleTime").set(
                    (float) (context.gameTime() + context.partialTick()));
        }

        double diameter = instance.parameters().getOrDefault(
                "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
        double configuredRadius = Math.max(0.5D, diameter * 0.5D);
        double horizonRadius = configuredRadius * HORIZON_SCALE;
        double diskHalfSize = horizonRadius / SHADER_HORIZON_RADIUS;
        Vec3 center = context.toCameraRelative(instance.position());

        PoseStack.Pose pose = context.poseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        // El disco se dibuja primero sobre una malla plana orientada en el mundo.
        // Así se ve de canto o en perspectiva al cambiar la cámara, no como un sprite.
        Vec3 referenceUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 diskRight = WORLD_DISK_NORMAL.cross(referenceUp).normalize();
        if (diskRight.lengthSqr() < 1.0E-6D) {
            diskRight = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 diskUp = diskRight.cross(WORLD_DISK_NORMAL).normalize();
        Vec3 rightOffset = diskRight.scale(diskHalfSize);
        Vec3 upOffset = diskUp.scale(diskHalfSize);
        Vec3 p0 = center.subtract(rightOffset).subtract(upOffset);
        Vec3 p1 = center.add(rightOffset).subtract(upOffset);
        Vec3 p2 = center.add(rightOffset).add(upOffset);
        Vec3 p3 = center.subtract(rightOffset).add(upOffset);

        RenderType diskType = VfxRenderTypes.blackHoleWorldQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);
        emitUv(disk, pose, p0, 0.0F, 0.0F);
        emitUv(disk, pose, p1, 1.0F, 0.0F);
        emitUv(disk, pose, p2, 1.0F, 1.0F);
        emitUv(disk, pose, p3, 0.0F, 1.0F);
        buffers.endBatch(diskType);

        // Horizonte negro como esfera UV. La silueta cambia con la perspectiva,
        // queda ocluida por el terreno y conserva un borde cálido muy tenue.
        Vec3 viewDirection = context.cameraPosition().subtract(instance.position()).normalize();
        Vec3 lightDirection = new Vec3(-0.44D, 0.72D, -0.53D).normalize();
        RenderType horizonType = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer horizon = buffers.getBuffer(horizonType);
        drawHorizonSphere(horizon, pose, center, horizonRadius, viewDirection, lightDirection);
        buffers.endBatch(horizonType);
    }

    private static void drawHorizonSphere(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 center,
            double radius,
            Vec3 viewDirection,
            Vec3 lightDirection
    ) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double lat0 = -Math.PI * 0.5D + Math.PI * latitude / LATITUDE_SEGMENTS;
            double lat1 = -Math.PI * 0.5D + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double lon0 = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                double lon1 = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;
                emitHorizonVertex(consumer, pose, center, radius, lat0, lon0, viewDirection, lightDirection);
                emitHorizonVertex(consumer, pose, center, radius, lat0, lon1, viewDirection, lightDirection);
                emitHorizonVertex(consumer, pose, center, radius, lat1, lon1, viewDirection, lightDirection);
                emitHorizonVertex(consumer, pose, center, radius, lat1, lon0, viewDirection, lightDirection);
            }
        }
    }

    private static void emitHorizonVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 center,
            double radius,
            double latitude,
            double longitude,
            Vec3 viewDirection,
            Vec3 lightDirection
    ) {
        double cosLatitude = Math.cos(latitude);
        double nx = cosLatitude * Math.cos(longitude);
        double ny = Math.sin(latitude);
        double nz = cosLatitude * Math.sin(longitude);
        Vec3 normal = new Vec3(nx, ny, nz);
        double facing = Math.max(0.0D, Math.min(1.0D, normal.dot(viewDirection)));
        double rim = Math.pow(1.0D - facing, 18.0D);
        double light = Math.max(0.0D, normal.dot(lightDirection));

        // El horizonte sigue siendo casi negro; la variación de color solo define
        // la curvatura y el borde, sin pintar una esfera gris encima del disco.
        float red = (float) Math.min(1.0D, 0.0015D + rim * 0.030D + light * 0.0015D);
        float green = (float) Math.min(1.0D, 0.0008D + rim * 0.0045D + light * 0.0008D);
        float blue = (float) Math.min(1.0D, 0.0018D + rim * 0.0065D + light * 0.0015D);

        consumer.vertex(
                        pose.pose(),
                        (float) (center.x + nx * radius),
                        (float) (center.y + ny * radius),
                        (float) (center.z + nz * radius))
                .color(red, green, blue, 1.0F)
                .endVertex();
    }

    private static void emitUv(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v
    ) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }
}
