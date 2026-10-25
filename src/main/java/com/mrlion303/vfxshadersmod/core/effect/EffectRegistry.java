package com.mrlion303.vfxshadersmod.core.effect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Registro inicial de fábricas de efectos. Está pensado para registrarse
 * durante el arranque; no es un registro dinámico seguro entre hilos.
 */
public final class EffectRegistry {
    private static final Map<EffectId, Supplier<? extends VfxEffect>> FACTORIES =
            new LinkedHashMap<>();

    private EffectRegistry() {
    }

    public static void register(EffectId id, Supplier<? extends VfxEffect> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");

        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("Ya existe un efecto con el identificador " + id);
        }
    }

    public static Optional<VfxEffect> create(EffectId id) {
        Supplier<? extends VfxEffect> factory = FACTORIES.get(Objects.requireNonNull(id, "id"));
        if (factory == null) {
            return Optional.empty();
        }

        VfxEffect effect = Objects.requireNonNull(factory.get(), "La fábrica devolvió null");
        if (!id.equals(effect.id())) {
            throw new IllegalStateException(
                    "La fábrica de " + id + " creó un efecto con identificador " + effect.id());
        }

        return Optional.of(effect);
    }

    public static Set<EffectId> ids() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(FACTORIES.keySet()));
    }
}
