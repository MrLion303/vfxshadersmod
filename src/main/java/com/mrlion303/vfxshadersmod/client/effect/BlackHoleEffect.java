package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;

import java.util.UUID;

/**
 * Definición activable del agujero negro. Al activarla coloca una instancia
 * de tamaño predeterminado; la instancia y el renderer espacial son independientes.
 */
public final class BlackHoleEffect implements VfxEffect {
    public static final EffectId ID = EffectId.of(VfxShadersMod.MOD_ID, "black_hole");
    public static final float DEFAULT_DIAMETER = 10.0F;

    private UUID instanceId;

    @Override
    public EffectId id() {
        return ID;
    }

    @Override
    public void onStart(EffectContext context) {
        WorldVfxInstance instance = BlackHolePlacement.spawnAtAim(DEFAULT_DIAMETER);
        instanceId = instance == null ? null : instance.instanceId();
    }

    @Override
    public void tick(EffectContext context) {
        // La evolución visual y el campo de distorsión se calculan en el renderer.
    }

    @Override
    public void onStop(EffectContext context) {
        if (instanceId != null) {
            ClientWorldVfxInstances.remove(instanceId);
            instanceId = null;
        }
    }
}
