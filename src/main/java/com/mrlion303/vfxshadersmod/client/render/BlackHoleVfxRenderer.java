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

import java.util.Set;

/**
 * Agujero negro colocado en el mundo. Comparte superficie con el agujero celeste:
 * horizonte negro y disco naranja/blanco, sin copiar ni volver a dibujar el framebuffer.
 */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
    private static final double HORIZON_SCALE = 0.35D;
    private static final double SHADER_HORIZON_RADIUS = 0.245D;

    public static Vec3 diskNormal() {
        return new Vec3(0.0D, 1.0D, 0.0D);
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
        ShaderInstance shader = VfxShaderLibrary.blackHoleSurface();
        if (shader == null) {
            return;
        }
        if (shader.getUniform("BlackHoleTime") != null) {
            shader.getUniform("BlackHoleTime").set((float) (context.gameTime() + context.partialTick()));
        }

        double diameter = instance.parameters().getOrDefault(
                "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
        double configuredRadius = Math.max(0.5D, diameter * 0.5D);
        double horizonRadius = configuredRadius * HORIZON_SCALE;
        double halfSize = Math.max(1.0D, horizonRadius / SHADER_HORIZON_RADIUS);

        Vec3 center = context.toCameraRelative(instance.position());
        Vec3 right = cameraRight(context.camera().getLeftVector());
        var cameraUp = context.camera().getUpVector();
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z);
        Vec3 rightOffset = right.scale(halfSize);
        Vec3 upOffset = up.scale(halfSize);
        Vec3 p0 = center.subtract(rightOffset).subtract(upOffset);
        Vec3 p1 = center.add(rightOffset).subtract(upOffset);
        Vec3 p2 = center.add(rightOffset).add(upOffset);
        Vec3 p3 = center.subtract(rightOffset).add(upOffset);

        PoseStack.Pose pose = context.poseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.blackHoleWorldQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        emit(vertices, pose, p0, 0.0F, 0.0F);
        emit(vertices, pose, p1, 1.0F, 0.0F);
        emit(vertices, pose, p2, 1.0F, 1.0F);
        emit(vertices, pose, p3, 0.0F, 1.0F);
        buffers.endBatch(type);
    }

    private static Vec3 cameraRight(org.joml.Vector3f cameraLeft) {
        return new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }
}
