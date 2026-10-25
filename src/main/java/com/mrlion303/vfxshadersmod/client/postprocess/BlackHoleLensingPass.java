package com.mrlion303.vfxshadersmod.client.postprocess;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrlion303.vfxshadersmod.client.ClientVfxManager;
import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleSkyEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowSkyEffect;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aplica una lente gravitacional fuerte, una sombra negra y el disco brillante
 * alrededor de los agujeros negros. El mismo pase tiñe el cielo y las nubes
 * cuando está activo rainbow_sky.
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

        boolean skyHoleActive = ClientVfxManager.isActive(BlackHoleSkyEffect.ID);
        boolean rainbowSkyActive = ClientVfxManager.isActive(RainbowSkyEffect.ID);
        List<WorldVfxInstance> holes = ClientWorldVfxInstances.forLevel(minecraft.level).stream()
                .filter(instance -> instance.effectId().equals(BlackHoleEffect.ID))
                .filter(instance -> instance.position().distanceToSqr(event.getCamera().getPosition())
                        <= 192.0D * 192.0D)
                .sorted(Comparator.comparingDouble(instance ->
                        instance.position().distanceToSqr(event.getCamera().getPosition())))
                .limit(MAX_HOLES - (skyHoleActive ? 1 : 0))
                .toList();

        if (holes.isEmpty() && !skyHoleActive && !rainbowSkyActive) {
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

            // La posición del agujero celeste es un vector fijo del mundo, no el ángulo
            // de la cámara cuando se activa. Su altura es de 50 grados sobre el horizonte.
            if (skyHoleActive) {
                double radius = BlackHoleSkyEffect.SKY_DIAMETER * 0.5D;
                Vec3 skyCenter = cameraPosition.add(BlackHoleSkyEffect.centerOffset());
                Vec3 skyEdge = skyCenter.add(
                        cameraLeft.x * radius,
                        cameraLeft.y * radius,
                        cameraLeft.z * radius
                );
                float[] center = project(skyCenter, camera, event);
                float[] edge = project(skyEdge, camera, event);
                if (center != null && edge != null) {
                    float dx = (edge[0] - center[0]) * aspect;
                    float dy = edge[1] - center[1];
                    float projectedRadius = Math.max(0.012F, (float) Math.sqrt(dx * dx + dy * dy));
                    screenHoles.add(new float[]{center[0], center[1], projectedRadius, 1.55F});
                }
            }

            for (WorldVfxInstance hole : holes) {
                double diameter = hole.parameters().getOrDefault(
                        "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
                double radius = Math.max(0.5D, diameter * 0.5D);
                double distance = hole.position().distanceTo(cameraPosition);

                // Al entrar en el horizonte se mantiene la distorsión en pantalla,
                // en lugar de perderla por la proyección de un punto demasiado cercano.
                if (distance <= radius) {
                    float spread = (float) Math.min(0.62D,
                            0.22D + 0.40D * (1.0D - distance / radius));
                    screenHoles.add(new float[]{0.5F, 0.5F, spread, 2.2F});
                    continue;
                }

                float[] center = project(hole.position(), camera, event);
                Vec3 edgePosition = hole.position().add(
                        cameraLeft.x * radius,
                        cameraLeft.y * radius,
                        cameraLeft.z * radius
                );
                float[] edge = project(edgePosition, camera, event);
                if (center == null || edge == null) {
                    continue;
                }

                float dx = (edge[0] - center[0]) * aspect;
                float dy = edge[1] - center[1];
                float projectedRadius = Math.max(0.006F, (float) Math.sqrt(dx * dx + dy * dy));
                float strength = (float) (1.35D + 0.65D * Math.max(0.0D, 1.0D - distance / 160.0D));
                screenHoles.add(new float[]{center[0], center[1], projectedRadius, strength});
            }

            shader.setSampler("DiffuseSampler", (java.util.function.IntSupplier) main::getColorTextureId);
            shader.setSampler("DepthSampler", (java.util.function.IntSupplier) main::getDepthTextureId);
            setUniform(shader, "ScreenSize", new float[]{main.width, main.height});
            setUniform(shader, "RainbowSkyActive", new float[]{rainbowSkyActive ? 1.0F : 0.0F});

            int rgb = RainbowSkyEffect.currentColorRgb();
            setUniform(shader, "RainbowSkyColor", new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F,
                    1.0F
            });

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
                LOGGER.error("No se pudo aplicar el pase de lente/graduación de cielo; se mantiene el renderizado normal.", exception);
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

    /**
     * Proyección mediante los vectores reales de la cámara. Evita aplicar por
     * duplicado la rotación/matriz del pose stack, causa típica de centros inválidos.
     */
    private static float[] project(
            Vec3 worldPosition, Camera camera, RenderLevelStageEvent event
    ) {
        Vec3 relative = worldPosition.subtract(camera.getPosition());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vector3f look = camera.getLookVector();

        Vec3 leftVector = new Vec3(left.x, left.y, left.z);
        Vec3 upVector = new Vec3(up.x, up.y, up.z);
        Vec3 lookVector = new Vec3(look.x, look.y, look.z);
        double depth = relative.dot(lookVector);
        if (depth <= 0.01D) {
            return null;
        }

        float cameraX = (float) -relative.dot(leftVector);
        float cameraY = (float) relative.dot(upVector);
        Vector4f point = new Vector4f(cameraX, cameraY, (float) -depth, 1.0F);
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
