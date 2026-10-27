package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;

/** Variante Photon de la aurora arcoíris; la aurora clásica conserva su renderizado original. */
public final class RainbowAuroraPhotonEffect implements VfxEffect {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "rainbow_aurora_photon");

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void tick(EffectContext context) {
        // Las bandas y estelas las anima el runtime de Photon.
    }
}
