package com.mrlion303.vfxshadersmod.client.postprocess;

import net.minecraft.resources.ResourceLocation;

/** Contrato de un pase de pantalla, como distorsión de lente o gradación de color. */
public interface ClientVfxPostProcessor {
    ResourceLocation id();

    /** Orden relativo de ejecución: los valores menores se procesan primero. */
    int priority();

    void process(VfxPostProcessContext context);
}
