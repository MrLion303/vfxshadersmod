package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Vórtice celeste reconstruido mediante un volumen de nubes espirales procedurales.
 * El núcleo luminoso y los bancos naranja/rojos se generan en GLSL, sin imagen plana ni duplicados Photon.
 */
public final class CloudVortexEffect implements VfxEffect, ClientVfxRenderer {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "cloud_vortex");
    private static final double SKY_DISTANCE = 224.0D;
    private static final double SKY_ELEVATION = Math.toRadians(28.0D);

    private long age;

    /** Centro del volumen respecto a la cámara, compartido con el emisor Photon. */
    public static Vec3 centerOffset() {
        return new Vec3(0.0D,
                SKY_DISTANCE * Math.sin(SKY_ELEVATION),
                -SKY_DISTANCE * Math.cos(SKY_ELEVATION));
    }

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

    /**
     * El pase ocupa la pantalla, pero no es un plano 2D: cada píxel lanza un rayo
     * contra el elipsoide 3D del vórtice y acumula capas de nube con transparencia.
     */
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

        setFloat(shader, "VortexTime", (float) (age + partialTick));

        Vec3 direction = centerOffset().normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 vortexRight = direction.cross(worldUp).normalize();
        if (vortexRight.lengthSqr() < 1.0E-6D) {
            vortexRight = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 vortexUp = vortexRight.cross(direction).normalize();

        var camera = event.getCamera();
        Vector3f look = camera.getLookVector();
        Vector3f left = camera.getLeftVector();
        Vector3f cameraUp = camera.getUpVector();
        setVector(shader, "CameraForward", new Vec3(look.x, look.y, look.z));
        // Camera.getLeftVector() apunta hacia la izquierda; el shader usa derecha positiva.
        setVector(shader, "CameraRight", new Vec3(-left.x, -left.y, -left.z));
        setVector(shader, "CameraUp", new Vec3(cameraUp.x, cameraUp.y, cameraUp.z));
        setVector(shader, "VortexDirection", direction);
        setVector(shader, "VortexRight", vortexRight);
        setVector(shader, "VortexUp", vortexUp);

        float projectionY = Math.abs(event.getProjectionMatrix().m11());
        if (projectionY > 1.0E-4F) {
            setFloat(shader, "TanHalfFov", 1.0F / projectionY);
        }
        var target = minecraft.getMainRenderTarget();
        if (target.width <= 0 || target.height <= 0) {
            return;
        }
        setFloat(shader, "ScreenAspect", (float) target.width / (float) target.height);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shader);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
            builder.vertex( 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
            builder.vertex( 1.0F,  1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
            builder.vertex(-1.0F,  1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(builder.end());
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void setVector(ShaderInstance shader, String name, Vec3 value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set((float) value.x, (float) value.y, (float) value.z);
        }
    }

    private static void setFloat(ShaderInstance shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
