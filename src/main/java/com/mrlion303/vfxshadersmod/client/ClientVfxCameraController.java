package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxCameraDirection;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/** Cámara virtual local para movimientos cinemáticos, sin añadir entidades al mundo. */
public final class ClientVfxCameraController {
    private static final double THIRD_PERSON_DISTANCE = 4.0D;
    private static final double SHOULDER_BACK = 3.15D;
    private static final double SHOULDER_SIDE = 0.62D;
    private static ArmorStand proxy;
    private static Mode mode = Mode.NONE;
    private static Vec3 fixedPosition = Vec3.ZERO;
    private static float fixedYaw, fixedPitch;
    private static Vec3 transitionStart = Vec3.ZERO;
    private static float transitionStartYaw, transitionStartPitch;
    private static int transitionTicks, transitionElapsed;

    private ClientVfxCameraController() {}

    public static void shoulder(float seconds) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player), pitch = currentPitch(player);
        ensureProxy(player);
        if (proxy == null) return;
        mode = Mode.SHOULDER;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        setViewPosition(current, yaw, pitch);
        beginTransition(current, shoulderPosition(player), yaw, pitch, seconds);
    }

    public static void fixed(Vec3 position, float seconds, String direction) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        var angles = VfxCameraDirection.parse(direction);
        if (angles.isEmpty()) return;
        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player), pitch = currentPitch(player);
        ensureProxy(player);
        if (proxy == null) return;
        mode = Mode.FIXED;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        fixedPosition = position;
        fixedYaw = angles.get().yaw();
        fixedPitch = angles.get().pitch();
        setViewPosition(current, yaw, pitch);
        beginTransition(current, fixedPosition, fixedYaw, fixedPitch, seconds);
    }

    public static void returnToPlayer(float seconds) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { reset(); return; }
        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player), pitch = currentPitch(player);
        ensureProxy(player);
        if (proxy == null) return;
        mode = Mode.RETURNING;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        setViewPosition(current, yaw, pitch);
        beginTransition(current, player.getEyePosition(), player.getYRot(), player.getXRot(), seconds);
    }

    public static void tick(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null || mode == Mode.NONE || proxy == null) return;
        if (proxy.level() != player.level()) { reset(); return; }
        mc.options.setCameraType(mode == Mode.SHOULDER
                ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);

        if (transitionTicks > 0) {
            transitionElapsed++;
            float t = Math.min(1.0F, transitionElapsed / (float) transitionTicks);
            float eased = t * t * (3.0F - 2.0F * t);
            Vec3 destination = destination(player);
            float yaw = Mth.rotLerp(eased, transitionStartYaw, destinationYaw(player));
            float pitch = Mth.lerp(eased, transitionStartPitch, destinationPitch(player));
            setViewPosition(transitionStart.lerp(destination, eased), yaw, pitch);
            if (t >= 1.0F) {
                transitionTicks = transitionElapsed = 0;
                if (mode == Mode.RETURNING) finishReturn(player);
                else setViewPosition(destination(player), destinationYaw(player), destinationPitch(player));
            }
        } else if (mode == Mode.SHOULDER) {
            setViewPosition(shoulderPosition(player), player.getYRot(), player.getXRot());
        } else if (mode == Mode.FIXED) {
            setViewPosition(fixedPosition, fixedYaw, fixedPitch);
        } else {
            setViewPosition(player.getEyePosition(), player.getYRot(), player.getXRot());
            finishReturn(player);
        }
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (proxy != null && !proxy.isRemoved()) proxy.discard();
        proxy = null;
        mode = Mode.NONE;
        transitionTicks = transitionElapsed = 0;
    }

    private static void ensureProxy(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (proxy == null || proxy.isRemoved() || proxy.level() != player.level()) {
            if (proxy != null && !proxy.isRemoved()) proxy.discard();
            proxy = EntityType.ARMOR_STAND.create(player.level());
            if (proxy == null) return;
            proxy.setInvisible(true);
            proxy.setNoGravity(true);
            proxy.setInvulnerable(true);
            proxy.setSilent(true);
        }
        mc.setCameraEntity(proxy);
    }

    private static Vec3 currentPosition(LocalPlayer player) {
        if (proxy == null || Minecraft.getInstance().getCameraEntity() != proxy) return player.getEyePosition();
        Vec3 eye = proxyEye();
        return mode == Mode.SHOULDER
                ? eye.subtract(Vec3.directionFromRotation(proxy.getXRot(), proxy.getYRot())
                        .normalize().scale(THIRD_PERSON_DISTANCE))
                : eye;
    }

    private static float currentYaw(LocalPlayer player) {
        return proxy != null && Minecraft.getInstance().getCameraEntity() == proxy ? proxy.getYRot() : player.getYRot();
    }
    private static float currentPitch(LocalPlayer player) {
        return proxy != null && Minecraft.getInstance().getCameraEntity() == proxy ? proxy.getXRot() : player.getXRot();
    }

    private static Vec3 destination(LocalPlayer player) {
        return switch (mode) {
            case SHOULDER -> shoulderPosition(player);
            case FIXED -> fixedPosition;
            default -> player.getEyePosition();
        };
    }
    private static float destinationYaw(LocalPlayer player) {
        return mode == Mode.FIXED ? fixedYaw : player.getYRot();
    }
    private static float destinationPitch(LocalPlayer player) {
        return mode == Mode.FIXED ? fixedPitch : player.getXRot();
    }
    private static Vec3 shoulderPosition(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        return eye.subtract(forward.scale(SHOULDER_BACK)).add(right.scale(SHOULDER_SIDE))
                .add(0.0D, 0.10D, 0.0D);
    }

    private static void setViewPosition(Vec3 visibleCamera, float yaw, float pitch) {
        if (proxy == null) return;
        Vec3 proxyEye = mode == Mode.SHOULDER
                ? visibleCamera.add(Vec3.directionFromRotation(pitch, yaw).normalize().scale(THIRD_PERSON_DISTANCE))
                : visibleCamera;
        proxy.setPos(proxyEye.x, proxyEye.y - proxy.getEyeHeight(Pose.STANDING), proxyEye.z);
        proxy.setYRot(yaw);
        proxy.setXRot(pitch);
        proxy.yRotO = yaw;
        proxy.xRotO = pitch;
        proxy.xo = proxy.getX();
        proxy.yo = proxy.getY();
        proxy.zo = proxy.getZ();
    }

    private static Vec3 proxyEye() {
        return new Vec3(proxy.getX(), proxy.getY() + proxy.getEyeHeight(Pose.STANDING), proxy.getZ());
    }

    private static void beginTransition(Vec3 start, Vec3 target, float targetYaw, float targetPitch, float seconds) {
        if (seconds < 0.0F || seconds == 0.0F) {
            transitionTicks = transitionElapsed = 0;
            setViewPosition(target, targetYaw, targetPitch);
            if (mode == Mode.RETURNING && Minecraft.getInstance().player != null)
                finishReturn(Minecraft.getInstance().player);
            return;
        }
        transitionStart = start;
        transitionStartYaw = currentYaw(Minecraft.getInstance().player);
        transitionStartPitch = currentPitch(Minecraft.getInstance().player);
        transitionTicks = Math.max(1, (int) Math.ceil(seconds * 20.0F));
        transitionElapsed = 0;
    }

    private static void finishReturn(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        mc.setCameraEntity(player);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (proxy != null && !proxy.isRemoved()) proxy.discard();
        proxy = null;
        mode = Mode.NONE;
        transitionTicks = transitionElapsed = 0;
    }

    private enum Mode { NONE, SHOULDER, FIXED, RETURNING }
}
