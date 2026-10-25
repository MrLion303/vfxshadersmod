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
 * Agujero negro celeste. Permanece en una dirección del cielo elegida al activarlo,
 * con horizonte negro, disco de acreción y lente gravitacional propia.
 */
public final class BlackHoleSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole_sky");
    public static final double SKY_DISTANCE = 112.0D;
    public static final double SKY_DIAMETER = 22.0D;

    private static Vec3 centerOffset = new Vec3(0.0D, 0.0D, -SKY_DISTANCE);
    private static Vec3 planeRight = new Vec3(1.0D, 0.0D, 0.0D);
    private static Vec3 planeUp = new Vec3(0.0D, 1.0D, 0.0D);
    private static Vec3 direction = new Vec3(0.0D, 0.0D, -1.0D);

    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        direction = minecraft.player.getLookAngle().normalize();
        centerOffset = direction.scale(SKY_DISTANCE);
        Vec3 referenceUp = Math.abs(direction.y) > 0.97D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        planeRight = direction.cross(referenceUp).normalize();
        planeUp = planeRight.cross(direction).normalize();
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    /** Centro en coordenadas relativas a cámara, usado también por el pase de lente. */
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 center = centerOffset;
        double radius = SKY_DIAMETER * 0.5D;
        double time = age + partialTick;
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

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
        final int segments = 48;
        final int bands = 18;
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

    private static void drawAccretionDisk(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double coreRadius, double time
    ) {
        final int segments = 192;
        final int layers = 5;
        for (int layer = 0; layer < layers; layer++) {
            double inner = coreRadius * (1.02D + layer * 0.105D);
            double outer = inner + coreRadius * (0.13D + layer * 0.018D);
            float alpha = switch (layer) {
                case 0 -> 0.84F;
                case 1 -> 0.62F;
                case 2 -> 0.43F;
                case 3 -> 0.30F;
                default -> 0.20F;
            };
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0D * i / segments;
                double b = Math.PI * 2.0D * (i + 1) / segments;
                double wobbleA = Math.sin(a * 3.0D + time * 0.045D + layer) * coreRadius * 0.025D;
                double wobbleB = Math.sin(b * 3.0D + time * 0.045D + layer) * coreRadius * 0.025D;
                Vec3 p0 = diskPoint(center, inner, a, wobbleA);
                Vec3 p1 = diskPoint(center, outer, a, wobbleA);
                Vec3 p2 = diskPoint(center, outer, b, wobbleB);
                Vec3 p3 = diskPoint(center, inner, b, wobbleB);
                int rgb = VfxGeometry.rainbowRgb((float) (a / (Math.PI * 2.0D) + time * 0.0016D + layer * 0.12D));
                VfxGeometry.quad(vertices, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
            }
        }
    }

    private static Vec3 diskPoint(Vec3 center, double radius, double angle, double wobble) {
        return center.add(
                planeRight.scale(Math.cos(angle) * radius)
                        .add(planeUp.scale(Math.sin(angle) * radius))
                        .add(direction.scale(wobble))
        );
    }

    private static void drawLensingArcs(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, double radius, double time
    ) {
        final int segments = 96;
        for (int layer = 0; layer < 3; layer++) {
            double ringRadius = radius * (1.13D + layer * 0.12D);
            Vec3 previous = diskPoint(center, ringRadius, 0.0D, 0.0D);
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0D * i / segments;
                double wobble = Math.sin(angle * 4.0D + time * 0.035D + layer) * radius * 0.018D;
                Vec3 current = diskPoint(center, ringRadius, angle, wobble);
                int rgb = VfxGeometry.rainbowRgb((float) (angle / (Math.PI * 2.0D) + time * 0.0018D + layer * 0.2D));
                emitLine(vertices, pose, previous, rgb, 0.92F);
                emitLine(vertices, pose, current, rgb, 0.92F);
                previous = current;
            }
        }
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
