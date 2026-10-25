package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * VFX de prueba del motor: anillos orbitales alrededor del jugador local.
 * Sirve para probar el registro, los comandos, el runtime y el renderizado.
 */
public final class DemoOrbitEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "demo_orbit");

    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        Vec3 playerPosition = player.getPosition(partialTick);
        Vec3 cameraPosition = event.getCamera().getPosition();
        double centerX = playerPosition.x - cameraPosition.x;
        double centerY = playerPosition.y + 1.0D - cameraPosition.y;
        double centerZ = playerPosition.z - cameraPosition.z;
        double phase = (age + partialTick) * 0.045D;

        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType lineType = VfxRenderTypes.colorLines();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(lineType);

        drawRing(vertices, pose, centerX, centerY, centerZ, 1.12D, phase, 0,
                0.76F, 0.34F, 1.0F);
        drawRing(vertices, pose, centerX, centerY, centerZ, 0.88D, -phase * 1.25D, 1,
                0.25F, 0.88F, 1.0F);
        drawRing(vertices, pose, centerX, centerY, centerZ, 0.68D, phase * 0.72D, 2,
                1.0F, 0.35F, 0.78F);

        buffers.endBatch(lineType);
    }

    private static void drawRing(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            double phase,
            int plane,
            float red,
            float green,
            float blue
    ) {
        final int segments = 96;
        Vec3 previous = ringPoint(centerX, centerY, centerZ, radius, phase, plane);

        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i / segments) + phase;
            Vec3 current = ringPoint(centerX, centerY, centerZ, radius, angle, plane);
            vertex(vertices, pose, previous, red, green, blue);
            vertex(vertices, pose, current, red, green, blue);
            previous = current;
        }
    }

    private static Vec3 ringPoint(
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            double angle,
            int plane
    ) {
        double cosine = Math.cos(angle) * radius;
        double sine = Math.sin(angle) * radius;
        return switch (plane) {
            case 1 -> new Vec3(centerX + cosine, centerY + sine * 0.82D, centerZ);
            case 2 -> new Vec3(centerX, centerY + sine * 0.82D, centerZ + cosine);
            default -> new Vec3(centerX + cosine, centerY, centerZ + sine);
        };
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 point,
            float red,
            float green,
            float blue
    ) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, 1.0F)
                .endVertex();
    }
}
