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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * Agujero negro situado en el mundo: disco anular con orientación fija y horizonte
 * esférico real. La deformación del entorno se realiza en el pase de lente gravitacional.
 */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
    private static final int DISK_SEGMENTS = 96;
    private static final int DISK_RADIAL_STEPS = 8;
    private static final int SPHERE_LATITUDES = 32;
    private static final int SPHERE_LONGITUDES = 64;
    private static final Vec3 DISK_NORMAL = new Vec3(0.22D, 0.91D, 0.35D).normalize();

    /** Normal exacta del disco renderizado, reutilizada por el emisor Photon de acreción. */
    public static Vec3 diskNormal() {
        return DISK_NORMAL;
    }

    @Override
    public Set<RenderLevelStageEvent.Stage> renderStages() {
        return Set.of(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
    }

    @Override
    public void render(VfxRenderContext context, WorldVfxInstance instance) {
        if (!context.isWithinDistance(instance.position(), 192.0D)) {
            return;
        }

        ShaderInstance diskShader = VfxShaderLibrary.blackHoleDisk();
        if (diskShader == null) {
            return;
        }
        VfxReferenceTextures.bind(diskShader, "BlackHoleReference", "textures/vfx/blackhole_disk_reference.png");
        double diameter = instance.parameters().getOrDefault(
                "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
        double configuredRadius = Math.max(0.5D, diameter * 0.5D);
        double horizonRadius = configuredRadius * 0.12D;
        double innerDiskRadius = horizonRadius * 1.045D;
        double outerDiskRadius = configuredRadius * 0.63D;
        Vec3 center = context.toCameraRelative(instance.position());

        Vec3 reference = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 diskRight = DISK_NORMAL.cross(reference).normalize();
        Vec3 diskUp = DISK_NORMAL.cross(diskRight).normalize();

        if (diskShader.getUniform("BlackHoleDiskTime") != null) {
            diskShader.getUniform("BlackHoleDiskTime").set(
                    (float) (context.gameTime() + context.partialTick()));
        }

        PoseStack.Pose pose = context.poseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        // El disco no es un plano: se genera una carcasa de acreción con superficies
        // superior/inferior y paredes en los bordes. La normal fija evita que sea un billboard.
        RenderType diskType = VfxRenderTypes.blackHoleDiskQuads();
        VertexConsumer diskVertices = buffers.getBuffer(diskType);
        Integer[] order = new Integer[DISK_SEGMENTS];
        for (int i = 0; i < DISK_SEGMENTS; i++) {
            order[i] = i;
        }
        final double averageRadius = (innerDiskRadius + outerDiskRadius) * 0.5D;
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> {
            double angle = (i + 0.5D) * Math.PI * 2.0D / DISK_SEGMENTS;
            Vec3 offset = diskRight.scale(Math.cos(angle) * averageRadius)
                    .add(diskUp.scale(Math.sin(angle) * averageRadius));
            return instance.position().add(offset).distanceToSqr(context.cameraPosition());
        }).reversed());

        double maximumHalfThickness = Math.max(0.12D, horizonRadius * 0.16D);
        for (int segment : order) {
            double a0 = Math.PI * 2.0D * segment / DISK_SEGMENTS;
            double a1 = Math.PI * 2.0D * (segment + 1) / DISK_SEGMENTS;
            float u0 = (float) (segment / (double) DISK_SEGMENTS);
            float u1 = (float) ((segment + 1) / (double) DISK_SEGMENTS);

            for (int step = 0; step < DISK_RADIAL_STEPS; step++) {
                double v0 = step / (double) DISK_RADIAL_STEPS;
                double v1 = (step + 1) / (double) DISK_RADIAL_STEPS;
                double r0 = innerDiskRadius + (outerDiskRadius - innerDiskRadius) * v0;
                double r1 = innerDiskRadius + (outerDiskRadius - innerDiskRadius) * v1;
                double h0 = diskThickness(v0, maximumHalfThickness);
                double h1 = diskThickness(v1, maximumHalfThickness);

                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, r0, h0), u0, (float) v0);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, r1, h1), u0, (float) v1);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, r1, h1), u1, (float) v1);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, r0, h0), u1, (float) v0);

                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, r0, -h0), u0, (float) v0);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, r0, -h0), u1, (float) v0);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, r1, -h1), u1, (float) v1);
                emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, r1, -h1), u0, (float) v1);
            }

            // Cierra los bordes interior y exterior para que el perfil conserve volumen al verlo de lado.
            double innerThickness = diskThickness(0.08D, maximumHalfThickness);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, innerDiskRadius, innerThickness), u0, 0.08F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, innerDiskRadius, innerThickness), u1, 0.08F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, innerDiskRadius, -innerThickness), u1, 0.08F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, innerDiskRadius, -innerThickness), u0, 0.08F);

            double outerThickness = diskThickness(0.90D, maximumHalfThickness);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, outerDiskRadius, outerThickness), u0, 0.90F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a0, outerDiskRadius, -outerThickness), u0, 0.90F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, outerDiskRadius, -outerThickness), u1, 0.90F);
            emitUv(diskVertices, pose, diskPoint(center, diskRight, diskUp, DISK_NORMAL, a1, outerDiskRadius, outerThickness), u1, 0.90F);
        }        buffers.endBatch(diskType);

        // El horizonte es una esfera opaca de verdad; su tamaño depende únicamente
        // del parámetro del efecto, por lo que crece al acercarse en lugar de encogerse.
        RenderType sphereType = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer sphere = buffers.getBuffer(sphereType);
        Vec3 toCamera = context.cameraPosition().subtract(instance.position()).normalize();
        for (int lat = 0; lat < SPHERE_LATITUDES; lat++) {
            double lat0 = -Math.PI * 0.5D + Math.PI * lat / SPHERE_LATITUDES;
            double lat1 = -Math.PI * 0.5D + Math.PI * (lat + 1) / SPHERE_LATITUDES;
            for (int lon = 0; lon < SPHERE_LONGITUDES; lon++) {
                double lon0 = Math.PI * 2.0D * lon / SPHERE_LONGITUDES;
                double lon1 = Math.PI * 2.0D * (lon + 1) / SPHERE_LONGITUDES;
                Vec3 n0 = sphereNormal(lat0, lon0);
                Vec3 n1 = sphereNormal(lat0, lon1);
                Vec3 n2 = sphereNormal(lat1, lon1);
                Vec3 n3 = sphereNormal(lat1, lon0);
                emitSphere(sphere, pose, center.add(n0.scale(horizonRadius)), n0, toCamera);
                emitSphere(sphere, pose, center.add(n1.scale(horizonRadius)), n1, toCamera);
                emitSphere(sphere, pose, center.add(n2.scale(horizonRadius)), n2, toCamera);
                emitSphere(sphere, pose, center.add(n3.scale(horizonRadius)), n3, toCamera);
            }
        }
        buffers.endBatch(sphereType);
    }

    private static double diskThickness(double radialCoordinate, double maximumHalfThickness) {
        return maximumHalfThickness * (0.20D + 0.80D * Math.sin(Math.PI * radialCoordinate));
    }

    private static Vec3 diskPoint(
            Vec3 center, Vec3 right, Vec3 up, Vec3 normal,
            double angle, double radius, double height
    ) {
        return center.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius))
                .add(normal.scale(height));
    }

    private static Vec3 sphereNormal(double latitude, double longitude) {
        double horizontal = Math.cos(latitude);
        return new Vec3(horizontal * Math.cos(longitude),
                Math.sin(latitude),
                horizontal * Math.sin(longitude));
    }

    private static void emitUv(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }

    private static void emitSphere(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            Vec3 normal, Vec3 toCamera
    ) {
        // El horizonte permanece prácticamente negro; el borde solo recibe un matiz
        // oscuro muy tenue para que la silueta se lea sin crear un halo exterior artificial.
        double facing = Math.max(0.0D, Math.min(1.0D, normal.dot(toCamera)));
        double limb = Math.pow(1.0D - facing, 8.0D);
        float red = (float) ((1.0D + limb * 4.0D) / 255.0D);
        float green = (float) ((1.0D + limb * 4.0D) / 255.0D);
        float blue = (float) ((2.0D + limb * 7.0D) / 255.0D);
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, 1.0F).endVertex();
    }
}
