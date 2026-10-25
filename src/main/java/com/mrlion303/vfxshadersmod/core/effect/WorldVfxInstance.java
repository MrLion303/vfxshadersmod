package com.mrlion303.vfxshadersmod.core.effect;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Una aparición concreta de un efecto en el mundo. Una misma definición puede
 * tener muchas instancias en diferentes coordenadas. Esto describe una aparición
 * visual; no modifica por sí solo bloques, entidades ni jugadores.
 *
 * durationTicks == -1 indica duración indefinida.
 */
public record WorldVfxInstance(
        UUID instanceId,
        EffectId effectId,
        ResourceKey<Level> dimension,
        Vec3 position,
        Vec3 rotationDegrees,
        float scale,
        long createdAtGameTime,
        long durationTicks,
        Map<String, Double> parameters
) {
    public WorldVfxInstance {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(rotationDegrees, "rotationDegrees");
        Objects.requireNonNull(parameters, "parameters");

        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("La escala debe ser finita y mayor que cero");
        }
        if (createdAtGameTime < 0L) {
            throw new IllegalArgumentException("createdAtGameTime no puede ser negativo");
        }
        if (durationTicks < -1L) {
            throw new IllegalArgumentException("durationTicks debe ser -1 o mayor");
        }

        for (Map.Entry<String, Double> entry : parameters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Los parámetros deben tener un nombre");
            }
            if (entry.getValue() == null || !Double.isFinite(entry.getValue())) {
                throw new IllegalArgumentException("El parámetro '" + entry.getKey() + "' debe ser finito");
            }
        }
        parameters = Map.copyOf(parameters);
    }

    public boolean isExpired(long gameTime) {
        return durationTicks >= 0L && gameTime - createdAtGameTime >= durationTicks;
    }

    public boolean belongsTo(Level level) {
        return level != null && dimension.equals(level.dimension());
    }
}
