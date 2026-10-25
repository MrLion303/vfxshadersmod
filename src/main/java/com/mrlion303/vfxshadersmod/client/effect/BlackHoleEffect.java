package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;

/**
 * Definición de agujero negro situado en coordenadas del mundo.
 * Las apariciones concretas se crean con /vfx enable blackhole <tamaño> <x> <y> <z>.
 */
public final class BlackHoleEffect implements VfxEffect {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "blackhole");
    public static final float DEFAULT_DIAMETER = 10.0F;
    public static final float MIN_DIAMETER = 1.0F;
    public static final float MAX_DIAMETER = 100.0F;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void tick(EffectContext context) {
        // La geometría y la distorsión se calculan con las coordenadas de cada instancia.
    }
}
