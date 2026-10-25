package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.client.effect.DemoOrbitEffect;
import com.mrlion303.vfxshadersmod.client.effect.MusicHorizonBarsEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowAuroraEffect;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;

/**
 * Catálogo central de los VFX incluidos. Cada efecto registrado aparece
 * automáticamente en /vfx list y en las sugerencias de los comandos.
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
        EffectRegistry.register(
                MusicHorizonBarsEffect.ID,
                "Barras musicales del horizonte",
                "Un ecualizador de neón rodea el horizonte con barras de altura y colores animados.",
                MusicHorizonBarsEffect::new
        );
        EffectRegistry.register(
                RainbowAuroraEffect.ID,
                "Aurora boreal arcoíris",
                "Cortinas de luz ondulantes que recorren el espectro arcoíris alrededor del cielo.",
                RainbowAuroraEffect::new
        );
        registered = true;
    }
}
