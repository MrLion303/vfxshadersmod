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

        // Se abre en poco más de dos segundos y después permanece abierto hasta desactivarse.
        double progress = Math.min(1.0D, (age + partialTick) / 48.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        double radius = 0.35D + eased * 24.0D;
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
        Vec3 p0 = point(center, right, screenUp, -radius, -radius);
        Vec3 p1 = point(center, right, screenUp, radius, -radius);
        Vec3 p2 = point(center, right, screenUp, radius, radius);
        Vec3 p3 = point(center, right, screenUp, -radius, radius);
        portal.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z).uv(0.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z).uv(1.0F, 0.0F).endVertex();
        portal.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z).uv(1.0F, 1.0F).endVertex();
        portal.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z).uv(0.0F, 1.0F).endVertex();
        buffers.endBatch(portalType);

        // Grieta inicial y fracturas brillantes que rodean el disco abierto.
        RenderType lineType = VfxRenderTypes.skyLines();
        VertexConsumer cracks = buffers.getBuffer(lineType);
        float crackAlpha = (float) (0.80D + 0.16D * Math.sin(time * 0.09D));
        if (progress < 0.72D) {
            double slit = radius * 1.3D;
            for (int i = 0; i < 36; i++) {
                double y = -slit + 2.0D * slit * i / 35.0D;
                double x = Math.sin(y * 0.47D + time * 0.025D) * radius * 0.075D;
                emit(cracks, pose, point(center, right, screenUp, x, y), 0xB4F3FF, crackAlpha);
                if (i % 3 == 0) {
                    double branchX = x + Math.sin(i * 2.17D + time * 0.03D) * radius * 0.32D;
                    double branchY = y + Math.cos(i * 1.31D) * radius * 0.18D;
                    emit(cracks, pose, point(center, right, screenUp, x, y), 0xB4F3FF, crackAlpha);
                    emit(cracks, pose, point(center, right, screenUp, branchX, branchY), 0x6FE6FF, crackAlpha * 0.7F);
                }
            }
        }

        final int segments = 180;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            double wobbleA = Math.sin(a * 13.0D + time * 0.035D) * 0.62D;
            double wobbleB = Math.sin(b * 13.0D + time * 0.035D) * 0.62D;
            Vec3 outerA = point(center, right, screenUp, Math.cos(a) * (radius + wobbleA), Math.sin(a) * (radius + wobbleA));
            Vec3 outerB = point(center, right, screenUp, Math.cos(b) * (radius + wobbleB), Math.sin(b) * (radius + wobbleB));
            emit(cracks, pose, outerA, 0xC7F8FF, crackAlpha);
            emit(cracks, pose, outerB, 0x6FE6FF, crackAlpha * 0.82F);
        }

        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0D * i / 28.0D + Math.sin(time * 0.025D + i) * 0.04D;
            double r0 = radius * (0.98D + 0.025D * Math.sin(i * 2.1D));
            double r1 = radius * (1.10D + 0.14D * (0.5D + 0.5D * Math.sin(i * 1.7D + time * 0.04D)));
            double rm = r0 + (r1 - r0) * 0.52D;
            Vec3 c0 = point(center, right, screenUp, Math.cos(angle) * r0, Math.sin(angle) * r0);
            Vec3 cm = point(center, right, screenUp, Math.cos(angle + 0.045D) * rm, Math.sin(angle + 0.045D) * rm);
            Vec3 c1 = point(center, right, screenUp, Math.cos(angle - 0.025D) * r1, Math.sin(angle - 0.025D) * r1);
            emit(cracks, pose, c0, 0xB4F3FF, crackAlpha);
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
