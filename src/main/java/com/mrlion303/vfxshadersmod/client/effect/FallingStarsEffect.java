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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Lluvia de estrellas diagonales con colores arcoíris y desvanecimiento al tocar el suelo. */
public final class FallingStarsEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "falling_stars");
    private static final int STAR_COUNT = 260;
    private static final int LIFE_TICKS = 84;
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        Vec3 playerPosition = player.getPosition(partialTick);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.colorLines();
        VertexConsumer lines = buffers.getBuffer(type);

        for (int i = 0; i < STAR_COUNT; i++) {
            int localAge = (int) Math.floorMod(age + i * 29L, LIFE_TICKS);
            double progress = (double) localAge / LIFE_TICKS;
            double seedX = fract(Math.sin((i + 4) * 127.1D) * 43758.5453D);
            double seedZ = fract(Math.sin((i + 13) * 311.7D) * 24634.6345D);
            double startX = playerPosition.x + (seedX - 0.5D) * 88.0D;
            double startZ = playerPosition.z + (seedZ - 0.5D) * 88.0D;
            double x = startX + progress * 16.0D;
            double z = startZ - progress * 9.0D;
            int bx = (int) Math.floor(x);
            int bz = (int) Math.floor(z);
            BlockPos sample = BlockPos.containing(x, playerPosition.y, z);
            if (!level.hasChunkAt(sample)) {
                continue;
            }
            double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.18D;
            double y = groundY + 52.0D * (1.0D - progress);

            float fadeIn = (float) Math.min(1.0D, progress / 0.07D);
            float fadeOut = (float) Math.min(1.0D, (1.0D - progress) / 0.20D);
            float alpha = Math.max(0.0F, Math.min(fadeIn, fadeOut)) * 0.94F;
            if (alpha < 0.025F) {
                continue;
            }

            float hue = (float) (i * 0.61803398875D + (age + partialTick) * 0.0045D);
            int rgb = VfxGeometry.rainbowRgb(hue);
            Vec3 tail = new Vec3(x - camera.x - 0.82D, y - camera.y + 2.5D, z - camera.z + 0.45D);
            Vec3 head = new Vec3(x - camera.x, y - camera.y, z - camera.z);
            emit(lines, pose, tail, rgb, alpha);
            emit(lines, pose, head, rgb, alpha);
        }
        buffers.endBatch(type);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(r, g, b, alpha).endVertex();
    }
}
