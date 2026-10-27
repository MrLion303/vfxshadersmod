package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
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

/** Vórtice de nubes rojo-anaranjadas con textura procedural y un núcleo incandescente. */
public final class CloudVortexEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "cloud_vortex");
    private static final double SKY_DISTANCE = 224.0D;
    private static final double ELEVATION = Math.toRadians(28.0D);
    private static final double HALF_SIZE = 160.0D;
    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
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
        ShaderInstance shader = VfxShaderLibrary.cloudVortex();
        if (shader == null) {
            return;
        }

        var camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.colorQuads();
        VertexConsumer out = buffers.getBuffer(type);
        float time = (float) (age + partialTick);

        // El vórtice se construye como una malla 3D fija en el mundo:
        // múltiples brazos helicoidales, capas de nubes y una esfera central.
        Vec3 center = new Vec3(0.0D, 74.0D, -150.0D);
        Vec3 axis = new Vec3(0.0D, 0.32D, -1.0D).normalize();
        Vec3 right = axis.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 up = right.cross(axis).normalize();
        int arms = 5;
        int segments = 140;
        for (int arm = 0; arm < arms; arm++) {
            for (int i = 0; i < segments; i++) {
                double t0 = i / (double) segments;
                double t1 = (i + 1) / (double) segments;
                double a0 = t0 * Math.PI * 5.4D + arm * Math.PI * 2.0D / arms - time * 0.012D;
                double a1 = t1 * Math.PI * 5.4D + arm * Math.PI * 2.0D / arms - time * 0.012D;
                double r0 = 5.0D + t0 * 57.0D;
                double r1 = 5.0D + t1 * 57.0D;
                double thickness0 = (2.0D + 7.0D * Math.sin(t0 * Math.PI))
                        * (0.65D + 0.35D * Math.sin(i * 1.71D + arm * 4.0D));
                double thickness1 = (2.0D + 7.0D * Math.sin(t1 * Math.PI))
                        * (0.65D + 0.35D * Math.sin((i + 1) * 1.71D + arm * 4.0D));
                Vec3 p0 = center.add(right.scale(Math.cos(a0) * r0)).add(up.scale(Math.sin(a0) * r0))
                        .add(axis.scale(Math.sin(a0 * 1.7D) * 10.0D));
                Vec3 p1 = center.add(right.scale(Math.cos(a1) * r1)).add(up.scale(Math.sin(a1) * r1))
                        .add(axis.scale(Math.sin(a1 * 1.7D) * 10.0D));
                Vec3 radial0 = right.scale(Math.cos(a0)).add(up.scale(Math.sin(a0))).normalize();
                Vec3 radial1 = right.scale(Math.cos(a1)).add(up.scale(Math.sin(a1))).normalize();
                Vec3 side0 = radial0.cross(axis).normalize().scale(thickness0);
                Vec3 side1 = radial1.cross(axis).normalize().scale(thickness1);
                float brightness = 0.55F + 0.45F * (float) Math.sin(t0 * Math.PI);
                int color = mixRgb(0x4A0508, 0xFF4A08, brightness);
                emit(out, pose, p0.subtract(side0), color, 0.86F);
                emit(out, pose, p0.add(side0), color, 0.86F);
                emit(out, pose, p1.add(side1), color, 0.86F);
                emit(out, pose, p1.subtract(side1), color, 0.86F);
            }
        }

        // Capas de escombros/plasma en órbitas distintas: no son una textura plana.
        for (int i = 0; i < 420; i++) {
            double seed = i * 12.9898D;
            double angle = seed + time * (0.006D + (i % 7) * 0.001D);
            double radius = 18.0D + fract(Math.sin(seed * 1.73D) * 43758.5453D) * 48.0D;
            double axial = (fract(Math.sin(seed * 0.91D) * 24634.6345D) - 0.5D) * 34.0D;
            Vec3 radial = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 p = center.add(radial.scale(radius)).add(axis.scale(axial));
            double size = 0.35D + fract(Math.sin(seed * 2.31D) * 1347.12D) * 1.5D;
            Vec3 side = radial.cross(axis).normalize().scale(size);
            Vec3 vertical = axis.scale(size * (1.0D + (i % 3) * 0.45D));
            int color = (i % 5 == 0) ? 0xFFD7A0 : ((i % 3 == 0) ? 0xFF741D : 0x9D1416);
            emit(out, pose, p.subtract(side).subtract(vertical), color, 0.74F);
            emit(out, pose, p.add(side).subtract(vertical), color, 0.74F);
            emit(out, pose, p.add(side).add(vertical), color, 0.74F);
            emit(out, pose, p.subtract(side).add(vertical), color, 0.74F);
        }
        buffers.endBatch(type);

        // Núcleo esférico 3D: malla de triángulos con gradiente radial.
        RenderType opaque = VfxRenderTypes.colorOpaqueQuads();
        VertexConsumer core = buffers.getBuffer(opaque);
        double coreRadius = 9.0D;
        for (int lat = 0; lat < 24; lat++) {
            double p0 = -Math.PI / 2.0D + Math.PI * lat / 24.0D;
            double p1 = -Math.PI / 2.0D + Math.PI * (lat + 1) / 24.0D;
            for (int lon = 0; lon < 48; lon++) {
                double a0 = Math.PI * 2.0D * lon / 48.0D;
                double a1 = Math.PI * 2.0D * (lon + 1) / 48.0D;
                Vec3 v0 = sphere(center, coreRadius, p0, a0, right, up, axis);
                Vec3 v1 = sphere(center, coreRadius, p0, a1, right, up, axis);
                Vec3 v2 = sphere(center, coreRadius, p1, a1, right, up, axis);
                Vec3 v3 = sphere(center, coreRadius, p1, a0, right, up, axis);
                emit(core, pose, v0, 0xFF3A08, 1.0F);
                emit(core, pose, v1, 0xFF3A08, 1.0F);
                emit(core, pose, v2, 0xFFF0B2, 1.0F);
                emit(core, pose, v3, 0xFFF0B2, 1.0F);
            }
        }
        buffers.endBatch(opaque);
    }

    private static Vec3 sphere(Vec3 center, double radius, double latitude, double longitude,
                               Vec3 right, Vec3 up, Vec3 axis) {
        double horizontal = Math.cos(latitude) * radius;
        return center.add(right.scale(Math.cos(longitude) * horizontal))
                .add(up.scale(Math.sin(longitude) * horizontal))
                .add(axis.scale(Math.sin(latitude) * radius));
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static int mixRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1.0F - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1.0F - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1.0F - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p, int rgb, float alpha) {
        consumer.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
                .color(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F,
                        (rgb & 255) / 255.0F, alpha).endVertex();
    }

    private static void setVector(ShaderInstance shader, String name, Vec3 vector) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(new float[]{
                    (float) vector.x, (float) vector.y, (float) vector.z
            });
        }
    }

}
