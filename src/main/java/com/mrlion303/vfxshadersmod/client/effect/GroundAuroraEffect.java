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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Ondas luminosas continuas que viajan por el terreno desde una coordenada. */
public final class GroundAuroraEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "ground_aurora");
    private static final int SEGMENTS = 440;
    private static final double MAX_RADIUS = 50.0D;
    private static final double MAX_SURFACE_STEP = 2.0D;
    private static final int SURFACE_CACHE_TICKS = 20;
    private static volatile Vec3 requestedOrigin = Vec3.ZERO;
    private static volatile int requestedColor = 0x58DFFF;

    private Vec3 origin = requestedOrigin;
    private int color = requestedColor;
    private long age;
    private final Map<Long, Double> connectedSurfaceHeights = new HashMap<>();
    private long surfaceMapBuiltAt = Long.MIN_VALUE;

    public static Vec3 configuredOrigin() {
        return requestedOrigin;
    }

    public static int configuredColor() {
        return 0xFF000000 | requestedColor;
    }

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
        connectedSurfaceHeights.clear();
        surfaceMapBuiltAt = Long.MIN_VALUE;
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
        prepareConnectedSurface(level);
        if (connectedSurfaceHeights.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.additiveQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        double time = age + partialTick;

        // Varias crestas de distintos ritmos mantienen el fenómeno en marcha sin superar los 50 bloques.
        for (int layer = 0; layer < 4; layer++) {
            double speed = 0.42D + layer * 0.025D;
            double cycle = MAX_RADIUS + 12.0D;
            double phase = layer * (cycle / 4.0D);
            double radius = (time * speed + phase) % cycle;
            double edgeT = Math.max(0.0D, Math.min(1.0D, (MAX_RADIUS - radius) / 8.0D));
            double nearT = Math.max(0.0D, Math.min(1.0D, radius / 4.0D));
            double edgeFade = edgeT * edgeT * (3.0D - 2.0D * edgeT);
            double nearFade = nearT * nearT * (3.0D - 2.0D * nearT);
            float alpha = (float) ((0.36D - layer * 0.045D) * edgeFade * nearFade);
            double width = 0.16D + (layer % 2) * 0.075D;
            int ringColor = layer % 3 == 0 ? lighten(color, 0.38F) : color;

            drawWave(vertices, pose, level, camera, time, radius, width * 1.7D,
                    ringColor, alpha * 0.13F, layer);
            drawWave(vertices, pose, level, camera, time, radius, width,
                    ringColor, alpha, layer);
            drawWave(vertices, pose, level, camera, time, radius, 0.07D,
                    0xE9FFFF, alpha * 0.72F, layer);
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

            // Omite los vértices que no tienen un camino conectado a la superficie de origen.
            if (p0 == null || p1 == null || p2 == null || p3 == null) continue;
            VfxGeometry.quad(vertices, pose,
                    p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
        }
    }

    private Vec3 surfacePoint(Level level, Vec3 camera, double angle, double radius, double time, int layer) {
        double x = origin.x + Math.cos(angle) * radius;
        double z = origin.z + Math.sin(angle) * radius;
        double groundY = connectedSurfaceHeights.getOrDefault(
                columnKey((int) Math.floor(x), (int) Math.floor(z)), Double.NaN);
        if (!Double.isFinite(groundY)) return null;
        return new Vec3(x - camera.x, groundY + 0.035D - camera.y, z - camera.z);
    }

    /**
     * Recorre la superficie desde el suelo bajo el origen. No acepta saltos verticales
     * mayores de dos bloques, de modo que un bloque aislado diez bloques arriba queda fuera
     * del mapa y no puede atraer el aro de aurora hacia él.
     */
    private void prepareConnectedSurface(Level level) {
        long now = level.getGameTime();
        if (surfaceMapBuiltAt != Long.MIN_VALUE && now - surfaceMapBuiltAt < SURFACE_CACHE_TICKS) return;
        surfaceMapBuiltAt = now;
        connectedSurfaceHeights.clear();

        int originX = (int) Math.floor(origin.x);
        int originZ = (int) Math.floor(origin.z);
        double seedHeight = findSeedSurface(level, originX, originZ);
        if (!Double.isFinite(seedHeight)) return;

        int reach = (int) Math.ceil(MAX_RADIUS + 3.0D);
        Queue<SurfaceColumn> frontier = new ArrayDeque<>();
        connectedSurfaceHeights.put(columnKey(originX, originZ), seedHeight);
        frontier.add(new SurfaceColumn(originX, originZ, seedHeight));
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!frontier.isEmpty()) {
            SurfaceColumn current = frontier.remove();
            for (int[] direction : directions) {
                int nextX = current.x() + direction[0];
                int nextZ = current.z() + direction[1];
                if (Math.abs(nextX - originX) > reach || Math.abs(nextZ - originZ) > reach) continue;
                long key = columnKey(nextX, nextZ);
                if (connectedSurfaceHeights.containsKey(key)) continue;

                double height = columnTop(level, nextX, nextZ);
                if (!Double.isFinite(height)
                        || Math.abs(height - current.surfaceY()) > MAX_SURFACE_STEP) continue;

                connectedSurfaceHeights.put(key, height);
                frontier.add(new SurfaceColumn(nextX, nextZ, height));
            }
        }
    }

    /** Busca hacia abajo desde el origen, no hacia plataformas suspendidas por encima. */
    private double findSeedSurface(Level level, int x, int z) {
        int maxY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(origin.y));
        int minY = Math.max(level.getMinBuildHeight(), maxY - 8);
        if (maxY < minY || !level.hasChunkAt(new BlockPos(x, maxY, z))) return Double.NaN;
        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && state.isFaceSturdy(level, pos, Direction.UP)) {
                return y + 1.0D;
            }
        }
        return Double.NaN;
    }

    private double columnTop(Level level, int x, int z) {
        int sampleY = Math.max(level.getMinBuildHeight(),
                Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(origin.y)));
        if (!level.hasChunkAt(new BlockPos(x, sampleY, z))) return Double.NaN;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static long columnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    private record SurfaceColumn(int x, int z, double surfaceY) {
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
