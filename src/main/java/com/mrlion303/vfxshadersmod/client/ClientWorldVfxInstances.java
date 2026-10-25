package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Almacén local de apariciones situadas en coordenadas del mundo. */
public final class ClientWorldVfxInstances {
    private static final Map<UUID, WorldVfxInstance> INSTANCES = new LinkedHashMap<>();

    private ClientWorldVfxInstances() {
    }

    public static WorldVfxInstance spawn(
            EffectId effectId,
            Level level,
            Vec3 position,
            Vec3 rotationDegrees,
            float scale,
            long durationTicks,
            Map<String, Double> parameters
    ) {
        if (level == null || level != Minecraft.getInstance().level) {
            throw new IllegalArgumentException("Las instancias locales requieren el mundo actual del cliente");
        }

        WorldVfxInstance instance = new WorldVfxInstance(
                UUID.randomUUID(), effectId, level.dimension(), position, rotationDegrees,
                scale, level.getGameTime(), durationTicks, parameters
        );
        INSTANCES.put(instance.instanceId(), instance);
        return instance;
    }

    public static boolean add(WorldVfxInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance no puede ser null");
        }
        if (INSTANCES.containsKey(instance.instanceId())) {
            return false;
        }
        INSTANCES.put(instance.instanceId(), instance);
        return true;
    }

    public static Optional<WorldVfxInstance> get(UUID instanceId) {
        return Optional.ofNullable(INSTANCES.get(instanceId));
    }

    public static boolean remove(UUID instanceId) {
        return INSTANCES.remove(instanceId) != null;
    }

    public static int removeByEffect(EffectId effectId) {
        int before = INSTANCES.size();
        INSTANCES.values().removeIf(instance -> instance.effectId().equals(effectId));
        return before - INSTANCES.size();
    }

    public static boolean hasInstances(EffectId effectId) {
        Level level = Minecraft.getInstance().level;
        return level != null && INSTANCES.values().stream()
                .anyMatch(instance -> instance.effectId().equals(effectId)
                        && instance.belongsTo(level)
                        && !instance.isExpired(level.getGameTime()));
    }

    public static int countByEffect(EffectId effectId, Level level) {
        if (level == null) {
            return 0;
        }
        return (int) INSTANCES.values().stream()
                .filter(instance -> instance.effectId().equals(effectId))
                .filter(instance -> instance.belongsTo(level))
                .filter(instance -> !instance.isExpired(level.getGameTime()))
                .count();
    }

    public static List<WorldVfxInstance> forLevel(Level level) {
        if (level == null) {
            return List.of();
        }

        List<WorldVfxInstance> result = new ArrayList<>();
        for (WorldVfxInstance instance : INSTANCES.values()) {
            if (instance.belongsTo(level) && !instance.isExpired(level.getGameTime())) {
                result.add(instance);
            }
        }
        return List.copyOf(result);
    }

    public static void tick(Level level) {
        if (level == null || level != Minecraft.getInstance().level) {
            return;
        }
        INSTANCES.values().removeIf(instance ->
                instance.belongsTo(level) && instance.isExpired(level.getGameTime()));
    }

    public static void clear() {
        INSTANCES.clear();
    }

    public static int size() {
        return INSTANCES.size();
    }
}
