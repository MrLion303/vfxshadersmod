package com.mrlion303.vfxshadersmod.client.postprocess;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registro ordenado de pases. El enlace al framebuffer y el ciclo ping-pong de
 * RenderTarget se añadirán en el pipeline de posprocesamiento.
 */
public final class ClientVfxPostProcessRegistry {
    private static final Map<ResourceLocation, ClientVfxPostProcessor> PROCESSORS = new LinkedHashMap<>();

    private ClientVfxPostProcessRegistry() {
    }

    public static synchronized void register(ClientVfxPostProcessor processor) {
        Objects.requireNonNull(processor, "processor");
        Objects.requireNonNull(processor.id(), "processor.id");
        if (PROCESSORS.putIfAbsent(processor.id(), processor) != null) {
            throw new IllegalArgumentException("Ya existe un pase VFX con el identificador " + processor.id());
        }
    }

    public static synchronized void unregister(ResourceLocation id) {
        PROCESSORS.remove(Objects.requireNonNull(id, "id"));
    }

    public static synchronized List<ClientVfxPostProcessor> processors() {
        List<ClientVfxPostProcessor> sorted = new ArrayList<>(PROCESSORS.values());
        sorted.sort(Comparator.comparingInt(ClientVfxPostProcessor::priority)
                .thenComparing(processor -> processor.id().toString()));
        return List.copyOf(sorted);
    }

    public static synchronized void clear() {
        PROCESSORS.clear();
    }
}
