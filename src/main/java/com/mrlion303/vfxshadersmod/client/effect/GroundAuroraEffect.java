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
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Ondas luminosas expansivas que recorren el suelo desde una coordenada concreta. */
public final class GroundAuroraEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "ground_aurora");
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

        // Cada capa repite una onda grande, con crestas que siguen el relieve del terreno.
        for (int layer = 0; layer < 5; layer++) {
            double radius = (time * 0.62D + layer * 8.0D) % 230.0D;
            double width = 1.0D + (layer % 2) * 0.65D;
            float alpha = 0.38F - layer * 0.052F;
            for (int segment = 0; segment < 160; segment++) {
                double a = Math.PI * 2.0D * segment / 160.0D;
                double b = Math.PI * 2.0D * (segment + 1) / 160.0D;
                double ra = radius + Math.sin(a * 7.0D - time * 0.09D + layer) * 1.1D;
                double rb = radius + Math.sin(b * 7.0D - time * 0.09D + layer) * 1.1D;

                double x0 = origin.x + Math.cos(a) * ra;
                double z0 = origin.z + Math.sin(a) * ra;
                double x1 = origin.x + Math.cos(b) * rb;
                double z1 = origin.z + Math.sin(b) * rb;
                double surface0 = surfaceY(level, x0, origin.y, z0);
                double surface1 = surfaceY(level, x1, origin.y, z1);
                double wave0 = Math.sin(a * 5.0D + time * 0.065D + layer * 0.8D) * 0.62D;
                double wave1 = Math.sin(b * 5.0D + time * 0.065D + layer * 0.8D) * 0.62D;

                VfxGeometry.quad(vertices, pose,
                        x0 - camera.x, surface0 + wave0 - camera.y, z0 - camera.z,
                        x0 + Math.cos(a) * width - camera.x, surface0 + wave0 + 0.23D - camera.y, z0 + Math.sin(a) * width - camera.z,
                        x1 + Math.cos(b) * width - camera.x, surface1 + wave1 + 0.23D - camera.y, z1 + Math.sin(b) * width - camera.z,
                        x1 - camera.x, surface1 + wave1 - camera.y, z1 - camera.z,
                        color, alpha);
            }
        }
        buffers.endBatch(type);
    }

    private static double surfaceY(Level level, double x, double originY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        BlockPos sample = BlockPos.containing(x, originY, z);
        if (!level.hasChunkAt(sample)) {
            return originY + 0.2D;
        }
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        return Math.max(originY, height) + 0.15D;
    }
}
