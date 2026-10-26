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

/** Lluvia de estrellas diagonales con estelas visibles y colores arcoíris suaves. */
public final class FallingStarsEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "falling_stars");
    private static final int STAR_COUNT = 320;
    private static final int LIFE_TICKS = 88;
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
        RenderType type = VfxRenderTypes.colorQuads();
        VertexConsumer stars = buffers.getBuffer(type);

        for (int i = 0; i < STAR_COUNT; i++) {
            int localAge = (int) Math.floorMod(age + i * 29L, LIFE_TICKS);
            double progress = (double) localAge / LIFE_TICKS;
            double seedX = fract(Math.sin((i + 4) * 127.1D) * 43758.5453D);
            double seedZ = fract(Math.sin((i + 13) * 311.7D) * 24634.6345D);
            double startX = playerPosition.x + (seedX - 0.5D) * 94.0D;
            double startZ = playerPosition.z + (seedZ - 0.5D) * 94.0D;
            double x = startX + progress * 14.0D;
            double z = startZ - progress * 8.0D;
            int bx = (int) Math.floor(x);
            int bz = (int) Math.floor(z);
            BlockPos sample = BlockPos.containing(x, playerPosition.y, z);
            if (!level.hasChunkAt(sample)) {
                continue;
            }

            double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.12D;
            double y = groundY + 68.0D * (1.0D - progress);
            float fadeIn = (float) Math.min(1.0D, progress / 0.05D);
            float fadeOut = (float) Math.min(1.0D, (1.0D - progress) / 0.12D);
            float alpha = Math.max(0.0F, Math.min(fadeIn, fadeOut)) * 0.96F;
            if (alpha < 0.02F) {
                continue;
            }

            float hue = (float) (i / (double) STAR_COUNT + (age + partialTick) * 0.0032D);
            int rgb = VfxGeometry.rainbowRgb(hue);
            Vec3 tail = new Vec3(x - camera.x - 1.1D, y - camera.y + 1.85D, z - camera.z + 0.68D);
            Vec3 head = new Vec3(x - camera.x, y - camera.y, z - camera.z);
            Vec3 side = new Vec3(-0.18D, 0.0D, -0.18D);

            // Halo ancho y cola más brillante: cada estrella se distingue sin convertir la lluvia en líneas de un píxel.
            emitStreak(stars, pose, tail, head, side.scale(2.6D), rgb, alpha * 0.17F);
            emitStreak(stars, pose, tail, head, side, rgb, alpha);
        }
        buffers.endBatch(type);
    }

    private static void emitStreak(
            VertexConsumer vertices, PoseStack.Pose pose, Vec3 tail, Vec3 head,
            Vec3 side, int rgb, float alpha
    ) {
        Vec3 p0 = tail.subtract(side);
        Vec3 p1 = tail.add(side);
        Vec3 p2 = head.add(side.scale(0.42D));
        Vec3 p3 = head.subtract(side.scale(0.42D));
        VfxGeometry.quad(vertices, pose,
                p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }
}
