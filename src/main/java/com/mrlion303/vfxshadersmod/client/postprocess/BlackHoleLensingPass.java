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
import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pasa de posprocesamiento para la lente gravitacional. Proyecta los agujeros
 * negros visibles a pantalla y deforma las UV de la escena, haciendo que los
 * bloques y el paisaje alrededor se vean curvados al mirar a través de ellos.
 */
public final class BlackHoleLensingPass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_HOLES = 8;
    private static TextureTarget scratch;
    private static int scratchWidth = -1;
    private static int scratchHeight = -1;
    private static boolean loggedFailure;

    private BlackHoleLensingPass() {
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        List<WorldVfxInstance> holes = ClientWorldVfxInstances.forLevel(minecraft.level).stream()
                .filter(instance -> instance.effectId().equals(BlackHoleEffect.ID))
                .filter(instance -> instance.position().distanceToSqr(event.getCamera().getPosition()) <= 192.0D * 192.0D)
                .sorted(Comparator.comparingDouble(instance ->
                        instance.position().distanceToSqr(event.getCamera().getPosition())))
                .limit(MAX_HOLES)
                .toList();

        if (holes.isEmpty()) {
            releaseTarget();
            return;
        }

        ShaderInstance shader = VfxShaderLibrary.blackHoleLensing();
        if (shader == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) {
            return;
        }
        ensureTarget(main.width, main.height);

        try {
            float aspect = (float) main.width / (float) main.height;
            List<float[]> screenHoles = new ArrayList<>(MAX_HOLES);
            Camera camera = event.getCamera();
            Vec3 cameraPosition = camera.getPosition();
            Vector3f cameraLeft = camera.getLeftVector();

            for (WorldVfxInstance hole : holes) {
                double diameter = hole.parameters().getOrDefault(
                        "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
                double radius = Math.max(0.5D, diameter * 0.5D);
                float[] center = project(hole.position(), cameraPosition, event);
                Vec3 edgePosition = hole.position().add(
                        cameraLeft.x * radius,
                        cameraLeft.y * radius,
                        cameraLeft.z * radius
                );
                float[] edge = project(edgePosition, cameraPosition, event);

                if (center == null || edge == null) {
                    continue;
                }

                float dx = (edge[0] - center[0]) * aspect;
                float dy = edge[1] - center[1];
                float projectedRadius = Math.max(0.0012F, (float) Math.sqrt(dx * dx + dy * dy));
                double distance = hole.position().distanceTo(cameraPosition);
                float strength = (float) (0.24D + 0.48D * Math.max(0.0D, 1.0D - distance / 150.0D));
                screenHoles.add(new float[]{center[0], center[1], projectedRadius, strength});
            }

            if (screenHoles.isEmpty()) {
                return;
            }

            shader.setSampler("DiffuseSampler", (java.util.function.IntSupplier) main::getColorTextureId);
            setUniform(shader, "ScreenSize", new float[]{main.width, main.height});
            for (int i = 0; i < MAX_HOLES; i++) {
                float[] data = i < screenHoles.size()
                        ? screenHoles.get(i)
                        : new float[]{-10.0F, -10.0F, 0.0F, 0.0F};
                setUniform(shader, "Hole" + i, data);
            }

            scratch.bindWrite(true);
            scratch.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            scratch.clear(false);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
            builder.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
            builder.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
            builder.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(builder.end());

            scratch.unbindWrite();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratch.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
            GL30.glBlitFramebuffer(
                    0, 0, scratch.width, scratch.height,
                    0, 0, main.width, main.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
            );
            main.bindWrite(false);
            loggedFailure = false;
        } catch (RuntimeException exception) {
            if (!loggedFailure) {
                LOGGER.error("No se pudo aplicar la lente gravitacional VFX; se mantiene el renderizado normal.", exception);
                loggedFailure = true;
            }
        } finally {
            if (scratch != null) {
                scratch.unbindWrite();
            }
            main.bindWrite(false);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static float[] project(Vec3 worldPosition, Vec3 cameraPosition, RenderLevelStageEvent event) {
        Vec3 relative = worldPosition.subtract(cameraPosition);
        Vector4f point = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
        event.getPoseStack().last().pose().transform(point);
        event.getProjectionMatrix().transform(point);

        if (point.w <= 0.0001F) {
            return null;
        }

        float ndcX = point.x / point.w;
        float ndcY = point.y / point.w;
        float u = ndcX * 0.5F + 0.5F;
        float v = ndcY * 0.5F + 0.5F;
        if (u < -0.5F || u > 1.5F || v < -0.5F || v > 1.5F) {
            return null;
        }
        return new float[]{u, v};
    }

    private static void setUniform(ShaderInstance shader, String name, float[] values) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(values);
        }
    }

    private static void ensureTarget(int width, int height) {
        if (scratch == null) {
            scratch = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            scratchWidth = width;
            scratchHeight = height;
            scratch.setFilterMode(GL11.GL_LINEAR);
            return;
        }

        if (scratchWidth != width || scratchHeight != height) {
            scratch.resize(width, height, Minecraft.ON_OSX);
            scratchWidth = width;
            scratchHeight = height;
        }
    }

    public static void releaseTarget() {
        if (scratch != null) {
            scratch.destroyBuffers();
            scratch = null;
            scratchWidth = -1;
            scratchHeight = -1;
        }
    }
}
