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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Agujero negro celeste de acreción y lente gravitacional, generado enteramente en GLSL. */
public final class BlackHoleSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole_sky");
    public static final double SKY_DISTANCE = 512.0D;
    public static final double SKY_DIAMETER = 2340.0D;
    public static final double SKY_ELEVATION_DEGREES = 14.0D;
    // Unidades visuales normalizadas para que 780 -> 2340 sea realmente un aumento de 3×
    // sin que el plano completo desborde la cámara y convierta el disco en un borde gigante.
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

    /** Vector de mundo relativo a cámara, compartido con el pase de lente. */
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null) {
            return;
        }

        ShaderInstance shader = VfxShaderLibrary.blackHoleSurface();
        if (shader == null) {
            return;
        }
        com.mrlion303.vfxshadersmod.client.render.VfxReferenceTextures.bind(
                shader, "BlackHoleReference", "textures/vfx/blackhole_disk_reference.png");
        if (shader.getUniform("BlackHoleTime") != null) {
            shader.getUniform("BlackHoleTime").set((float) (age + partialTick));
        }

        Vec3 center = centerOffset;
        double halfWidth = SKY_HALF_WIDTH;
        double halfHeight = halfWidth / 2.07D;
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(VfxRenderTypes.blackHoleSkyQuads());

        Vec3 topLeft = point(center, -halfWidth, halfHeight);
        Vec3 topRight = point(center, halfWidth, halfHeight);
        Vec3 bottomRight = point(center, halfWidth, -halfHeight);
        Vec3 bottomLeft = point(center, -halfWidth, -halfHeight);

        emit(vertices, pose, topLeft, 0.0F, 0.0F);
        emit(vertices, pose, topRight, 1.0F, 0.0F);
        emit(vertices, pose, bottomRight, 1.0F, 1.0F);
        emit(vertices, pose, bottomLeft, 0.0F, 1.0F);
        buffers.endBatch(VfxRenderTypes.blackHoleSkyQuads());
    }

    private static Vec3 point(Vec3 center, double x, double y) {
        return center.add(planeRight.scale(x)).add(planeUp.scale(y));
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }
}
