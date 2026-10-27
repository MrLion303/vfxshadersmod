package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxGeometry;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mrlion303.vfxshadersmod.core.physics.VfxFieldMath;
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
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Pulso circular de un solo uso. Renderiza una malla transparente sobre la superficie
 * del terreno; el impulso físico de los jugadores se calcula en el servidor.
 */
public final class TrampolineWaveEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "trampolin");
    private static final double GRID_STEP = 0.75D;
    private static final double WAVE_FREQUENCY = 0.88D;
    private static final double COVERAGE_SOFTNESS = GRID_STEP * 2.0D;
    private static final int BASE_COLOR = 0x245CFF;
    private static final int CREST_COLOR = 0xD9FFFF;

    private static volatile Vec3 requestedOrigin = Vec3.ZERO;
    private static volatile double requestedPower = 12.0D;
    private static volatile double requestedRadius = 32.0D;
    private static volatile double requestedSpeed = 0.65D;

    private Vec3 origin = Vec3.ZERO;
    private double power = requestedPower;
    private double radius = requestedRadius;
    private double speed = requestedSpeed;
    private long age;
    private final Map<Long, Double> groundHeightCache = new HashMap<>();

    public static Vec3 configuredOrigin() {
        return requestedOrigin;
    }

    public static double configuredRadius() {
        return requestedRadius;
    }

    public static double configuredSpeed() {
        return requestedSpeed;
    }

    public static void configure(Vec3 position, double peakHeight, double waveRadius, double waveSpeed) {
        requestedOrigin = position;
        requestedPower = clamp(peakHeight, 1.0D, 64.0D);
        requestedRadius = clamp(waveRadius, 4.0D, 64.0D);
        requestedSpeed = clamp(waveSpeed, 0.15D, 2.0D);
    }

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        origin = requestedOrigin;
        power = requestedPower;
        radius = requestedRadius;
        speed = requestedSpeed;
        age = 0L;
        groundHeightCache.clear();
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public boolean isFinished() {
        return age >= (long) Math.ceil(radius / speed) + 10L;
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

        double time = age + partialTick;
        double travelTime = radius / speed;
        double frontRadius = Math.min(radius, time * speed);
        double fadeIn = smoothstep(0.0D, 5.0D, time);
        double fadeOut = 1.0D - smoothstep(travelTime - 8.0D, travelTime + 2.0D, time);
        double energy = fadeIn * fadeOut;
        if (energy <= 0.001D) {
            return;
        }

        // La potencia representa altura aproximada del salto. Su equivalente visual
        // se limita para mantener el efecto pegado al terreno y evitar picos gigantes.
        double amplitude = Math.min(1.8D, 0.45D + Math.sqrt(power) * 0.18D);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.additiveQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        int cells = (int) Math.ceil(radius / GRID_STEP);

        for (int gx = -cells; gx < cells; gx++) {
            double localX0 = gx * GRID_STEP;
            double localX1 = localX0 + GRID_STEP;
            for (int gz = -cells; gz < cells; gz++) {
                double localZ0 = gz * GRID_STEP;
                double localZ1 = localZ0 + GRID_STEP;
                double centerX = (localX0 + localX1) * 0.5D;
                double centerZ = (localZ0 + localZ1) * 0.5D;
                double centerDistance = Math.sqrt(centerX * centerX + centerZ * centerZ);
                if (centerDistance > radius || centerDistance > frontRadius + COVERAGE_SOFTNESS) {
                    continue;
                }

                SurfacePoint p0 = surfacePoint(level, camera, localX0, localZ0,
                        frontRadius, time, amplitude, energy);
                SurfacePoint p1 = surfacePoint(level, camera, localX0, localZ1,
                        frontRadius, time, amplitude, energy);
                SurfacePoint p2 = surfacePoint(level, camera, localX1, localZ1,
                        frontRadius, time, amplitude, energy);
                SurfacePoint p3 = surfacePoint(level, camera, localX1, localZ0,
                        frontRadius, time, amplitude, energy);
                if (p0 == null || p1 == null || p2 == null || p3 == null) {
                    continue;
                }

                double coverage = (p0.coverage + p1.coverage + p2.coverage + p3.coverage) * 0.25D;
                double crest = (p0.crest + p1.crest + p2.crest + p3.crest) * 0.25D;
                float alpha = (float) (energy * coverage * (0.25D + crest * 0.48D));
                if (alpha < 0.012F) {
                    continue;
                }
                int color = mixRgb(BASE_COLOR, CREST_COLOR, (float) clamp(crest, 0.0D, 1.0D));
                VfxGeometry.quad(vertices, pose,
                        p0.x, p0.y, p0.z,
                        p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z,
                        p3.x, p3.y, p3.z,
                        color, Math.min(0.78F, alpha));
            }
        }

        // Un borde cian intenso marca el frente de la onda para que la deformación
        // se lea incluso sobre césped, piedra y superficies con iluminación oscura.
        drawWaveFront(vertices, pose, level, camera, frontRadius, time, amplitude, energy);
        buffers.endBatch(type);
    }

    private void drawWaveFront(
            VertexConsumer vertices, PoseStack.Pose pose, Level level, Vec3 camera,
            double frontRadius, double time, double amplitude, double energy
    ) {
        if (energy <= 0.01D || frontRadius <= 0.05D) {
            return;
        }

        final int segments = 160;
        double innerRadius = Math.max(0.0D, frontRadius - 0.48D);
        double outerRadius = Math.min(radius, frontRadius + 0.48D);
        float alpha = (float) Math.min(0.92D, energy * 0.88D);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;

            SurfacePoint p0 = surfacePoint(level, camera,
                    Math.cos(a0) * innerRadius, Math.sin(a0) * innerRadius,
                    frontRadius, time, amplitude, energy);
            SurfacePoint p1 = surfacePoint(level, camera,
                    Math.cos(a1) * innerRadius, Math.sin(a1) * innerRadius,
                    frontRadius, time, amplitude, energy);
            SurfacePoint p2 = surfacePoint(level, camera,
                    Math.cos(a1) * outerRadius, Math.sin(a1) * outerRadius,
                    frontRadius, time, amplitude, energy);
            SurfacePoint p3 = surfacePoint(level, camera,
                    Math.cos(a0) * outerRadius, Math.sin(a0) * outerRadius,
                    frontRadius, time, amplitude, energy);
            if (p0 == null || p1 == null || p2 == null || p3 == null) {
                continue;
            }
            VfxGeometry.quad(vertices, pose,
                    p0.x, p0.y, p0.z,
                    p1.x, p1.y, p1.z,
                    p2.x, p2.y, p2.z,
                    p3.x, p3.y, p3.z,
                    CREST_COLOR, alpha);
        }
    }

    private SurfacePoint surfacePoint(
            Level level, Vec3 camera, double localX, double localZ,
            double frontRadius, double time, double amplitude, double energy
    ) {
        double distance = Math.sqrt(localX * localX + localZ * localZ);
        double radialCoverage = smoothstep(
                0.0D, COVERAGE_SOFTNESS, frontRadius + COVERAGE_SOFTNESS - distance);
        double edgeFade = 1.0D - smoothstep(radius - 3.0D, radius, distance);
        double coverage = radialCoverage * edgeFade;
        double phase = (distance - frontRadius) * WAVE_FREQUENCY;
        double crest = 0.5D + 0.5D * Math.sin(phase);
        double worldX = origin.x + localX;
        double worldZ = origin.z + localZ;
        double groundY = groundHeight(level, worldX, worldZ);
        if (!Double.isFinite(groundY)) {
            return null;
        }

        // Los valles llegan casi hasta el suelo y las crestas sobresalen: el terreno
        // parece ondular sin sustituir los bloques ni alterar su colisión permanente.
        double waveLift = amplitude * coverage * energy * (0.5D + 0.5D * Math.sin(phase));
        return new SurfacePoint(
                worldX - camera.x,
                groundY + 0.035D + waveLift - camera.y,
                worldZ - camera.z,
                coverage,
                crest
        );
    }

    private double groundHeight(Level level, double worldX, double worldZ) {
        int blockX = (int) Math.floor(worldX);
        int blockZ = (int) Math.floor(worldZ);
        long key = (((long) blockX) << 32) ^ (blockZ & 0xFFFFFFFFL);
        Double cached = groundHeightCache.get(key);
        if (cached != null) {
            return cached;
        }

        int sampleY = (int) clamp(Math.floor(origin.y), level.getMinBuildHeight(),
                level.getMaxBuildHeight() - 1.0D);
        BlockPos sample = new BlockPos(blockX, sampleY, blockZ);
        if (!level.hasChunkAt(sample)) {
            return Double.NaN;
        }
        double height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        groundHeightCache.put(key, height);
        return height;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        return VfxFieldMath.smoothstep(edge0, edge1, value);
    }

    private static double clamp(double value, double min, double max) {
        return VfxFieldMath.clamp(value, min, max);
    }

    private static int mixRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1.0F - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1.0F - t) + ((b >> 8) & 255) * t);
        int blue = Math.round((a & 255) * (1.0F - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | blue;
    }

    private record SurfacePoint(
            double x, double y, double z, double coverage, double crest
    ) {
    }
}
