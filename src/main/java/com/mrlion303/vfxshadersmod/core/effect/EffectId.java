package com.mrlion303.vfxshadersmod.core.effect;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Identificador estable para un efecto del motor.
 */
public record EffectId(ResourceLocation location) {
    public EffectId {
        Objects.requireNonNull(location, "location");
    }

    public static EffectId of(String namespace, String path) {
        return new EffectId(new ResourceLocation(namespace, path));
    }

    @Override
    public String toString() {
        return location.toString();
    }
}
