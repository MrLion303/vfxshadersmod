package com.mrlion303.vfxshadersmod.client;

import com.mojang.logging.LogUtils;
import com.mrlion303.vfxshadersmod.client.render.ClientVfxRenderer;
import com.mrlion303.vfxshadersmod.client.render.ClientWorldVfxRendererRegistry;
import com.mrlion303.vfxshadersmod.core.effect.EffectContext;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.EffectRegistry;
import com.mrlion303.vfxshadersmod.core.effect.VfxEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runtime de efectos visuales activados localmente. Las definiciones activadas
 * con /vfx son independientes de las apariciones colocadas en el mundo.
 */
public final class ClientVfxManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EffectId, VfxEffect> ACTIVE = new LinkedHashMap<>();

    private ClientVfxManager() {
    }

    public enum Result {
        SUCCESS,
        ALREADY_ACTIVE,
        NOT_ACTIVE,
        UNKNOWN_EFFECT,
        NO_WORLD
    }

    public static Result enable(EffectId id) {
        if (!EffectRegistry.ids().contains(id)) {
            return Result.UNKNOWN_EFFECT;
        }
        if (ACTIVE.containsKey(id)) {
            return Result.ALREADY_ACTIVE;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return Result.NO_WORLD;
        }

        VfxEffect effect = EffectRegistry.create(id).orElse(null);
        if (effect == null) {
            return Result.UNKNOWN_EFFECT;
        }

        try {
            effect.onStart(EffectContext.fromLevel(level));
            ACTIVE.put(id, effect);
            return Result.SUCCESS;
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudo iniciar el efecto VFX {}", id, exception);
            return Result.UNKNOWN_EFFECT;
        }
    }

    public static Result disable(EffectId id) {
        VfxEffect effect = ACTIVE.remove(id);
        if (effect == null) {
            return Result.NOT_ACTIVE;
        }

        Level level = Minecraft.getInstance().level;
        if (level != null) {
            try {
                effect.onStop(EffectContext.fromLevel(level));
            } catch (RuntimeException exception) {
                LOGGER.error("Error al detener el efecto VFX {}", id, exception);
            }
        }
        return Result.SUCCESS;
    }

    public static Result toggle(EffectId id) {
        if (!EffectRegistry.ids().contains(id)) {
            return Result.UNKNOWN_EFFECT;
        }
        return ACTIVE.containsKey(id) ? disable(id) : enable(id);
    }

    public static int disableAll() {
        int count = ACTIVE.size();
        clear();
        return count;
    }

    /** Limpia el runtime de efectos y las apariciones locales al abandonar el mundo. */
    public static void clear() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            EffectContext context = EffectContext.fromLevel(level);
            for (Map.Entry<EffectId, VfxEffect> entry : new ArrayList<>(ACTIVE.entrySet())) {
                try {
                    entry.getValue().onStop(context);
                } catch (RuntimeException exception) {
                    LOGGER.error("Error al limpiar el efecto VFX {}", entry.getKey(), exception);
                }
            }
        }
        ACTIVE.clear();
        ClientWorldVfxInstances.clear();
    }

    public static void tick(Level level) {
        if (level == null || level != Minecraft.getInstance().level) {
            return;
        }

        EffectContext context = EffectContext.fromLevel(level);
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EffectId, VfxEffect> entry = iterator.next();
            try {
                VfxEffect effect = entry.getValue();
                effect.tick(context);
                if (effect.isFinished()) {
                    effect.onStop(context);
                    iterator.remove();
                }
            } catch (RuntimeException exception) {
                LOGGER.error("El efecto VFX {} falló y será desactivado", entry.getKey(), exception);
                iterator.remove();
            }
        }
        ClientWorldVfxInstances.tick(level);
    }

    public static void render(RenderLevelStageEvent event) {
        if (!ACTIVE.isEmpty()) {
            for (Map.Entry<EffectId, VfxEffect> entry : new ArrayList<>(ACTIVE.entrySet())) {
                if (!(entry.getValue() instanceof ClientVfxRenderer renderer)) {
                    continue;
                }

                try {
                    renderer.render(event, event.getPartialTick());
                } catch (RuntimeException exception) {
                    LOGGER.error("El renderizado del efecto VFX {} falló; se desactivará",
                            entry.getKey(), exception);
                    disable(entry.getKey());
                }
            }
        }

        // Una aparición colocada tiene su propio ciclo de vida y se dibuja
        // aunque no haya efectos globales activados con /vfx.
        ClientWorldVfxRendererRegistry.render(event);
    }

    public static boolean isActive(EffectId id) {
        return ACTIVE.containsKey(id);
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static Set<EffectId> activeIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(ACTIVE.keySet()));
    }
}
