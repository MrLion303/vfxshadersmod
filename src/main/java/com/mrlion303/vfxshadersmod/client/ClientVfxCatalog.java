package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.client.effect.DemoOrbitEffect;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;

/**
 * Punto central de registro de los VFX incluidos. Cuando se cree un efecto
 * nuevo, se añade su fábrica y sus metadatos aquí y aparecerá en /vfx list.
 */
public final class ClientVfxCatalog {
    private static boolean registered;

    private ClientVfxCatalog() {
    }

    public static synchronized void registerDefaults() {
        if (registered) {
            return;
        }

        EffectRegistry.register(
                DemoOrbitEffect.ID,
                "Órbitas de demostración",
                "Tres anillos de color que giran alrededor del jugador local. Efecto de prueba del motor.",
                DemoOrbitEffect::new
        );
        registered = true;
    }
}
