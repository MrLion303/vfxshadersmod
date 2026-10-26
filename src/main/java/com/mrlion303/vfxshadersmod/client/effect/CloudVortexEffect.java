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

/** Vórtice de nubes rojo-anaranjadas que gira sobre su propio eje en el cielo. */
public final class CloudVortexEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "cloud_vortex");
    private static final double SKY_DISTANCE = 184.0D;
    private static final double ELEVATION = Math.toRadians(51.0D);
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
        Vec3 camera = event.getCamera().getPosition();
        Vec3 worldDirection = new Vec3(0.0D, Math.sin(ELEVATION), -Math.cos(ELEVATION));
        Vec3 center = worldDirection.scale(SKY_DISTANCE);
        var cameraLeft = event.getCamera().getLeftVector();
        var cameraUp = event.getCamera().getUpVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z).normalize();
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z).normalize();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.colorQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        double time = age + partialTick;

        // Centro brillante rojizo rodeado por varias capas de humo luminoso.
        drawGlow(vertices, pose, center, right, up, 4.0D, 0xFFF1C2, 0.78F);
        drawGlow(vertices, pose, center, right, up, 7.0D, 0xFF9A53, 0.34F);
        drawGlow(vertices, pose, center, right, up, 11.0D, 0xFF4B27, 0.18F);

        // Cuatro brazos anchos, formados por cientos de segmentos con bordes suaves y aspecto nuboso.
        for (int arm = 0; arm < 4; arm++) {
            double armOffset = arm * Math.PI * 0.5D;
            for (int step = 0; step < 112; step++) {
                double f0 = (double) step / 112.0D;
                double f1 = (double) (step + 1) / 112.0D;
                double angle0 = f0 * Math.PI * 4.2D + time * 0.045D + armOffset;
                double angle1 = f1 * Math.PI * 4.2D + time * 0.045D + armOffset;
                double radius0 = 2.2D + f0 * 32.0D + Math.sin(angle0 * 2.7D + time * 0.09D) * 1.4D;
                double radius1 = 2.2D + f1 * 32.0D + Math.sin(angle1 * 2.7D + time * 0.09D) * 1.4D;
                double width0 = 1.1D + 0.7D * (0.5D + 0.5D * Math.sin(angle0 * 5.0D + time * 0.13D));
                double width1 = 1.1D + 0.7D * (0.5D + 0.5D * Math.sin(angle1 * 5.0D + time * 0.13D));

                Vec3 p0 = point(center, right, up, radius0, angle0 - width0 / Math.max(radius0, 1.0D));
                Vec3 p1 = point(center, right, up, radius0, angle0 + width0 / Math.max(radius0, 1.0D));
                Vec3 p2 = point(center, right, up, radius1, angle1 + width1 / Math.max(radius1, 1.0D));
                Vec3 p3 = point(center, right, up, radius1, angle1 - width1 / Math.max(radius1, 1.0D));

                int rgb = step % 5 == 0 ? 0xFFCF83 : (step % 3 == 0 ? 0xFF6A39 : 0xD83B2B);
                float alpha = (float) (0.15D + 0.28D * (1.0D - f0));
                VfxGeometry.quad(vertices, pose,
                        p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);

                // Nubecillas laterales rompen la silueta para que no parezca una línea geométrica.
                if (step % 3 == 0) {
                    double puffRadius = radius0 + Math.sin(angle0 * 4.0D) * 1.3D;
                    Vec3 puffCenter = point(center, right, up, puffRadius, angle0);
                    double puffSize = 0.7D + 0.7D * (0.5D + 0.5D * Math.sin(time * 0.06D + step));
                    drawGlow(vertices, pose, puffCenter, right, up, puffSize, rgb, 0.22F);
                }
            }
        }
        buffers.endBatch(type);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double radius, double angle) {
        return center.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius));
    }

    private static void drawGlow(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, Vec3 right, Vec3 up,
            double radius, int rgb, float alpha
    ) {
        final int segments = 28;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            Vec3 p0 = center;
            Vec3 p1 = point(center, right, up, radius, a);
            Vec3 p2 = point(center, right, up, radius, b);
            VfxGeometry.quad(vertices, pose,
                    p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    p2.x, p2.y, p2.z, p0.x, p0.y, p0.z, rgb, alpha);
        }
    }
}
