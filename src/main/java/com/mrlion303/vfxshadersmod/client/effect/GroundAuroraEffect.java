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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Ondas luminosas continuas que viajan por el terreno desde una coordenada. */
public final class GroundAuroraEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "ground_aurora");
    private static final int SEGMENTS = 220;
    private static final double MAX_RADIUS = 50.0D;
    private static volatile Vec3 requestedOrigin = Vec3.ZERO;
    private static volatile int requestedColor = 0x58DFFF;

    private Vec3 origin = requestedOrigin;
    private int color = requestedColor;
    private long age;

    public static void configure(Vec3 position, int rgb) {
        requestedOrigin = position;
        requestedColor = rgb & 0xFFFFFF;
    }

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        origin = requestedOrigin;
        color = requestedColor;
        age = 0L;
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || Minecraft.getInstance().level == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.colorQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        double time = age + partialTick;

        // Varias crestas de distintos ritmos mantienen el fenómeno en marcha sin superar los 50 bloques.
        for (int layer = 0; layer < 6; layer++) {
            double speed = 0.42D + layer * 0.025D;
            double cycle = MAX_RADIUS + 12.0D;
            double phase = layer * (cycle / 6.0D);
            double radius = (time * speed + phase) % cycle;
            double edgeT = Math.max(0.0D, Math.min(1.0D, (MAX_RADIUS - radius) / 8.0D));
            double nearT = Math.max(0.0D, Math.min(1.0D, radius / 4.0D));
            double edgeFade = edgeT * edgeT * (3.0D - 2.0D * edgeT);
            double nearFade = nearT * nearT * (3.0D - 2.0D * nearT);
            float alpha = (float) ((0.38D - layer * 0.032D) * edgeFade * nearFade);
            double width = 0.65D + (layer % 3) * 0.18D;
            int ringColor = layer % 3 == 0 ? lighten(color, 0.38F) : color;

            drawWave(vertices, pose, level, camera, time, radius, width * 1.7D,
                    ringColor, alpha * 0.24F, layer);
            drawWave(vertices, pose, level, camera, time, radius, width,
                    ringColor, alpha, layer);
            drawWave(vertices, pose, level, camera, time, radius, 0.07D,
                    0xE9FFFF, alpha * 0.92F, layer);
        }
        buffers.endBatch(type);
    }

    private void drawWave(
            VertexConsumer vertices, PoseStack.Pose pose, Level level, Vec3 camera, double time,
            double radius, double width, int rgb, float alpha, int layer
    ) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a = Math.PI * 2.0D * segment / SEGMENTS;
            double b = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
            double warpA = Math.sin(a * 5.0D + time * 0.06D + layer * 0.45D) * 0.18D;
            double warpB = Math.sin(b * 5.0D + time * 0.06D + layer * 0.45D) * 0.18D;
            double innerA = Math.max(0.05D, radius - width + warpA);
            double outerA = radius + width + warpA;
            double innerB = Math.max(0.05D, radius - width + warpB);
            double outerB = radius + width + warpB;

            Vec3 p0 = surfacePoint(level, camera, a, innerA, time, layer);
            Vec3 p1 = surfacePoint(level, camera, a, outerA, time, layer);
            Vec3 p2 = surfacePoint(level, camera, b, outerB, time, layer);
            Vec3 p3 = surfacePoint(level, camera, b, innerB, time, layer);
            VfxGeometry.quad(vertices, pose,
                    p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
        }
    }

    private Vec3 surfacePoint(Level level, Vec3 camera, double angle, double radius, double time, int layer) {
        double x = origin.x + Math.cos(angle) * radius;
        double z = origin.z + Math.sin(angle) * radius;
        int yBound = (int) Math.max(level.getMinBuildHeight(),
                Math.min(level.getMaxBuildHeight() - 1, origin.y));
        BlockPos sample = BlockPos.containing(x, yBound, z);
        double groundY = origin.y + 0.15D;
        if (level.hasChunkAt(sample)) {
            groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z)) + 0.12D;
        }

        // Altura ondulada: parece una marea luminosa, sin rellenar el mundo como agua.
        double wave = 0.12D
                + Math.sin(angle * 4.0D - time * 0.055D + layer * 0.6D) * 0.11D
                + Math.sin(angle * 9.0D + time * 0.035D + layer * 0.7D) * 0.035D;
        return new Vec3(x - camera.x, groundY + 0.24D + wave - camera.y, z - camera.z);
    }

    private static int lighten(int rgb, float amount) {
        int r = ((rgb >> 16) & 255);
        int g = ((rgb >> 8) & 255);
        int b = rgb & 255;
        r = Math.round(r + (255 - r) * amount);
        g = Math.round(g + (255 - g) * amount);
        b = Math.round(b + (255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
