package com.mrlion303.vfxshadersmod.client.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrlion303.vfxshadersmod.client.effect.TrampolineWaveEffect;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Distorsiona texturas y siluetas del terreno usando profundidad y una cresta radial móvil. */
public final class TrampolineWaveDistortionPass {
    private static TextureTarget scratch;
    private static int scratchWidth = -1;
    private static int scratchHeight = -1;

    private TrampolineWaveDistortionPass() {}

    public static void render(RenderLevelStageEvent event, TrampolineWaveEffect effect) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ShaderInstance shader = VfxShaderLibrary.trampolineWave();
        if (shader == null) return;

        double energy = effect.waveEnergy(event.getPartialTick());
        if (energy < 0.015D) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0 || main.getDepthTextureId() <= 0) return;
        ensureTarget(main.width, main.height);

        try {
            var camera = event.getCamera();
            Vec3 cameraPosition = camera.getPosition();
            Vector3f left = camera.getLeftVector();
            Vector3f up = camera.getUpVector();
            Vector3f forward = camera.getLookVector();
            Vec3 rightVector = new Vec3(-left.x, -left.y, -left.z);
            Vec3 upVector = new Vec3(up.x, up.y, up.z);
            Vec3 forwardVector = new Vec3(forward.x, forward.y, forward.z);

            shader.setSampler("DiffuseSampler", (java.util.function.IntSupplier) main::getColorTextureId);
            shader.setSampler("DepthSampler", (java.util.function.IntSupplier) main::getDepthTextureId);
            set(shader, "ScreenSize", new float[]{main.width, main.height});
            set(shader, "CameraPosition", new float[]{(float)cameraPosition.x,(float)cameraPosition.y,(float)cameraPosition.z});
            set(shader, "CameraRight", new float[]{(float)rightVector.x,(float)rightVector.y,(float)rightVector.z});
            set(shader, "CameraUp", new float[]{(float)upVector.x,(float)upVector.y,(float)upVector.z});
            set(shader, "CameraForward", new float[]{(float)forwardVector.x,(float)forwardVector.y,(float)forwardVector.z});
            Vec3 origin = effect.waveOrigin();
            set(shader, "WaveOrigin", new float[]{(float)origin.x,(float)origin.y,(float)origin.z});
            set(shader, "WaveParams", new float[]{
                    (float)effect.waveFrontRadius(event.getPartialTick()),
                    (float)effect.waveRadius(), (float)effect.waveAmplitude(), (float)energy
            });
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            setMatrix(shader, "ProjMat", projection);
            setMatrix(shader, "InverseProjectionMat", new Matrix4f(projection).invert());

            main.bindRead();
            scratch.bindWrite(true);
            scratch.setClearColor(0, 0, 0, 1);
            scratch.clear(false);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, main.getColorTextureId());

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(-1, -1, 0).uv(0, 0).endVertex();
            builder.vertex( 1, -1, 0).uv(1, 0).endVertex();
            builder.vertex( 1,  1, 0).uv(1, 1).endVertex();
            builder.vertex(-1,  1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(builder.end());

            scratch.unbindWrite();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratch.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, scratch.width, scratch.height, 0, 0, main.width, main.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(false);
        } finally {
            scratch.unbindWrite();
            main.bindWrite(false);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void set(ShaderInstance shader, String name, float[] values) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(values);
    }
    private static void setMatrix(ShaderInstance shader, String name, Matrix4f matrix) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(matrix);
    }
    private static void ensureTarget(int width, int height) {
        if (scratch == null) {
            scratch = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            scratchWidth = width; scratchHeight = height;
            scratch.setFilterMode(GL11.GL_LINEAR);
        } else if (scratchWidth != width || scratchHeight != height) {
            scratch.resize(width, height, Minecraft.ON_OSX);
            scratchWidth = width; scratchHeight = height;
        }
    }
    public static void releaseTarget() {
        if (scratch != null) {
            scratch.destroyBuffers(); scratch = null;
            scratchWidth = -1; scratchHeight = -1;
        }
    }
}
