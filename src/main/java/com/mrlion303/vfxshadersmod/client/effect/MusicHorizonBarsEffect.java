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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Anillo de barras de estilo ecualizador. Las ondas usan armónicos enteros
 * del ángulo para que el último sector coincida con el primero sin un salto.
 */
public final class MusicHorizonBarsEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "music_horizon_bars");

    private static final int BAR_COUNT = 160;
    private static final double RADIUS = 42.0D;
    private static final double BASE_OFFSET = 0.12D;
    private long age;
    private static volatile Vec3 requestedActivationOrigin;
    private Vec3 origin = Vec3.ZERO;
    private final double[] groundHeights = new double[BAR_COUNT];
    private long groundHeightsUpdatedAt = Long.MIN_VALUE;

    public static void configureActivationOrigin(Vec3 position) {
        requestedActivationOrigin = position;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
        Vec3 configured = requestedActivationOrigin;
        requestedActivationOrigin = null;
        if (configured != null) {
            origin = configured;
        } else {
            var player = Minecraft.getInstance().player;
            origin = player == null
                    ? context.level().getSharedSpawnPos().getCenter()
                    : player.position();
        }
        Arrays.fill(groundHeights, Double.NaN);
        groundHeightsUpdatedAt = Long.MIN_VALUE;
    }

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
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        updateGroundHeights(level);
        Vec3 cameraPosition = event.getCamera().getPosition();
        double centerX = origin.x - cameraPosition.x;
        double centerZ = origin.z - cameraPosition.z;
        double time = age + partialTick;

        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType renderType = VfxRenderTypes.colorQuads();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        for (int i = 0; i < BAR_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / BAR_COUNT;
            // Frecuencias enteras de la vuelta completa: no hay discontinuidad en el cierre.
            double waveA = 0.5D + 0.5D * Math.sin(angle * 4.0D + time * 0.13D);
            double waveB = 0.5D + 0.5D * Math.sin(angle * 7.0D - time * 0.091D + 1.4D);
            double waveC = 0.5D + 0.5D * Math.sin(angle * 13.0D + time * 0.061D);
            double pulse = Math.pow(0.5D + 0.5D * Math.sin(time * 0.095D), 8.0D);
            double height = 0.75D + 3.5D * waveA + 2.2D * waveB + 1.2D * waveC + pulse * 1.2D;

            float hue = (float) (angle / (Math.PI * 2.0D)) + (float) time * 0.0012F;
            int rgb = VfxGeometry.rainbowRgb(hue);
            double groundY = groundHeights[i];
            if (!Double.isFinite(groundY)) continue;
            double baseY = groundY - cameraPosition.y;
            drawBar(vertices, pose, centerX, baseY, centerZ, angle, height, rgb, 1.85D, 0.02F, 0.12F);
            drawBar(vertices, pose, centerX, baseY, centerZ, angle, height, rgb, 1.28D, 0.07F, 0.28F);
            drawBar(vertices, pose, centerX, baseY, centerZ, angle, height, rgb, 0.82D, 0.32F, 0.96F);
        }

        buffers.endBatch(renderType);
    }

    private void updateGroundHeights(Level level) {
        long gameTime = level.getGameTime();
        if (groundHeightsUpdatedAt != Long.MIN_VALUE && gameTime - groundHeightsUpdatedAt < 10L) return;
        groundHeightsUpdatedAt = gameTime;
        int sampleY = Math.max(level.getMinBuildHeight(),
                Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(origin.y)));
        for (int i = 0; i < BAR_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / BAR_COUNT;
            int blockX = (int) Math.floor(origin.x + Math.cos(angle) * RADIUS);
            int blockZ = (int) Math.floor(origin.z + Math.sin(angle) * RADIUS);
            if (!level.hasChunkAt(new BlockPos(blockX, sampleY, blockZ))) {
                groundHeights[i] = Double.NaN;
                continue;
            }
            groundHeights[i] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        }
    }

    private static void drawBar(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            double centerX,
            double baseY,
            double centerZ,
            double angle,
            double height,
            int rgb,
            double width,
            float bottomAlpha,
            float topAlpha
    ) {
        double radialX = Math.cos(angle);
        double radialZ = Math.sin(angle);
        double tangentX = -radialZ;
        double tangentZ = radialX;
        double cx = centerX + radialX * RADIUS;
        double cz = centerZ + radialZ * RADIUS;
        double halfWidth = width * 0.5D;
        double bottom = baseY + BASE_OFFSET;
        double top = bottom + height;

        double leftX = cx - tangentX * halfWidth;
        double leftZ = cz - tangentZ * halfWidth;
        double rightX = cx + tangentX * halfWidth;
        double rightZ = cz + tangentZ * halfWidth;

        VfxGeometry.gradientQuad(
                vertices, pose,
                leftX, bottom, leftZ,
                rightX, bottom, rightZ,
                rightX, top, rightZ,
                leftX, top, leftZ,
                rgb, bottomAlpha, topAlpha
        );
    }
}
