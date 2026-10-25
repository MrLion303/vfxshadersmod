package com.mrlion303.vfxshadersmod.core.effect;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Datos del mundo disponibles durante la simulación de un efecto.
 *
 * El contexto no contiene objetos de renderizado: el motor gráfico del cliente
 * tendrá su propio contexto para cámara, matrices y tiempo parcial de fotograma.
 */
public record EffectContext(Level level, long gameTime, RandomSource random) {
    public EffectContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(random, "random");
    }

    public static EffectContext fromLevel(Level level) {
        Objects.requireNonNull(level, "level");
        return new EffectContext(level, level.getGameTime(), level.getRandom());
    }
}
