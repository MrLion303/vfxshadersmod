package com.mrlion303.vfxshadersmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Operaciones pequeñas y compartidas para generar geometría VFX con el formato
 * POSITION_COLOR de Minecraft. Las coordenadas recibidas son relativas a cámara.
 */
public final class VfxGeometry {
    private VfxGeometry() {
    }

    /**
     * Convierte un matiz HSV a RGB empaquetado, sin crear objetos temporales.
     */
    public static int rainbowRgb(float hue) {
        float wrapped = hue - (float) Math.floor(hue);
        float h = wrapped * 6.0F;
        int sector = (int) h;
        float fraction = h - sector;
        float p = 0.0F;
        float q = 1.0F - fraction;
        float t = fraction;
        float red;
        float green;
        float blue;

        switch (sector % 6) {
            case 0 -> { red = 1.0F; green = t; blue = p; }
            case 1 -> { red = q; green = 1.0F; blue = p; }
            case 2 -> { red = p; green = 1.0F; blue = t; }
            case 3 -> { red = p; green = q; blue = 1.0F; }
            case 4 -> { red = t; green = p; blue = 1.0F; }
            default -> { red = 1.0F; green = p; blue = q; }
        }

        return (Math.round(red * 255.0F) << 16)
                | (Math.round(green * 255.0F) << 8)
                | Math.round(blue * 255.0F);
    }

    public static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            int rgb,
            float alpha
    ) {
        vertex(consumer, pose, x0, y0, z0, rgb, alpha);
        vertex(consumer, pose, x1, y1, z1, rgb, alpha);
        vertex(consumer, pose, x2, y2, z2, rgb, alpha);
        vertex(consumer, pose, x3, y3, z3, rgb, alpha);
    }

    /**
     * Quad vertical con un degradado sencillo de opacidad desde la base hasta
     * la parte superior. Los dos primeros vértices forman el borde inferior.
     */
    public static void gradientQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            int rgb,
            float bottomAlpha,
            float topAlpha
    ) {
        vertex(consumer, pose, x0, y0, z0, rgb, bottomAlpha);
        vertex(consumer, pose, x1, y1, z1, rgb, bottomAlpha);
        vertex(consumer, pose, x2, y2, z2, rgb, topAlpha);
        vertex(consumer, pose, x3, y3, z3, rgb, topAlpha);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            int rgb,
            float alpha
    ) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
