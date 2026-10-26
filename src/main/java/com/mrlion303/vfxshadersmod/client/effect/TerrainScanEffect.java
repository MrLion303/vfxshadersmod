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

/** Escaneo de un solo uso que recorre la superficie cargada del terreno. */
public final class TerrainScanEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "scan");
    private static final int SEGMENTS = 220;
    private static final long LIFETIME = 70L;
    private static volatile Vec3 requestedOrigin = Vec3.ZERO;
    private static volatile int requestedColor = 0x3478FF;

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
    public boolean isFinished() {
        return age >= LIFETIME;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || Minecraft.getInstance().level == null) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.colorQuads();
        VertexConsumer vertices = buffers.getBuffer(type);

        double time = age + partialTick;
        double radius = Math.min(100.0D, 0.5D + time * 1.48D);
        double fadeT = Math.max(0.0D, Math.min(1.0D, (100.0D - radius) / 9.0D));
        float fade = (float) (fadeT * fadeT * (3.0D - 2.0D * fadeT));
        if (fade <= 0.0F || radius >= 100.0D) {
            buffers.endBatch(type);
            return;
        }

        // Un halo ancho lo hace visible; el borde blanco da la sensación de un escáner energético.
        drawBand(vertices, pose, level, camera, radius, 1.05D, color, fade * 0.20F, time);
        drawBand(vertices, pose, level, camera, radius, 0.42D, color, fade * 0.88F, time);
        drawBand(vertices, pose, level, camera, radius, 0.075D, 0xFFFFFF, fade * 0.95F, time);
        buffers.endBatch(type);
    }

    private void drawBand(
            VertexConsumer vertices, PoseStack.Pose pose, Level level, Vec3 camera,
            double radius, double width, int rgb, float alpha, double time
    ) {
        for (int i = 0; i < SEGMENTS; i++) {
            double a = Math.PI * 2.0D * i / SEGMENTS;
            double b = Math.PI * 2.0D * (i + 1) / SEGMENTS;
            double rippleA = Math.sin(a * 10.0D - time * 0.09D) * 0.11D;
            double rippleB = Math.sin(b * 10.0D - time * 0.09D) * 0.11D;

            Vec3 p0 = surfacePoint(level, camera, a, radius - width + rippleA);
            Vec3 p1 = surfacePoint(level, camera, a, radius + width + rippleA);
            Vec3 p2 = surfacePoint(level, camera, b, radius + width + rippleB);
            Vec3 p3 = surfacePoint(level, camera, b, radius - width + rippleB);
            VfxGeometry.quad(vertices, pose,
                    p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, rgb, alpha);
        }
    }

    private Vec3 surfacePoint(Level level, Vec3 camera, double angle, double radius) {
        double x = origin.x + Math.cos(angle) * Math.max(0.05D, radius);
        double z = origin.z + Math.sin(angle) * Math.max(0.05D, radius);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int sampleY = (int) Math.max(minY, Math.min(maxY, origin.y));
        BlockPos sample = BlockPos.containing(x, sampleY, z);
        double y = origin.y + 0.2D;
        if (level.hasChunkAt(sample)) {
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z)) + 0.16D;
        }
        // La altura se consulta en cada vértice: el aro sube por las montañas en vez de atravesarlas.
        return new Vec3(x - camera.x, y - camera.y, z - camera.z);
    }
}
