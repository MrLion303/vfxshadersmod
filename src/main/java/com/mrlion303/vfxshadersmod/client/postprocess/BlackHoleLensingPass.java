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
import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.client.ClientVfxManager;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleSkyEffect;
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
 * alrededor de los agujeros negros. Los efectos de cielo se renderizan aparte
 * para no alterar el resto de la imagen.
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
                .filter(instance -> instance.position().distanceToSqr(event.getCamera().getPosition())
                        <= 192.0D * 192.0D)
                .sorted(Comparator.comparingDouble(instance ->
                        instance.position().distanceToSqr(event.getCamera().getPosition())))
                .limit(MAX_HOLES)
                .toList();
        // Además de los agujeros físicos, el agujero celeste también curva el cielo y el
        // terreno que aparecen detrás, como una lente gravitacional real de pantalla.
        boolean skyHoleActive = ClientVfxManager.isActive(BlackHoleSkyEffect.ID);
        if (holes.isEmpty() && !skyHoleActive) {
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
                if (screenHoles.size() >= MAX_HOLES) {
                    break;
                }
                double diameter = hole.parameters().getOrDefault(
                        "diameter", (double) BlackHoleEffect.DEFAULT_DIAMETER);
                double configuredRadius = Math.max(0.5D, diameter * 0.5D);
                double horizonRadius = configuredRadius;
                double distance = hole.position().distanceTo(cameraPosition);

                // Si la cámara cruza el horizonte físico, la sombra debe ocupar la vista.
                // Fuera de él la proyección es perspectiva y crece al acercarse.
                if (distance <= horizonRadius) {
                    screenHoles.add(new float[]{0.5F, 0.5F, 0.48F, 1.45F});
                    continue;
                }

                float[] center = project(hole.position(), camera, event);
                Vec3 edgePosition = hole.position().add(
                        cameraLeft.x * horizonRadius,
                        cameraLeft.y * horizonRadius,
                        cameraLeft.z * horizonRadius
                );
                float[] edge = project(edgePosition, camera, event);
                if (center == null || edge == null) {
                    continue;
                }

                float dx = (edge[0] - center[0]) * aspect;
                float dy = edge[1] - center[1];
                float projectedRadius = Math.min(0.46F,
                        Math.max(0.0015F, (float) Math.sqrt(dx * dx + dy * dy)));
                float strength = (float) Math.min(1.45D,
                        1.22D + 0.23D * Math.max(0.0D, 1.0D - distance / 192.0D));
                screenHoles.add(new float[]{center[0], center[1], projectedRadius, strength});
            }

            if (skyHoleActive && screenHoles.size() < MAX_HOLES) {
                Vec3 skyCenter = cameraPosition.add(BlackHoleSkyEffect.centerOffset());
                float[] center = project(skyCenter, camera, event);
                double skyHalfWidth = BlackHoleSkyEffect.SKY_DIAMETER * 0.90D;
                double horizonRadius = skyHalfWidth * 0.145D;
                Vec3 skyEdge = skyCenter.add(
                        cameraLeft.x * horizonRadius,
                        cameraLeft.y * horizonRadius,
                        cameraLeft.z * horizonRadius
                );
                float[] edge = project(skyEdge, camera, event);
                if (center != null && edge != null) {
                    float dx = (edge[0] - center[0]) * aspect;
                    float dy = edge[1] - center[1];
                    float radius = Math.min(0.46F,
                            Math.max(0.0015F, (float) Math.sqrt(dx * dx + dy * dy)));
                    screenHoles.add(new float[]{center[0], center[1], radius, 1.36F});
                }
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

            // Captura la imagen original como textura de entrada antes de dibujar al destino auxiliar.
            main.bindRead();
            scratch.bindWrite(true);
            scratch.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            scratch.clear(false);
            shader.setSampler("DiffuseSampler", (java.util.function.IntSupplier) main::getColorTextureId);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, main.getColorTextureId());

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
                LOGGER.error("No se pudo aplicar la distorsión del agujero negro; se mantiene el renderizado normal.", exception);
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
