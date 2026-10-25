package com.mrlion303.vfxshadersmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Objects;

/** Contexto común para dibujar efectos situados en el mundo. */
public record VfxRenderContext(
        Level level,
        Camera camera,
        Vec3 cameraPosition,
        PoseStack poseStack,
        RenderLevelStageEvent.Stage stage,
        float partialTick,
        long gameTime
) {
    public VfxRenderContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(cameraPosition, "cameraPosition");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(stage, "stage");
    }

    public static VfxRenderContext from(RenderLevelStageEvent event) {
        Objects.requireNonNull(event, "event");
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException("No hay un mundo de cliente durante el renderizado VFX");
        }

        Camera camera = event.getCamera();
        return new VfxRenderContext(level, camera, camera.getPosition(),
                event.getPoseStack(), event.getStage(), event.getPartialTick(), level.getGameTime());
    }

    /** Convierte una posición absoluta del mundo a coordenadas relativas a cámara. */
    public Vec3 toCameraRelative(Vec3 worldPosition) {
        return worldPosition.subtract(cameraPosition);
    }

    public boolean isWithinDistance(Vec3 worldPosition, double maxDistance) {
        return maxDistance >= 0.0D
                && worldPosition.distanceToSqr(cameraPosition) <= maxDistance * maxDistance;
    }
}
