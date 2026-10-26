package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Portal circular que se abre desde una grieta cian y revela la galaxia del proyecto. */
public final class SkyPortalEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "sky_portal");
    private static final ResourceLocation PORTAL_TEXTURE =
            new ResourceLocation(VfxShadersMod.MOD_ID, "textures/vfx/panorama_overlay.png");
    private static final double SKY_DISTANCE = 148.0D;
    private static final double ELEVATION = Math.toRadians(90.0D);
    private long age;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        age = 0L;
    }

    @Override
    public void tick(EffectContext context) {
        age++;
    }

    @Override
    public void render(RenderLevelStageEvent event, float partialTick) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shader = VfxShaderLibrary.portalWindow();
        if (shader == null) {
            return;
        }

        Vec3 direction = new Vec3(0.0D, Math.sin(ELEVATION), -Math.cos(ELEVATION));
        Vec3 center = direction.scale(SKY_DISTANCE);
        var camera = event.getCamera();
        var left = camera.getLeftVector();
        var up = camera.getUpVector();
        Vec3 right = new Vec3(-left.x, -left.y, -left.z).normalize();
        Vec3 screenUp = new Vec3(up.x, up.y, up.z).normalize();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        // Se abre en poco más de dos segundos y después permanece abierto hasta desactivarse.
        double progress = Math.min(1.0D, (age + partialTick) / 48.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        double radius = 0.35D + eased * 28.0D;
        double opening = Math.min(1.0D, Math.pow(Math.max(0.0D, progress / 0.46D), 1.25D));
        double radiusX = radius * Math.max(0.025D, opening);
        double time = age + partialTick;

        boolean hasOverlay = minecraft.getResourceManager().getResource(PORTAL_TEXTURE).isPresent();
        int textureId = minecraft.getTextureManager().getTexture(PORTAL_TEXTURE).getId();
        shader.setSampler("DiffuseSampler", (java.util.function.IntSupplier) () -> textureId);
        if (shader.getUniform("UseTexture") != null) {
            shader.getUniform("UseTexture").set(hasOverlay ? 1.0F : 0.0F);
        }
        if (shader.getUniform("PortalTime") != null) {
            shader.getUniform("PortalTime").set((float) time);
        }

        RenderType portalType = VfxRenderTypes.portalQuads();
        VertexConsumer portal = buffers.getBuffer(portalType);
        Vec3 p0 = point(center, right, screenUp, -radiusX, -radius);
        Vec3 p1 = point(center, right, screenUp, radiusX, -radius);
        Vec3 p2 = point(center, right, screenUp, radiusX, radius);
        Vec3 p3 = point(center, right, screenUp, -radiusX, radius);
        portal.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z).uv(0.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z).uv(1.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z).uv(1.0F, 1.0F).endVertex();
        portal.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z).uv(0.0F, 1.0F).endVertex();
        buffers.endBatch(portalType);

        // Grieta inicial y fracturas brillantes que rodean el disco abierto.
        RenderType lineType = VfxRenderTypes.skyLines();
        VertexConsumer cracks = buffers.getBuffer(lineType);
        float crackAlpha = (float) (0.80D + 0.16D * Math.sin(time * 0.09D));
        if (progress < 0.62D) {
            double slit = 2.0D + progress * 34.0D;
            for (int i = 0; i < 40; i++) {
                double y0 = -slit + 2.0D * slit * i / 40.0D;
                double y1 = -slit + 2.0D * slit * (i + 1) / 40.0D;
                double x0 = Math.sin(y0 * 0.29D + time * 0.027D) * (0.8D + progress * 2.4D);
                double x1 = Math.sin(y1 * 0.29D + time * 0.027D) * (0.8D + progress * 2.4D);
                emitSegment(cracks, pose, point(center, right, screenUp, x0, y0),
                        point(center, right, screenUp, x1, y1), 0xC7F8FF, crackAlpha);
                if (i % 5 == 0) {
                    double branchX = x0 + Math.sin(i * 2.17D + time * 0.03D) * (2.0D + progress * 11.0D);
                    double branchY = y0 + Math.cos(i * 1.31D) * (1.5D + progress * 6.0D);
                    emitSegment(cracks, pose, point(center, right, screenUp, x0, y0),
                            point(center, right, screenUp, branchX, branchY), 0x6FE6FF, crackAlpha * 0.72F);
                }
            }
        }

        // Borde fracturado: faltan segmentos y quedan astillas del cielo suspendidas.
        final int segments = 216;
        for (int i = 0; i < segments; i++) {
            if (i % 17 == 0 || i % 29 == 0) continue;
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            double wobbleA = Math.sin(a * 13.0D + time * 0.035D) * (0.55D + opening * 0.35D);
            double wobbleB = Math.sin(b * 13.0D + time * 0.035D) * (0.55D + opening * 0.35D);
            double xA = Math.cos(a) * (radiusX + wobbleA);
            double xB = Math.cos(b) * (radiusX + wobbleB);
            double yA = Math.sin(a) * (radius + wobbleA);
            double yB = Math.sin(b) * (radius + wobbleB);
            emitSegment(cracks, pose, point(center, right, screenUp, xA, yA),
                    point(center, right, screenUp, xB, yB), 0xC7F8FF, crackAlpha * 0.94F);
        }

        for (int i = 0; i < 34; i++) {
            double angle = Math.PI * 2.0D * i / 34.0D + Math.sin(time * 0.025D + i) * 0.04D;
            double r0 = 0.94D + 0.04D * Math.sin(i * 2.1D);
            double r1 = 1.06D + 0.18D * (0.5D + 0.5D * Math.sin(i * 1.7D + time * 0.04D));
            double rx0 = radiusX * r0;
            double rx1 = radiusX * r1;
            double ry0 = radius * r0;
            double ry1 = radius * r1;
            Vec3 c0 = point(center, right, screenUp, Math.cos(angle) * rx0, Math.sin(angle) * ry0);
            Vec3 cm = point(center, right, screenUp,
                    Math.cos(angle + 0.045D) * (rx0 + rx1) * 0.5D,
                    Math.sin(angle + 0.045D) * (ry0 + ry1) * 0.5D);
            Vec3 c1 = point(center, right, screenUp, Math.cos(angle - 0.025D) * rx1, Math.sin(angle - 0.025D) * ry1);
            emitSegment(cracks, pose, c0, cm, 0xB4F3FF, crackAlpha * 0.95F);
            emitSegment(cracks, pose, cm, c1, 0x6FE6FF, crackAlpha * 0.78F);
        }
        buffers.endBatch(lineType);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double x, double y) {
        return center.add(right.scale(x)).add(up.scale(y));
    }

    private static void emitSegment(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, int rgb, float alpha
    ) {
        emit(consumer, pose, start, rgb, alpha);
        emit(consumer, pose, end, rgb, alpha);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(r, g, b, alpha).endVertex();
    }
}
