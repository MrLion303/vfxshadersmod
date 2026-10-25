package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.DemoOrbitEffect;
import com.mrlion303.vfxshadersmod.client.effect.MusicHorizonBarsEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowSkyEffect;
import com.mrlion303.vfxshadersmod.client.render.BlackHoleVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.ClientWorldVfxRendererRegistry;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffectCategory;

/** Catálogo central de los VFX incluidos. */
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
                VfxEffectCategory.ENTITY_ATTACHED,
                DemoOrbitEffect::new
        );
        EffectRegistry.register(
                MusicHorizonBarsEffect.ID,
                "Barras musicales del horizonte",
                "Un ecualizador de neón rodea el horizonte con barras de altura y colores animados.",
                VfxEffectCategory.ENVIRONMENT,
                MusicHorizonBarsEffect::new
        );
        EffectRegistry.register(
                RainbowAuroraEffect.ID,
                "Aurora boreal arcoíris",
                "Cortinas de luz ondulantes que recorren el espectro arcoíris alrededor del cielo, con cierre continuo.",
                VfxEffectCategory.ENVIRONMENT,
                RainbowAuroraEffect::new
        );
        EffectRegistry.register(
                RainbowSkyEffect.ID,
                "Cielo arcoíris cambiante",
                "Tiñe el cielo con una cúpula translúcida cuyos colores recorren lentamente el arcoíris.",
                VfxEffectCategory.ENVIRONMENT,
                RainbowSkyEffect::new
        );
        EffectRegistry.register(
                BlackHoleEffect.ID,
                "Agujero negro",
                "Horizonte negro de tamaño ajustable, disco de acreción y lente gravitacional sobre la escena.",
                VfxEffectCategory.WORLD_ANCHORED,
                BlackHoleEffect::new
        );

        ClientWorldVfxRendererRegistry.register(BlackHoleEffect.ID, new BlackHoleVfxRenderer());
        registered = true;
    }
}
