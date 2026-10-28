package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.client.render.VfxRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Efectos visuales ligados a jugadores; no modifica su movimiento ni sus colisiones. */
public final class ClientPlayerVfxRenderer {
    private static final Map<Integer, Set<String>> EFFECTS = new HashMap<>();
    private static final Set<Integer> FLYING_POSES = new HashSet<>();
    private static final Map<Integer, RenderPoseState> ORIGINAL_POSES = new HashMap<>();

    private ClientPlayerVfxRenderer() {}

    public static void setPlayerEffect(int entityId, String effect, boolean enabled) {
        Set<String> active = EFFECTS.computeIfAbsent(entityId, ignored -> new HashSet<>());
        if (enabled) active.add(effect);
        else active.remove(effect);
        if (active.isEmpty()) EFFECTS.remove(entityId);
    }

    public static void setFlyingPose(int entityId, boolean enabled) {
        Level level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(entityId);
        if (enabled) {
            FLYING_POSES.add(entityId);
            if (entity instanceof Player player) {
                ORIGINAL_POSES.putIfAbsent(entityId, RenderPoseState.capture(player));
                applySwimmingPose(player);
            }
            return;
        }
        FLYING_POSES.remove(entityId);
        RenderPoseState saved = ORIGINAL_POSES.remove(entityId);
        if (entity instanceof Player player && saved != null) saved.restore(player);
    }

    /** Mantiene la pose visual sincronizada en cada tick del cliente, sin conceder vuelo real. */
    public static void tick(Level level) {
        if (level == null || level != Minecraft.getInstance().level) return;
        for (int entityId : FLYING_POSES) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Player player) || !player.isAlive()) continue;
            ORIGINAL_POSES.putIfAbsent(entityId, RenderPoseState.capture(player));
            applySwimmingPose(player);
        }
        if ((level.getGameTime() & 1L) != 0L) return;
        for (Map.Entry<Integer, Set<String>> entry : EFFECTS.entrySet()) {
            if (!entry.getValue().contains("aura")) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Player player) || !player.isAlive()) continue;

            double time = level.getGameTime() * 0.16D;
            for (int i = 0; i < 5; i++) {
                double angle = time + i * (Math.PI * 2.0D / 5.0D);
                double height = 0.15D + ((i * 3 + (int) (level.getGameTime() / 2L)) % 9) * 0.19D;
                double radius = 0.34D + 0.05D * Math.sin(time * 1.4D + i);
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                level.addParticle(ParticleTypes.FLAME, x, player.getY() + height, z,
                        Math.cos(angle) * 0.006D, 0.025D + (i % 3) * 0.009D, Math.sin(angle) * 0.006D);
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || EFFECTS.isEmpty()) return;

        PoseStack.Pose pose = event.getPoseStack().last();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var lineType = VfxRenderTypes.additiveLines();
        VertexConsumer vertices = buffers.getBuffer(lineType);
        boolean drew = false;

        for (Map.Entry<Integer, Set<String>> entry : EFFECTS.entrySet()) {
            if (!entry.getValue().contains("ondas")) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Player player) || !player.isAlive()) continue;
            Vec3 position = player.getPosition(event.getPartialTick());
            Vec3 center = new Vec3(position.x - camera.x, position.y + 0.92D - camera.y, position.z - camera.z);
            double phase = (level.getGameTime() + event.getPartialTick()) * 0.035D;
            drawRing(vertices, pose, center, 0.82D, 0.66D, 0.22D, phase,
                    0.16F, 0.88F, 1.0F);
            drawRing(vertices, pose, center, 0.82D, -0.66D, -0.22D, -phase * 1.17D,
                    0.74F, 0.36F, 1.0F);
            drew = true;
        }
        if (drew) buffers.endBatch(lineType);
    }

    private static void drawRing(VertexConsumer vertices, PoseStack.Pose pose, Vec3 center,
                                 double radius, double tiltZ, double tiltX, double phase,
                                 float red, float green, float blue) {
        final int segments = 72;
        Vec3 previous = ringPoint(center, radius, 0.0D, tiltZ, tiltX, phase);
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            Vec3 current = ringPoint(center, radius, angle, tiltZ, tiltX, phase);
            vertex(vertices, pose, previous, red, green, blue);
            vertex(vertices, pose, current, red, green, blue);
            previous = current;
        }
    }

    private static Vec3 ringPoint(Vec3 center, double radius, double angle, double tiltZ, double tiltX, double phase) {
        double x = Math.cos(angle + phase) * radius;
        double y = Math.sin(angle + phase) * radius;
        double rotatedY = y * Math.cos(tiltX);
        double z = y * Math.sin(tiltX);
        double rotatedX = x * Math.cos(tiltZ) - rotatedY * Math.sin(tiltZ);
        double rotatedVertical = x * Math.sin(tiltZ) + rotatedY * Math.cos(tiltZ);
        return center.add(rotatedX, rotatedVertical, z);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
                               float red, float green, float blue) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, 1.0F).endVertex();
    }

    private static void applySwimmingPose(Player player) {
        player.setSwimming(true);
        if (player.getPose() != Pose.SWIMMING) player.setPose(Pose.SWIMMING);
    }

    public static void clear() {
        Level level = Minecraft.getInstance().level;
        for (Map.Entry<Integer, RenderPoseState> entry : ORIGINAL_POSES.entrySet()) {
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity instanceof Player player) entry.getValue().restore(player);
        }
        EFFECTS.clear();
        FLYING_POSES.clear();
        ORIGINAL_POSES.clear();
    }

    private record RenderPoseState(Pose pose, boolean swimming) {
        static RenderPoseState capture(Player player) {
            return new RenderPoseState(player.getPose(), player.isSwimming());
        }

        void restore(Player player) {
            player.setPose(pose);
            player.setSwimming(swimming);
        }
    }
}
