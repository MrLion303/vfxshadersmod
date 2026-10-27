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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Cortinas de aurora semitransparentes que rodean el horizonte. Los armónicos
 * geométricos usan ciclos enteros, de modo que el último segmento coincide
 * con el primero y no aparece una junta al cerrar la vuelta.
 */
public final class RainbowAuroraEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "rainbow_aurora");

    private static final int SEGMENTS = 192;
    private static final int VERTICAL_BANDS = 6;
    private static final int LAYERS = 3;
    private static final double BASE_RADIUS = 76.0D;
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
        // Eleva las cortinas para que floten claramente por encima del terreno.
        double centerY = playerPosition.y + 20.0D - cameraPosition.y;
        double centerZ = playerPosition.z - cameraPosition.z;
        double time = age + partialTick;

        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType renderType = VfxRenderTypes.additiveQuads();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        for (int layer = 0; layer < LAYERS; layer++) {
            double radius = BASE_RADIUS + layer * 2.4D;
            float layerAlpha = layer == 0 ? 0.95F : (layer == 1 ? 0.68F : 0.46F);

            for (int i = 0; i < SEGMENTS; i++) {
                double angleA = Math.PI * 2.0D * i / SEGMENTS;
                double angleB = Math.PI * 2.0D * (i + 1) / SEGMENTS;

                double bottomA = bottomWave(angleA, time, layer);
                double bottomB = bottomWave(angleB, time, layer);
                double topA = topWave(angleA, time, layer, bottomA);
                double topB = topWave(angleB, time, layer, bottomB);

                double radiusA = radius + radialWave(angleA, time, layer);
                double radiusB = radius + radialWave(angleB, time, layer);

                for (int band = 0; band < VERTICAL_BANDS; band++) {
                    double f0 = (double) band / VERTICAL_BANDS;
                    double f1 = (double) (band + 1) / VERTICAL_BANDS;
                    double yA0 = interpolate(bottomA, topA, f0);
                    double yA1 = interpolate(bottomA, topA, f1);
                    double yB0 = interpolate(bottomB, topB, f0);
                    double yB1 = interpolate(bottomB, topB, f1);
                    double mid = (f0 + f1) * 0.5D;

                    float fold = (float) (0.78D + 0.22D * (0.5D
                            + 0.5D * Math.sin(angleA * 10.0D + time * 0.045D + layer * 1.3D)));
                    float alpha = (float) (0.018D + 0.46D * Math.pow(Math.sin(mid * Math.PI), 1.45D))
                            * layerAlpha * fold;
                    float hue = (float) (angleA / (Math.PI * 2.0D)
                            + time * 0.00145D + layer * 0.105D + mid * 0.045D);
                    int rgb = VfxGeometry.rainbowRgb(hue);

                    double xA = centerX + Math.cos(angleA) * radiusA;
                    double zA = centerZ + Math.sin(angleA) * radiusA;
                    double xB = centerX + Math.cos(angleB) * radiusB;
                    double zB = centerZ + Math.sin(angleB) * radiusB;

                    VfxGeometry.quad(
                            vertices, pose,
                            xA, centerY + yA0, zA,
                            xB, centerY + yB0, zB,
                            xB, centerY + yB1, zB,
                            xA, centerY + yA1, zA,
                            rgb, alpha
                    );
                }
            }
        }

        buffers.endBatch(renderType);
    }

    private static double bottomWave(double angle, double time, int layer) {
        return 14.0D
                + Math.sin(angle * 4.0D + time * 0.021D + layer * 1.1D) * 2.0D
                + Math.sin(angle * 9.0D - time * 0.032D + layer * 0.7D) * 0.85D;
    }

    private static double topWave(double angle, double time, int layer, double bottom) {
        double height = 30.0D
                + Math.sin(angle * 3.0D - time * 0.016D + layer * 1.7D) * 4.2D
                + Math.sin(angle * 8.0D + time * 0.027D + layer) * 2.5D;
        return Math.max(bottom + 9.0D, height);
    }

    private static double radialWave(double angle, double time, int layer) {
        return Math.sin(angle * 6.0D + time * 0.024D + layer * 1.5D) * 1.15D
                + Math.sin(angle * 12.0D - time * 0.031D + layer) * 0.42D;
    }

    private static double interpolate(double a, double b, double amount) {
        return a + (b - a) * amount;
    }
}
