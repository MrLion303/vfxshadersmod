package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Set;

/** Renderizador del agujero negro de mundo: toda la forma se calcula por píxel en GLSL. */
public final class BlackHoleVfxRenderer implements ClientWorldVfxRenderer {
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
            shader.getUniform("BlackHoleTime").set(
                    (float) (context.gameTime() + context.partialTick()));
        }

        double diameter = instance.parameters().getOrDefault(
                "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
        double requestedRadius = Math.max(0.5D, diameter * 0.5D);
        double distanceToCenter = instance.position().distanceTo(context.cameraPosition());
        double radius = Math.min(requestedRadius, Math.max(0.025D, distanceToCenter * 0.40D));
        Vec3 center = context.toCameraRelative(instance.position());

        var cameraLeft = context.camera().getLeftVector();
        var cameraUp = context.camera().getUpVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z).normalize();
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z).normalize();

        double halfWidth = radius * 2.2D;
        double halfHeight = halfWidth / 2.07D;
        Vec3 topLeft = point(center, right, up, -halfWidth, halfHeight);
        Vec3 topRight = point(center, right, up, halfWidth, halfHeight);
        Vec3 bottomRight = point(center, right, up, halfWidth, -halfHeight);
        Vec3 bottomLeft = point(center, right, up, -halfWidth, -halfHeight);

        PoseStack.Pose pose = context.poseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var type = VfxRenderTypes.blackHoleWorldQuads();
        VertexConsumer vertices = buffers.getBuffer(type);

        emit(vertices, pose, topLeft, 0.0F, 0.0F);
        emit(vertices, pose, topRight, 1.0F, 0.0F);
        emit(vertices, pose, bottomRight, 1.0F, 1.0F);
        emit(vertices, pose, bottomLeft, 0.0F, 1.0F);
        buffers.endBatch(type);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double x, double y) {
        return center.add(right.scale(x)).add(up.scale(y));
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }
}
