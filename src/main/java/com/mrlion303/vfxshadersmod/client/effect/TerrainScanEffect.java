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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Efecto de un solo uso: un anillo de escaneo que recorre el relieve del mundo. */
public final class TerrainScanEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "scan");
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
        return age >= 228L;
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
        RenderType type = VfxRenderTypes.colorLines();
        VertexConsumer lines = buffers.getBuffer(type);
        double radius = Math.min(260.0D, (age + partialTick) * 1.15D);
        float fade = (float) Math.min(1.0D, (228.0D - age - partialTick) / 24.0D);
        if (fade <= 0.0F) {
            buffers.endBatch(type);
            return;
        }

        Vec3[] points = new Vec3[160];
        for (int i = 0; i < points.length; i++) {
            double angle = Math.PI * 2.0D * i / points.length;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            BlockPos sample = BlockPos.containing(x, origin.y, z);
            if (!level.hasChunkAt(sample)) {
                continue;
            }
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            double ripple = Math.sin(angle * 12.0D + age * 0.075D) * 0.10D;
            points[i] = new Vec3(x - camera.x, Math.max(origin.y + 0.12D, surface + 0.12D) - camera.y + ripple,
                    z - camera.z);
        }

        for (int i = 0; i < points.length; i++) {
            Vec3 a = points[i];
            Vec3 b = points[(i + 1) % points.length];
            if (a == null || b == null) {
                continue;
            }
            emit(lines, pose, a, color, fade * 0.44F);
            emit(lines, pose, b, color, fade * 0.44F);
        }
        // Un borde interior más claro refuerza el aspecto de escáner sin cubrir los bloques.
        for (int i = 0; i < points.length; i++) {
            Vec3 a = points[i];
            Vec3 b = points[(i + 1) % points.length];
            if (a == null || b == null) {
                continue;
            }
            emit(lines, pose, a.add(0.0D, 0.055D, 0.0D), 0xFFFFFF, fade * 0.78F);
            emit(lines, pose, b.add(0.0D, 0.055D, 0.0D), 0xFFFFFF, fade * 0.78F);
        }
        buffers.endBatch(type);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(r, g, b, alpha).endVertex();
    }
}
