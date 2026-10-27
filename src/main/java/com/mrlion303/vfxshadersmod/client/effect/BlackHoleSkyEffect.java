package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mrlion303.vfxshadersmod.client.render.VfxReferenceTextures;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Agujero negro celeste hecho con geometría 3D: horizonte esférico y disco de
 * acreción con grosor, superficies superior/inferior y paredes laterales.
 */
public final class BlackHoleSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole_sky");
    public static final double SKY_DISTANCE = 512.0D;
    public static final double SKY_DIAMETER = 2340.0D;
    public static final double SKY_ELEVATION_DEGREES = 50.0D;
    // El diámetro lógico se triplicó respecto al valor anterior (780), sin crear un plano gigante.
    public static final double SKY_HALF_WIDTH = SKY_DIAMETER * 0.90D / 10.5D;

    private static final int DISK_SEGMENTS = 128;
    private static final int DISK_RADIAL_STEPS = 10;
    private static final int SPHERE_LATITUDES = 28;
    private static final int SPHERE_LONGITUDES = 56;
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

    /** Vectoro relativo de la escena del agujero celeste. */
    public static Vec3 centerOffset() {
        return centerOffset;
    }

    /** Normal usada por el disco GLSL/3D para orientar el disco Photon en el mismo plano. */
    public static Vec3 diskNormal() {
        return direction.add(planeRight.scale(0.30D)).normalize();
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null) {
            return;
        }

        ShaderInstance diskShader = VfxShaderLibrary.blackHoleDisk();
        if (diskShader == null) {
            return;
        }
        VfxReferenceTextures.bind(diskShader, "BlackHoleReference", "textures/vfx/blackhole_disk_reference.png");
        if (diskShader.getUniform("BlackHoleDiskTime") != null) {
            diskShader.getUniform("BlackHoleDiskTime").set((float) (age + partialTick));
        }

        Vec3 center = centerOffset;
        double horizonRadius = SKY_HALF_WIDTH * 0.245D;
        double innerDiskRadius = horizonRadius * 1.065D;
        double outerDiskRadius = SKY_HALF_WIDTH * 0.455D;
        double maximumHalfThickness = horizonRadius * 0.15D;

        // Ligeramente inclinado respecto a la línea de visión: la silueta no es un círculo plano.
        Vec3 diskNormal = direction.add(planeRight.scale(0.30D)).normalize();
        Vec3 diskRight = diskNormal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        if (diskRight.lengthSqr() < 1.0E-6D) {
            diskRight = planeRight;
        }
        Vec3 diskUp = diskRight.cross(diskNormal).normalize();

        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType diskType = VfxRenderTypes.blackHoleSkyDiskQuads();
        VertexConsumer disk = buffers.getBuffer(diskType);

        // Superficies superior e inferior de un anillo grueso, con sus bordes de cierre.
        // Las coordenadas UV mantienen las hebras de gas procedurales del shader en el anillo real.
        for (int segment = 0; segment < DISK_SEGMENTS; segment++) {
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

                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, r0, h0), u0, (float) v0);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, r1, h1), u0, (float) v1);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, r1, h1), u1, (float) v1);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, r0, h0), u1, (float) v0);

                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, r0, -h0), u0, (float) v0);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, r0, -h0), u1, (float) v0);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, r1, -h1), u1, (float) v1);
                emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, r1, -h1), u0, (float) v1);
            }

            // Las paredes laterales hacen que el grosor sea una forma cerrada, no solo dos láminas.
            double innerThickness = diskThickness(0.08D, maximumHalfThickness);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, innerDiskRadius, innerThickness), u0, 0.08F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, innerDiskRadius, innerThickness), u1, 0.08F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, innerDiskRadius, -innerThickness), u1, 0.08F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, innerDiskRadius, -innerThickness), u0, 0.08F);

            double outerThickness = diskThickness(0.90D, maximumHalfThickness);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, outerDiskRadius, outerThickness), u0, 0.90F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a0, outerDiskRadius, -outerThickness), u0, 0.90F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, outerDiskRadius, -outerThickness), u1, 0.90F);
            emitUv(disk, pose, diskPoint(center, diskRight, diskUp, diskNormal, a1, outerDiskRadius, outerThickness), u1, 0.90F);
        }
        buffers.endBatch(diskType);

        // El horizonte se dibuja como esfera opaca casi negra. La iluminación del borde
        // permanece dentro de la silueta y no crea una aureola rectangular o exterior gigante.
        RenderType sphereType = VfxRenderTypes.skyQuads();
        VertexConsumer sphere = buffers.getBuffer(sphereType);
        Vec3 toCamera = center.scale(-1.0D).normalize();
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
                emitHorizon(sphere, pose, center.add(n0.scale(horizonRadius)), n0, toCamera);
                emitHorizon(sphere, pose, center.add(n1.scale(horizonRadius)), n1, toCamera);
                emitHorizon(sphere, pose, center.add(n2.scale(horizonRadius)), n2, toCamera);
                emitHorizon(sphere, pose, center.add(n3.scale(horizonRadius)), n3, toCamera);
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

    private static void emitHorizon(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            Vec3 normal, Vec3 toCamera
    ) {
        double facing = Math.max(0.0D, Math.min(1.0D, normal.dot(toCamera)));
        double limb = Math.pow(1.0D - facing, 9.0D);
        float shade = (float) (0.002D + limb * 0.012D);
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(shade, shade, shade * 1.5F, 1.0F).endVertex();
    }
}
