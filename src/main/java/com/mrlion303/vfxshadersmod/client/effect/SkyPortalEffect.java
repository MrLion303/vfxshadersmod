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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.renderer.ShaderInstance;

/** Portal cinematográfico en el cielo, con una grieta cian y un interior de galaxia. */
public final class SkyPortalEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "sky_portal");
    private static final ResourceLocation PORTAL_TEXTURE =
            new ResourceLocation(VfxShadersMod.MOD_ID, "textures/vfx/panorama_overlay.png");
    private static final double SKY_DISTANCE = 148.0D;
    private static final double ELEVATION = Math.toRadians(52.0D);
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

        double progress = Math.min(1.0D, (age + partialTick) / 72.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        double radius = 0.45D + eased * 20.5D;
        double time = age + partialTick;

        boolean hasOverlay = minecraft.getResourceManager().getResource(PORTAL_TEXTURE).isPresent();
        int textureId = minecraft.getTextureManager().getTexture(PORTAL_TEXTURE).getId();
        shader.setSampler("DiffuseSampler", () -> textureId);
        if (shader.getUniform("UseTexture") != null) {
            shader.getUniform("UseTexture").set(hasOverlay ? 1.0F : 0.0F);
        }
        if (shader.getUniform("PortalTime") != null) {
            shader.getUniform("PortalTime").set((float) time);
        }

        // El cuadrado se recorta en el shader en forma de disco, así que no hay esquinas ni una caja visible.
        RenderType portalType = VfxRenderTypes.portalQuads();
        VertexConsumer portal = buffers.getBuffer(portalType);
        Vec3 p0 = point(center, right, screenUp, -radius, -radius);
        Vec3 p1 = point(center, right, screenUp, radius, -radius);
        Vec3 p2 = point(center, right, screenUp, radius, radius);
        Vec3 p3 = point(center, right, screenUp, -radius, radius);
        portal.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z).uv(0.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z).uv(1.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z).uv(1.0F, 1.0F).endVertex();
        portal.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z).uv(0.0F, 1.0F).endVertex();
        buffers.endBatch(portalType);

        // La grieta se abre antes que el portal; después quedan bordes fracturados y ramificaciones de energía.
        RenderType lineType = VfxRenderTypes.colorLines();
        VertexConsumer cracks = buffers.getBuffer(lineType);
        float crackAlpha = (float) (0.78D + 0.17D * Math.sin(time * 0.09D));
        if (progress < 0.72D) {
            double slit = radius * 1.25D;
            for (int i = 0; i < 18; i++) {
                double f0 = (double) i / 18.0D;
                double f1 = (double) (i + 1) / 18.0D;
                double y0 = -slit + 2.0D * slit * f0;
                double y1 = -slit + 2.0D * slit * f1;
                double x0 = Math.sin(y0 * 0.47D + time * 0.025D) * radius * 0.055D;
                double x1 = Math.sin(y1 * 0.53D + time * 0.025D) * radius * 0.055D;
                emit(cracks, pose, point(center, right, screenUp, x0, y0), 0xB4F3FF, crackAlpha);
                emit(cracks, pose, point(center, right, screenUp, x1, y1), 0xB4F3FF, crackAlpha);
            }
        }

        final int segments = 144;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            double wobbleA = Math.sin(a * 13.0D + time * 0.035D) * 0.58D;
            double wobbleB = Math.sin(b * 13.0D + time * 0.035D) * 0.58D;
            Vec3 a0 = point(center, right, screenUp, Math.cos(a) * radius, Math.sin(a) * radius);
            Vec3 a1 = point(center, right, screenUp, Math.cos(a) * (radius + wobbleA), Math.sin(a) * (radius + wobbleA));
            Vec3 b0 = point(center, right, screenUp, Math.cos(b) * radius, Math.sin(b) * radius);
            Vec3 b1 = point(center, right, screenUp, Math.cos(b) * (radius + wobbleB), Math.sin(b) * (radius + wobbleB));
            emit(cracks, pose, a0, 0x6FE6FF, crackAlpha);
            emit(cracks, pose, a1, 0xC7F8FF, crackAlpha);
            emit(cracks, pose, a1, 0x6FE6FF, crackAlpha * 0.8F);
            emit(cracks, pose, b1, 0x6FE6FF, crackAlpha * 0.8F);
        }

        for (int i = 0; i < 22; i++) {
            double angle = Math.PI * 2.0D * i / 22.0D + Math.sin(time * 0.025D + i) * 0.04D;
            double r0 = radius * (0.97D + 0.025D * Math.sin(i * 2.1D));
            double r1 = radius * (1.10D + 0.11D * (0.5D + 0.5D * Math.sin(i * 1.7D + time * 0.04D)));
            double rm = r0 + (r1 - r0) * 0.52D;
            Vec3 c0 = point(center, right, screenUp, Math.cos(angle) * r0, Math.sin(angle) * r0);
            Vec3 cm = point(center, right, screenUp,
                    Math.cos(angle + 0.045D) * rm, Math.sin(angle + 0.045D) * rm);
            Vec3 c1 = point(center, right, screenUp,
                    Math.cos(angle - 0.025D) * r1, Math.sin(angle - 0.025D) * r1);
            emit(cracks, pose, c0, 0xB4F3FF, crackAlpha);
            emit(cracks, pose, cm, 0x6FE6FF, crackAlpha * 0.9F);
            emit(cracks, pose, cm, 0x6FE6FF, crackAlpha * 0.9F);
            emit(cracks, pose, c1, 0xB4F3FF, crackAlpha * 0.65F);
        }
        buffers.endBatch(lineType);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double x, double y) {
        return center.add(right.scale(x)).add(up.scale(y));
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(r, g, b, alpha).endVertex();
    }
}
