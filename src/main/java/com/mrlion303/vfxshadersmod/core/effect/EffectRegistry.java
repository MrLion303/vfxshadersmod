package com.mrlion303.vfxshadersmod.core.effect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Registro central de definiciones y fábricas de efectos.
 *
 * Se completa durante la inicialización del cliente. Las definiciones también
 * alimentan la lista y el autocompletado de los comandos /vfx.
 */
public final class EffectRegistry {
    private static final Map<EffectId, Supplier<? extends VfxEffect>> FACTORIES =
            new LinkedHashMap<>();
    private static final Map<EffectId, Definition> DEFINITIONS = new LinkedHashMap<>();

    private EffectRegistry() {
    }

    public record Definition(EffectId id, String displayName, String description) {
        public Definition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
        }
    }

    /**
     * Registro compatible para efectos que todavía no tienen metadatos propios.
     */
    public static void register(EffectId id, Supplier<? extends VfxEffect> factory) {
        register(id, id.toString(), "", factory);
    }

    public static void register(
            EffectId id,
            String displayName,
            String description,
            Supplier<? extends VfxEffect> factory
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(factory, "factory");

        if (FACTORIES.containsKey(id)) {
            throw new IllegalArgumentException("Ya existe un efecto con el identificador " + id);
        }

        FACTORIES.put(id, factory);
        DEFINITIONS.put(id, new Definition(id, displayName, description));
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

    public static Optional<Definition> definition(EffectId id) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(id, "id")));
    }

    public static List<Definition> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }

    public static Set<EffectId> ids() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(FACTORIES.keySet()));
    }
}
