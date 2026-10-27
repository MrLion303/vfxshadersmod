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
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Gran brecha espacial con anillo incandescente, fracturas y una columna alienígena. */
public final class SkyPortalEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "sky_portal");
    private static final double SKY_DISTANCE = 150.0D;
    private static final double ELEVATION = Math.toRadians(24.0D);
    private static final double MAX_RADIUS = 124.0D;
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

        // Carga el panorama del proyecto como textura real del portal, también tras recargar recursos.
        ResourceLocation portalTextureId = new ResourceLocation(
                VfxShadersMod.MOD_ID, "textures/vfx/panorama_overlay.png");
        var textureManager = minecraft.getTextureManager();
        if (!(textureManager.getTexture(portalTextureId) instanceof SimpleTexture)) {
            textureManager.register(portalTextureId, new SimpleTexture(portalTextureId));
        }
        var portalTexture = textureManager.getTexture(portalTextureId);
        shader.setSampler("PortalSampler", (java.util.function.IntSupplier) portalTexture::getId);

        Vec3 direction = new Vec3(0.0D, Math.sin(ELEVATION), -Math.cos(ELEVATION)).normalize();
        Vec3 center = direction.scale(SKY_DISTANCE);
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = direction.cross(worldUp).normalize();
        Vec3 portalUp = right.cross(direction).normalize();

        double time = age + partialTick;
        double progress = Math.min(1.0D, time / 48.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        double radius = 0.35D + eased * MAX_RADIUS;
        double opening = Math.min(1.0D, Math.pow(Math.max(0.0D, progress / 0.42D), 1.18D));
        double radiusX = radius * Math.max(0.025D, opening);

        if (shader.getUniform("PortalTime") != null) {
            shader.getUniform("PortalTime").set((float) time);
        }

        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType portalType = VfxRenderTypes.portalQuads();
        VertexConsumer portal = buffers.getBuffer(portalType);

        Vec3 p0 = point(center, right, portalUp, -radiusX, -radius);
        Vec3 p1 = point(center, right, portalUp, radiusX, -radius);
        Vec3 p2 = point(center, right, portalUp, radiusX, radius);
        Vec3 p3 = point(center, right, portalUp, -radiusX, radius);
        emitUv(portal, pose, p0, 0.0F, 0.0F);
        emitUv(portal, pose, p1, 1.0F, 0.0F);
        emitUv(portal, pose, p2, 1.0F, 1.0F);
        emitUv(portal, pose, p3, 0.0F, 1.0F);
        buffers.endBatch(portalType);

        // Fracturas ardientes que salen del anillo como grietas en el cielo.
        RenderType lineType = VfxRenderTypes.skyLines();
        VertexConsumer cracks = buffers.getBuffer(lineType);
        float pulse = (float) (0.76D + 0.20D * Math.sin(time * 0.075D));

        for (int i = 0; i < 52; i++) {
            double angle = Math.PI * 2.0D * i / 52.0D
                    + Math.sin(time * 0.006D + i * 2.7D) * 0.012D;
            double startR = radius * (0.84D + 0.10D * fract(Math.sin(i * 17.3D) * 817.2D));
            double endR = radius * (1.02D + 0.17D * fract(Math.sin(i * 31.1D) * 123.7D));
            double bend = (fract(Math.sin(i * 7.1D) * 72.3D) - 0.5D) * 0.18D;
            Vec3 start = point(center, right, portalUp,
                    Math.cos(angle) * startR * radiusX / radius,
                    Math.sin(angle) * startR);
            Vec3 middle = point(center, right, portalUp,
                    Math.cos(angle + bend) * (startR + endR) * 0.5D * radiusX / radius,
                    Math.sin(angle + bend) * (startR + endR) * 0.5D);
            Vec3 end = point(center, right, portalUp,
                    Math.cos(angle + bend * 1.8D) * endR * radiusX / radius,
                    Math.sin(angle + bend * 1.8D) * endR);
            emitLine(cracks, pose, start, middle, 0xFF6A2C, pulse * 0.88F);
            emitLine(cracks, pose, middle, end, 0xFFC17A, pulse * 0.74F);

            if (i % 3 == 0) {
                double branchAngle = angle + (i % 2 == 0 ? 1.0D : -1.0D) * 0.07D;
                Vec3 branch = point(center, right, portalUp,
                        Math.cos(branchAngle) * endR * 1.12D * radiusX / radius,
                        Math.sin(branchAngle) * endR * 1.12D);
                emitLine(cracks, pose, middle, branch, 0xFF8A3B, pulse * 0.58F);
            }
        }

        // Un pulso de luz vertical en el centro recuerda la columna que conecta la grieta con el horizonte.
        if (progress < 0.72D) {
            double beamLength = radius * 1.40D;
            Vec3 beamStart = point(center, right, portalUp, -1.4D, -beamLength * 0.52D);
            Vec3 beamEnd = point(center, right, portalUp, 1.0D, beamLength * 0.58D);
            emitLine(cracks, pose, beamStart, beamEnd, 0xFFC58C, pulse * 0.45F);
        }
        buffers.endBatch(lineType);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static Vec3 point(Vec3 center, Vec3 right, Vec3 up, double x, double y) {
        return center.add(right.scale(x)).add(up.scale(y));
    }

    private static void emitUv(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .uv(u, v).endVertex();
    }

    private static void emitLine(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, int rgb, float alpha
    ) {
        emitVertex(consumer, pose, start, rgb, alpha);
        emitVertex(consumer, pose, end, rgb, alpha);
    }

    private static void emitVertex(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int rgb, float alpha
    ) {
        float red = ((rgb >> 16) & 255) / 255.0F;
        float green = ((rgb >> 8) & 255) / 255.0F;
        float blue = (rgb & 255) / 255.0F;
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
