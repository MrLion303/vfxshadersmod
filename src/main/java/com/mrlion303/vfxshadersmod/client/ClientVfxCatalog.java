package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleSkyEffect;
import com.mrlion303.vfxshadersmod.client.effect.CloudVortexEffect;
import com.mrlion303.vfxshadersmod.client.effect.StarsspeedEffect;
import com.mrlion303.vfxshadersmod.client.effect.GroundAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.FallingStarsEffect;
import com.mrlion303.vfxshadersmod.client.effect.TerrainScanEffect;
import com.mrlion303.vfxshadersmod.client.effect.TrampolineWaveEffect;
import com.mrlion303.vfxshadersmod.client.effect.SkyPortalEffect;
import com.mrlion303.vfxshadersmod.client.effect.DemoOrbitEffect;
import com.mrlion303.vfxshadersmod.client.effect.MusicHorizonBarsEffect;
import com.mrlion303.vfxshadersmod.client.effect.MusicSyncBarsEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.RainbowAuroraPhotonEffect;
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
                MusicSyncBarsEffect.ID,
                "Barras Sync",
                "Copia de las barras musicales que reacciona a los sonidos reproducidos localmente en este cliente.",
                VfxEffectCategory.ENVIRONMENT,
                MusicSyncBarsEffect::new
        );
        EffectRegistry.register(
                RainbowAuroraEffect.ID,
                "Aurora boreal arcoíris",
                "Cortinas de luz ondulantes que recorren el espectro arcoíris alrededor del cielo, con cierre continuo.",
                VfxEffectCategory.ENVIRONMENT,
                RainbowAuroraEffect::new
        );
        EffectRegistry.register(
                RainbowAuroraPhotonEffect.ID,
                "Aurora arcoíris Photon",
                "Variante de partículas Photon con bandas orbitales, estelas largas y bloom.",
                VfxEffectCategory.ENVIRONMENT,
                RainbowAuroraPhotonEffect::new
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
                "Horizonte negro colocado en coordenadas exactas, con disco de acreción y lente gravitacional de pantalla.",
                VfxEffectCategory.WORLD_ANCHORED,
                BlackHoleEffect::new
        );
        EffectRegistry.register(
                BlackHoleSkyEffect.ID,
                "Agujero negro celeste",
                "Un agujero negro de apariencia astronómica en el cielo, con disco de acreción y lente gravitacional.",
                VfxEffectCategory.ENVIRONMENT,
                BlackHoleSkyEffect::new
        );

        EffectRegistry.register(
                CloudVortexEffect.ID,
                "Vórtice de nubes celeste",
                "Nubes naranja rojizas giran alrededor de un centro brillante en el cielo.",
                VfxEffectCategory.ENVIRONMENT,
                CloudVortexEffect::new
        );
        EffectRegistry.register(
                StarsspeedEffect.ID,
                "Starsspeed",
                "Cielo tintado uniformemente, con estelas de estrellas veloces y resplandor en el horizonte.",
                VfxEffectCategory.ENVIRONMENT,
                StarsspeedEffect::new
        );
        EffectRegistry.register(
                GroundAuroraEffect.ID,
                "Aurora del suelo",
                "Ondas luminosas de color personalizable se expanden por el terreno desde una coordenada.",
                VfxEffectCategory.WORLD_ANCHORED,
                GroundAuroraEffect::new
        );
        EffectRegistry.register(
                FallingStarsEffect.ID,
                "Lluvia de estrellas",
                "Estrellas diagonales de colores arcoíris caen hacia el suelo y se desvanecen al llegar.",
                VfxEffectCategory.PARTICLE_SYSTEM,
                FallingStarsEffect::new
        );
        EffectRegistry.register(
                TerrainScanEffect.ID,
                "Escaneo del terreno",
                "Un aro de escaneo de un solo uso se expande sobre la superficie de los bloques.",
                VfxEffectCategory.PARTICLE_SYSTEM,
                TerrainScanEffect::new
        );
        EffectRegistry.register(
                TrampolineWaveEffect.ID,
                "Onda de trampolín",
                "Una onda circular deforma visualmente el suelo y su cresta eleva a los jugadores al alcanzarlos.",
                VfxEffectCategory.WORLD_ANCHORED,
                TrampolineWaveEffect::new
        );

        EffectRegistry.register(
                SkyPortalEffect.ID,
                "Portal celeste",
                "Una grieta cinematográfica se abre como un portal circular con interior galáctico.",
                VfxEffectCategory.ENVIRONMENT,
                SkyPortalEffect::new
        );

        ClientWorldVfxRendererRegistry.register(BlackHoleEffect.ID, new BlackHoleVfxRenderer());
        registered = true;
    }
}
