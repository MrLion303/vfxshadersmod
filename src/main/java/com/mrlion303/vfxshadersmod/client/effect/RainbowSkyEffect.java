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
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Cielo de color uniforme que pasa por tonos del arcoíris.
 * La cúpula cubre el cenit completo y el pase de pantalla tiñe también las nubes.
 */
public final class RainbowSkyEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "rainbow_sky");

    private static final int SEGMENTS = 96;
    private static final int VERTICAL_BANDS = 48;
    private static final double RADIUS = 192.0D;
    private static final int HOLD_TICKS = 48;
    private static final int TRANSITION_TICKS = 24;
    private static final int TICKS_PER_COLOR = HOLD_TICKS + TRANSITION_TICKS;

    // Empieza azul como pediste y luego recorre rojo, naranja, amarillo, verde,
    // cian, índigo y violeta. Cada tono se mantiene antes de pasar al siguiente.
    private static final int[] COLORS = {
            0x1B55FF,
            0xFF1C38,
            0xFF7A18,
            0xFFFF26,
            0x1FE34C,
            0x11D5D5,
            0x3D36C7,
            0xA62BFF
    };

    private long age;
    private static volatile int currentColorRgb = COLORS[0];

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
        currentColorRgb = COLORS[0];
    }

    @Override
    public void tick(EffectContext context) {
        age++;
        currentColorRgb = colorAt(age);
    }

    /** Color RGB exacto usado tanto por la cúpula como por el pase que tiñe las nubes. */
    public static int currentColorRgb() {
        return currentColorRgb;
    }

    private static int colorAt(long tick) {
        long cycleTick = Math.floorMod(tick, (long) TICKS_PER_COLOR * COLORS.length);
        int index = (int) (cycleTick / TICKS_PER_COLOR);
        int localTick = (int) (cycleTick % TICKS_PER_COLOR);
        int current = COLORS[index];
        if (localTick < HOLD_TICKS) {
            return current;
        }

        int next = COLORS[(index + 1) % COLORS.length];
        float t = (float) (localTick - HOLD_TICKS) / TRANSITION_TICKS;
        t = t * t * (3.0F - 2.0F * t);
        return blend(current, next, t);
    }

    private static int blend(int a, int b, float t) {
        int red = Math.round(((a >> 16) & 0xFF) * (1.0F - t) + ((b >> 16) & 0xFF) * t);
        int green = Math.round(((a >> 8) & 0xFF) * (1.0F - t) + ((b >> 8) & 0xFF) * t);
        int blue = Math.round((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null) {
            return;
        }

        int rgb = currentColorRgb;
        PoseStack.Pose pose = event.getPoseStack().last();
        RenderType renderType = VfxRenderTypes.colorQuads();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        // Llega hasta PI/2; el último anillo cierra el polo y elimina el círculo vacío de arriba.
        double lowerLatitude = -0.16D;
        double upperLatitude = Math.PI * 0.5D;

        for (int band = 0; band < VERTICAL_BANDS; band++) {
            double f0 = (double) band / VERTICAL_BANDS;
            double f1 = (double) (band + 1) / VERTICAL_BANDS;
            double latitudeA = lowerLatitude + (upperLatitude - lowerLatitude) * f0;
            double latitudeB = lowerLatitude + (upperLatitude - lowerLatitude) * f1;

            for (int i = 0; i < SEGMENTS; i++) {
                double angleA = Math.PI * 2.0D * i / SEGMENTS;
                double angleB = Math.PI * 2.0D * (i + 1) / SEGMENTS;

                var a = domePoint(angleA, latitudeA);
                var b = domePoint(angleB, latitudeA);
                var c = domePoint(angleB, latitudeB);
                var d = domePoint(angleA, latitudeB);

                VfxGeometry.quad(vertices, pose,
                        a.x, a.y, a.z, b.x, b.y, b.z,
                        c.x, c.y, c.z, d.x, d.y, d.z,
                        rgb, 1.0F);
            }
        }

        buffers.endBatch(renderType);
    }

    private static net.minecraft.world.phys.Vec3 domePoint(double azimuth, double latitude) {
        double horizontal = Math.cos(latitude) * RADIUS;
        return new net.minecraft.world.phys.Vec3(
                Math.cos(azimuth) * horizontal,
                Math.sin(latitude) * RADIUS,
                Math.sin(azimuth) * horizontal
        );
    }
}
