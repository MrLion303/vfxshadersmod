package com.mrlion303.vfxshadersmod.core.runtime;

import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Administra el ciclo de vida de los efectos activos en un único hilo.
 *
 * El responsable de invocar tick debe ser explícito (por ejemplo, el cliente
 * para una escena visual local o el servidor para una simulación de mundo).
 */
public final class EffectManager {
    private final List<VfxEffect> activeEffects = new ArrayList<>();

    public void start(VfxEffect effect, EffectContext context) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(context, "context");

        effect.onStart(context);
        activeEffects.add(effect);
    }

    public void tick(EffectContext context) {
        Objects.requireNonNull(context, "context");

        Iterator<VfxEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            VfxEffect effect = iterator.next();
            effect.tick(context);

            if (effect.isFinished()) {
                effect.onStop(context);
                iterator.remove();
            }
        }
    }

    public void stopAll(EffectContext context) {
        Objects.requireNonNull(context, "context");

        for (VfxEffect effect : activeEffects) {
            effect.onStop(context);
        }
        activeEffects.clear();
    }

    public int activeCount() {
        return activeEffects.size();
    }
}
