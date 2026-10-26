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
    private static final double ELEVATION = Math.toRadians(51.0D);
    private static final double HALF_SIZE = 80.0D;
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

        double time = age + partialTick;
        if (shader.getUniform("VortexTime") != null) {
            shader.getUniform("VortexTime").set((float) time);
        }

        Vec3 center = new Vec3(0.0D, Math.sin(ELEVATION), -Math.cos(ELEVATION)).scale(SKY_DISTANCE);
        var camera = event.getCamera();
        var cameraLeft = camera.getLeftVector();
        var cameraUp = camera.getUpVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z).normalize();
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z).normalize();

        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.cloudQuads();
        VertexConsumer vertices = buffers.getBuffer(type);

        Vec3 p0 = point(center, right, up, -HALF_SIZE, -HALF_SIZE);
        Vec3 p1 = point(center, right, up, HALF_SIZE, -HALF_SIZE);
        Vec3 p2 = point(center, right, up, HALF_SIZE, HALF_SIZE);
        Vec3 p3 = point(center, right, up, -HALF_SIZE, HALF_SIZE);
        vertices.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z).uv(0.0F, 0.0F).endVertex();
        vertices.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z).uv(1.0F, 0.0F).endVertex();
        vertices.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z).uv(1.0F, 1.0F).endVertex();
        vertices.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z).uv(0.0F, 1.0F).endVertex();
        buffers.endBatch(type);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double x, double y) {
        return center.add(right.scale(x)).add(up.scale(y));
    }
}
