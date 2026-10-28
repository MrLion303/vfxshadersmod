package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxCameraDirection;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;


/**
 * Cámaras cinematográficas locales. La entidad de cámara sigue siendo el jugador:
 * así Minecraft conserva el control, las colisiones y los paquetes de movimiento normales.
 */
public final class ClientVfxCameraController {
    private static final double THIRD_PERSON_DISTANCE = 4.0D;
    private static final double SHOULDER_BACK = 3.15D;
    private static final double SHOULDER_SIDE = 0.62D;
    private static final double SHOULDER_UP = 0.10D;
    private static final double CAMERA_CLEARANCE = 0.18D;

    private static Mode mode = Mode.NONE;
    private static Vec3 fixedPosition = Vec3.ZERO;
    private static float fixedYaw;
    private static float fixedPitch;
    private static Vec3 transitionStartPosition = Vec3.ZERO;
    private static float transitionStartYaw;
    private static float transitionStartPitch;
    private static int transitionTicks;
    private static int transitionElapsed;
    private static Vec3 lastCameraPosition;
    private static float lastCameraYaw;
    private static float lastCameraPitch;
    private static boolean hasCameraSample;

    private ClientVfxCameraController() {}

    public static void shoulder(float seconds) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (mode == Mode.SHOULDER) {
            returnToPlayer(seconds);
            return;
        }

        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player);
        float pitch = currentPitch(player);
        mode = Mode.SHOULDER;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        beginTransition(current, yaw, pitch, seconds);
    }

    public static void fixed(Vec3 position, float seconds, String direction) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        var angles = VfxCameraDirection.parse(direction);
        if (angles.isEmpty()) return;

        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player);
        float pitch = currentPitch(player);
        fixedPosition = position;
        fixedYaw = angles.get().yaw();
        fixedPitch = angles.get().pitch();
        mode = Mode.FIXED;
        // La vista desacoplada necesita tercera persona para renderizar el avatar
        // del jugador desde la posición remota de la cámara.
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        beginTransition(current, yaw, pitch, seconds);
    }

    public static void returnToPlayer(float seconds) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }

        Vec3 current = currentPosition(player);
        float yaw = currentYaw(player);
        float pitch = currentPitch(player);
        mode = Mode.RETURNING;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        beginTransition(current, yaw, pitch, seconds);
    }

    /** Actualiza únicamente el tipo de vista y el avance de las transiciones. */
    public static void tick(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null) {
            if (mode != Mode.NONE) reset();
            return;
        }
        if (mode == Mode.NONE) return;

        // La cámara fija también usa tercera persona: el siguiente ajuste coloca
        // Camera exactamente en fixedPosition y mantiene visible al jugador.
        mc.options.setCameraType(mode == Mode.SHOULDER || mode == Mode.FIXED
                ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        if (transitionTicks > 0) {
            transitionElapsed = Math.min(transitionTicks, transitionElapsed + 1);
        }
    }

    /**
     * Aplica la posición cinematográfica al objeto Camera después del cálculo vanilla.
     * Nunca cambia Minecraft.getCameraEntity(), por lo que el jugador continúa moviéndose
     * y girando como siempre.
     */
    public static void applyCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 vanillaPosition = camera.getPosition();
        Vec3 appliedPosition = vanillaPosition;
        float appliedYaw = event.getYaw();
        float appliedPitch = event.getPitch();
        boolean finishReturn = false;

        switch (mode) {
            case NONE -> {
                rememberCamera(appliedPosition, appliedYaw, appliedPitch);
                return;
            }
            case SHOULDER -> {
                float t = transitionProgress(event.getPartialTick());
                appliedYaw = transitionTicks > 0
                        ? Mth.rotLerp(t, transitionStartYaw, player.getYRot())
                        : player.getYRot();
                appliedPitch = transitionTicks > 0
                        ? Mth.lerp(t, transitionStartPitch, player.getXRot())
                        : player.getXRot();

                Vec3 target = shoulderPosition(player, vanillaPosition, appliedYaw, appliedPitch);
                appliedPosition = transitionTicks > 0
                        ? transitionStartPosition.lerp(target, t)
                        : target;
                if (transitionTicks > 0 && t >= 1.0F) clearTransition();
            }
            case FIXED -> {
                float t = transitionProgress(event.getPartialTick());
                appliedPosition = transitionTicks > 0
                        ? transitionStartPosition.lerp(fixedPosition, t)
                        : fixedPosition;
                appliedYaw = Mth.rotLerp(t, transitionStartYaw, fixedYaw);
                appliedPitch = Mth.lerp(t, transitionStartPitch, fixedPitch);
                if (transitionTicks > 0 && t >= 1.0F) clearTransition();
            }
            case RETURNING -> {
                float t = transitionProgress(event.getPartialTick());
                appliedPosition = transitionTicks > 0
                        ? transitionStartPosition.lerp(player.getEyePosition(), t)
                        : player.getEyePosition();
                appliedYaw = Mth.rotLerp(t, transitionStartYaw, player.getYRot());
                appliedPitch = Mth.lerp(t, transitionStartPitch, player.getXRot());
                finishReturn = transitionTicks == 0 || t >= 1.0F;
            }
        }

        event.setYaw(appliedYaw);
        event.setPitch(appliedPitch);
        camera.setPosition(appliedPosition);
        rememberCamera(appliedPosition, appliedYaw, appliedPitch);
        if (finishReturn) finishReturn(player);
    }

    /** Oculta la mano cuando la vista de primera persona está lejos del cuerpo del jugador. */
    public static boolean shouldHideHand() {
        Minecraft mc = Minecraft.getInstance();
        if (mode == Mode.FIXED) return true;
        if (mode != Mode.RETURNING || mc.player == null) return false;
        Vec3 position = hasCameraSample ? lastCameraPosition : mc.player.getEyePosition();
        return position.distanceTo(mc.player.getEyePosition()) > 0.65D;
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Fallback de seguridad para limpiar también sesiones iniciadas por versiones anteriores.
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        mode = Mode.NONE;
        clearTransition();
        lastCameraPosition = null;
        hasCameraSample = false;
    }

    private static Vec3 currentPosition(LocalPlayer player) {
        return hasCameraSample && lastCameraPosition != null
                ? lastCameraPosition : player.getEyePosition();
    }

    private static float currentYaw(LocalPlayer player) {
        return hasCameraSample && mode != Mode.NONE ? lastCameraYaw : player.getYRot();
    }

    private static float currentPitch(LocalPlayer player) {
        return hasCameraSample && mode != Mode.NONE ? lastCameraPitch : player.getXRot();
    }

    private static Vec3 shoulderPosition(LocalPlayer player, Vec3 vanillaPosition, float yaw, float pitch) {
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 horizontalForward = Vec3.directionFromRotation(0.0F, yaw).normalize();
        // En Minecraft yaw=0 mira al sur (+Z); el vector de la derecha apunta hacia -X.
        Vec3 right = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x);
        Vec3 target = vanillaPosition
                .add(forward.scale(THIRD_PERSON_DISTANCE - SHOULDER_BACK))
                .add(right.scale(SHOULDER_SIDE))
                .add(0.0D, SHOULDER_UP, 0.0D);
        return clipCameraToBlocks(player, target);
    }

    private static Vec3 clipCameraToBlocks(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        Vec3 offset = target.subtract(eye);
        if (offset.lengthSqr() < 1.0E-6D) return target;
        var hit = player.level().clip(new ClipContext(
                eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return target;
        Vec3 direction = offset.normalize();
        return hit.getLocation().subtract(direction.scale(CAMERA_CLEARANCE));
    }

    private static void beginTransition(Vec3 start, float yaw, float pitch, float seconds) {
        transitionStartPosition = start;
        transitionStartYaw = yaw;
        transitionStartPitch = pitch;
        transitionElapsed = 0;
        if (!Float.isFinite(seconds) || seconds <= 0.0F) {
            transitionTicks = 0;
        } else {
            transitionTicks = Math.max(1, (int) Math.ceil(seconds * 20.0F));
        }
    }

    private static float transitionProgress(double partialTick) {
        if (transitionTicks <= 0) return 1.0F;
        return Mth.clamp((transitionElapsed + (float) partialTick) / (float) transitionTicks, 0.0F, 1.0F);
    }

    private static void clearTransition() {
        transitionTicks = 0;
        transitionElapsed = 0;
    }

    private static void rememberCamera(Vec3 position, float yaw, float pitch) {
        lastCameraPosition = position;
        lastCameraYaw = yaw;
        lastCameraPitch = pitch;
        hasCameraSample = true;
    }

    private static void finishReturn(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        mode = Mode.NONE;
        clearTransition();
        mc.setCameraEntity(player);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        rememberCamera(player.getEyePosition(), player.getYRot(), player.getXRot());
    }

    private enum Mode { NONE, SHOULDER, FIXED, RETURNING }
}
