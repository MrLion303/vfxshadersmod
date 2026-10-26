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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Vórtice de nubes rojo-anaranjadas con textura procedural y un núcleo incandescente. */
public final class CloudVortexEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "cloud_vortex");
    private static final double SKY_DISTANCE = 224.0D;
    private static final double ELEVATION = Math.toRadians(51.0D);
    private static final double HALF_SIZE = 160.0D;
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
        ShaderInstance shader = VfxShaderLibrary.cloudVortex();
        if (shader == null) {
            return;
        }

        var camera = event.getCamera();
        var cameraLook = camera.getLookVector();
        Vec3 forward = new Vec3(cameraLook.x, cameraLook.y, cameraLook.z).normalize();
        var cameraLeft = camera.getLeftVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z).normalize();
        var cameraUp = camera.getUpVector();
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z).normalize();

        // El volumen se orienta en una dirección fija del mundo, no hacia la cámara.
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 direction = new Vec3(0.0D, Math.sin(ELEVATION), -Math.cos(ELEVATION)).normalize();
        Vec3 vortexRight = direction.cross(worldUp).normalize();
        Vec3 vortexUp = vortexRight.cross(direction).normalize();

        float fov = minecraft.options.fov().get().floatValue();
        float tanHalfFov = (float) Math.tan(Math.toRadians(fov) * 0.5D);
        float aspect = minecraft.getWindow().getWidth()
                / (float) Math.max(1, minecraft.getWindow().getHeight());

        setVector(shader, "CameraForward", forward);
        setVector(shader, "CameraRight", right);
        setVector(shader, "CameraUp", up);
        setVector(shader, "VortexDirection", direction);
        setVector(shader, "VortexRight", vortexRight);
        setVector(shader, "VortexUp", vortexUp);
        if (shader.getUniform("TanHalfFov") != null) {
            shader.getUniform("TanHalfFov").set(tanHalfFov);
        }
        if (shader.getUniform("ScreenAspect") != null) {
            shader.getUniform("ScreenAspect").set(aspect);
        }
        if (shader.getUniform("VortexTime") != null) {
            shader.getUniform("VortexTime").set((float) (age + partialTick));
        }

        // El shader calcula el volumen por rayo sobre el cielo ya dibujado.
        // No se proyecta una tarjeta rectangular, por lo que no existe borde de plano.
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = VfxRenderTypes.cloudQuads();
        VertexConsumer vertices = buffers.getBuffer(type);
        vertices.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        vertices.vertex( 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        vertices.vertex( 1.0F,  1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        vertices.vertex(-1.0F,  1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffers.endBatch(type);
    }

    private static void setVector(ShaderInstance shader, String name, Vec3 vector) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(new float[]{
                    (float) vector.x, (float) vector.y, (float) vector.z
            });
        }
    }

}
