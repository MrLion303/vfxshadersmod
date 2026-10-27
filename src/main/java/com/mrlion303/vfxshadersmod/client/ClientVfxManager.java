package com.mrlion303.vfxshadersmod.client;

import com.mojang.logging.LogUtils;
import com.mrlion303.vfxshadersmod.client.postprocess.BlackHoleLensingPass;
import com.mrlion303.vfxshadersmod.client.photon.PhotonEffectBridge;
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

/** Runtime de efectos globales. Las apariciones colocadas se administran por separado. */
public final class ClientVfxManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EffectId, VfxEffect> ACTIVE = new LinkedHashMap<>();
    private static final Map<EffectId, PhotonEffectBridge.Handle> PHOTON_ACTIVE = new LinkedHashMap<>();

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
        if (isActive(id)) {
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
            try {
                PHOTON_ACTIVE.put(id, PhotonEffectBridge.playForEffect(id.location().getPath(), level));
            } catch (RuntimeException photonException) {
                // El efecto original conserva su funcionamiento si un preset Photon no puede inicializarse.
                LOGGER.error("No se pudo iniciar el preset Photon para {}", id, photonException);
            }
            return Result.SUCCESS;
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudo iniciar el efecto VFX {}", id, exception);
            return Result.UNKNOWN_EFFECT;
        }
    }

    public static Result disable(EffectId id) {
        VfxEffect effect = ACTIVE.remove(id);
        stopPhoton(id);
        Level level = Minecraft.getInstance().level;
        if (effect != null && level != null) {
            try {
                effect.onStop(EffectContext.fromLevel(level));
            } catch (RuntimeException exception) {
                LOGGER.error("Error al detener el efecto VFX {}", id, exception);
            }
        }

        int removedInstances = ClientWorldVfxInstances.removeByEffect(id);
        if (effect == null && removedInstances == 0) {
            return Result.NOT_ACTIVE;
        }
        return Result.SUCCESS;
    }

    public static Result toggle(EffectId id) {
        if (!EffectRegistry.ids().contains(id)) {
            return Result.UNKNOWN_EFFECT;
        }
        return isActive(id) ? disable(id) : enable(id);
    }

    public static int disableAll() {
        int count = ACTIVE.size() + ClientWorldVfxInstances.size();
        clear();
        return count;
    }

    /** Limpia los efectos y libera buffers auxiliares al abandonar el mundo. */
    public static void clear() {
        for (EffectId id : new ArrayList<>(PHOTON_ACTIVE.keySet())) {
            stopPhoton(id);
        }
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
        BlackHoleLensingPass.releaseTarget();
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
                PhotonEffectBridge.updateForEffect(entry.getKey().location().getPath(),
                        PHOTON_ACTIVE.get(entry.getKey()), level);
                if (effect.isFinished()) {
                    effect.onStop(context);
                    stopPhoton(entry.getKey());
                    iterator.remove();
                }
            } catch (RuntimeException exception) {
                LOGGER.error("El efecto VFX {} falló y será desactivado", entry.getKey(), exception);
                stopPhoton(entry.getKey());
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

                // Cuando Photon ya está ejecutando el preset como renderer principal,
                // no dibujamos también la versión antigua del efecto.
                String effectPath = entry.getKey().location().getPath();
                PhotonEffectBridge.Handle photonHandle = PHOTON_ACTIVE.get(entry.getKey());
                if (PhotonEffectBridge.usesPhotonAsPrimaryRenderer(effectPath)
                        && photonHandle != null && photonHandle.isRunning()) {
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

        ClientWorldVfxRendererRegistry.render(event);
    }

    private static void stopPhoton(EffectId id) {
        PhotonEffectBridge.Handle handle = PHOTON_ACTIVE.remove(id);
        if (handle != null) {
            try {
                handle.close();
            } catch (RuntimeException exception) {
                LOGGER.warn("No se pudo detener el runtime Photon para {}", id, exception);
            }
        }
    }

    public static boolean isActive(EffectId id) {
        return ACTIVE.containsKey(id) || ClientWorldVfxInstances.hasInstances(id);
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static Set<EffectId> activeIds() {
        Set<EffectId> ids = new LinkedHashSet<>(ACTIVE.keySet());
        for (EffectRegistry.Definition definition : EffectRegistry.definitions()) {
            if (ClientWorldVfxInstances.hasInstances(definition.id())) {
                ids.add(definition.id());
            }
        }
        return Collections.unmodifiableSet(ids);
    }
}
