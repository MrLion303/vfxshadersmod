package com.mrlion303.vfxshadersmod.core.effect;

/**
 * Contrato de ciclo de vida de un efecto.
 *
 * Las implementaciones representan la simulación y el estado del efecto.
 * El renderizado se implementará en contratos específicos del cliente para
 * evitar cargar clases gráficas en el servidor dedicado.
 */
public interface VfxEffect {
    EffectId id();

    default void onStart(EffectContext context) {
    }

    void tick(EffectContext context);

    /**
     * Indica si el efecto terminó de forma natural.
     */
    default boolean isFinished() {
        return false;
    }

    default void onStop(EffectContext context) {
    }
}
