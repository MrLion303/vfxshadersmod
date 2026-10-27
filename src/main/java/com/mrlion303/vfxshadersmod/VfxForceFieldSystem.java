package com.mrlion303.vfxshadersmod;

import com.mrlion303.vfxshadersmod.core.physics.VfxFieldMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Núcleo de fuerzas compartido para efectos VFX anclados al mundo.
 * La simulación se ejecuta en el servidor para que todos los jugadores vean
 * la misma respuesta física; los renderers solo representan el campo.
 */
@Mod.EventBusSubscriber(modid = VfxShadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VfxForceFieldSystem {
    private static final Map<ServerLevel, List<BlackHoleField>> BLACK_HOLES = new HashMap<>();
    private static final double MAX_ACCELERATION = 0.075D;

    private VfxForceFieldSystem() {
    }

    public static synchronized void addBlackHole(ServerLevel level, Vec3 center, double diameter) {
        List<BlackHoleField> fields = BLACK_HOLES.computeIfAbsent(level, ignored -> new ArrayList<>());
        fields.add(new BlackHoleField(center, Math.max(1.0D, diameter)));
        // Evita que colocar repetidamente agujeros acumule campos infinitos.
        if (fields.size() > 8) {
            fields.remove(0);
        }
    }

    public static synchronized void clearBlackHoles(ServerLevel level) {
        BLACK_HOLES.remove(level);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        List<BlackHoleField> fields;
        synchronized (VfxForceFieldSystem.class) {
            fields = BLACK_HOLES.get(level);
            if (fields == null || fields.isEmpty()) {
                return;
            }
            fields = List.copyOf(fields);
        }

        for (Entity entity : level.getAllEntities()) {
            if (!entity.isAlive() || entity.isSpectator() || entity.isNoGravity() || entity.isPassenger()) {
                continue;
            }
            Vec3 position = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 velocity = entity.getDeltaMovement();
            Vec3 added = Vec3.ZERO;

            for (BlackHoleField field : fields) {
                Vec3 toward = field.center.subtract(position);
                double distance = toward.length();
                if (distance < 1.25D || distance > field.influenceRadius()) {
                    continue;
                }

                double falloff = VfxFieldMath.radialFalloff(distance, field.influenceRadius());
                // Campo gravitacional suave: evita singularidades y no secuestra el movimiento
                // de entidades que están fuera del radio de influencia.
                double acceleration = Math.min(MAX_ACCELERATION,
                        (0.004D + field.diameter * 0.00045D) * falloff);
                Vec3 radial = VfxFieldMath.safeDirection(toward);
                Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
                Vec3 tangent = radial.cross(up);
                if (tangent.lengthSqr() > 1.0E-6D) {
                    tangent = tangent.normalize().scale(acceleration * 0.18D);
                } else {
                    tangent = Vec3.ZERO;
                }
                added = added.add(radial.scale(acceleration)).add(tangent);
            }

            if (added.lengthSqr() > 1.0E-8D) {
                Vec3 next = velocity.add(added);
                // Limita la velocidad añadida por los campos para conservar control y estabilidad.
                double horizontal = Math.sqrt(next.x * next.x + next.z * next.z);
                if (horizontal > 2.8D) {
                    next = new Vec3(next.x * 2.8D / horizontal, next.y, next.z * 2.8D / horizontal);
                }
                next = new Vec3(next.x, Math.max(-1.8D, Math.min(1.8D, next.y)), next.z);
                entity.setDeltaMovement(next);
                entity.hurtMarked = true;
                if (entity instanceof ServerPlayer player) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                }
            }
        }
    }

    private record BlackHoleField(Vec3 center, double diameter) {
        private double influenceRadius() {
            return Math.max(8.0D, diameter * 3.5D);
        }
    }
}
