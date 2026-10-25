package com.mrlion303.vfxshadersmod.core.effect;

/** Clasificación funcional: describe dónde actúa un efecto, no su shader. */
public enum VfxEffectCategory {
    ENVIRONMENT("ambiental"),
    WORLD_ANCHORED("anclado al mundo"),
    ENTITY_ATTACHED("ligado a entidad"),
    SCREEN_SPACE("pantalla/cámara"),
    PARTICLE_SYSTEM("partículas"),
    WORLD_SIMULATION("simulación del mundo"),
    AUDIO_REACTIVE("reactivo al audio"),
    COMPOSITE("compuesto");

    private final String displayName;

    VfxEffectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
