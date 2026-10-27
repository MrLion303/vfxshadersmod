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
import org.joml.Vector3f;

/** Agujero negro celeste con el mismo horizonte y disco procedural que el agujero colocado. */
public final class BlackHoleSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole_sky");
    public static final double SKY_DISTANCE = 512.0D;
    public static final double SKY_DIAMETER = 2340.0D;
    public static final double SKY_ELEVATION_DEGREES = 50.0D;
    public static final double SKY_HALF_WIDTH = SKY_DIAMETER * 0.90D / 10.5D;

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

    public static Vec3 centerOffset() {
        return centerOffset;
    }

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
        ShaderInstance shader = VfxShaderLibrary.blackHoleSurface();
        if (shader == null) {
            return;
        }
        if (shader.getUniform("BlackHoleTime") != null) {
            shader.getUniform("BlackHoleTime").set((float) (age + partialTick));
        }

        Vec3 center = centerOffset;
        Vector3f cameraLeft = event.getCamera().getLeftVector();
        Vector3f cameraUp = event.getCamera().getUpVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z);
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z);
        Vec3 rightOffset = right.scale(SKY_HALF_WIDTH);
        Vec3 upOffset = up.scale(SKY_HALF_WIDTH);
        Vec3 p0 = center.subtract(rightOffset).subtract(upOffset);
        Vec3 p1 = center.add(rightOffset).subtract(upOffset);
        Vec3 p2 = center.add(rightOffset).add(upOffset);
        Vec3 p3 = center.subtract(rightOffset).add(upOffset);

        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.blackHoleSkyQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        emit(vertices, pose, p0, 0.0F, 0.0F);
        emit(vertices, pose, p1, 1.0F, 0.0F);
        emit(vertices, pose, p2, 1.0F, 1.0F);
        emit(vertices, pose, p3, 0.0F, 1.0F);
        buffers.endBatch(type);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }
}
