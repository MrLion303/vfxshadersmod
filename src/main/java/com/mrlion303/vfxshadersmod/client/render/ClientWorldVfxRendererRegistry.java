package com.mrlion303.vfxshadersmod.client.render;

import com.mojang.logging.LogUtils;
import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Vincula tipos de efecto colocables con sus renderers del cliente. */
public final class ClientWorldVfxRendererRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EffectId, ClientWorldVfxRenderer> RENDERERS = new LinkedHashMap<>();

    private ClientWorldVfxRendererRegistry() {
    }

    public static synchronized void register(EffectId id, ClientWorldVfxRenderer renderer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(renderer, "renderer");
        if (RENDERERS.putIfAbsent(id, renderer) != null) {
            throw new IllegalArgumentException("Ya hay un renderer colocado registrado para " + id);
        }
    }

    public static synchronized void unregister(EffectId id) {
        RENDERERS.remove(id);
    }

    public static void render(RenderLevelStageEvent event) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        VfxRenderContext context = VfxRenderContext.from(event);
        for (WorldVfxInstance instance : ClientWorldVfxInstances.forLevel(context.level())) {
            ClientWorldVfxRenderer renderer;
            synchronized (ClientWorldVfxRendererRegistry.class) {
                renderer = RENDERERS.get(instance.effectId());
            }
            if (renderer == null || !renderer.renderStages().contains(context.stage())) {
                continue;
            }

            try {
                renderer.render(context, instance);
            } catch (RuntimeException exception) {
                LOGGER.error("Falló el renderer de la instancia VFX {} ({})",
                        instance.effectId(), instance.instanceId(), exception);
                ClientWorldVfxInstances.remove(instance.instanceId());
            }
        }
    }
}
