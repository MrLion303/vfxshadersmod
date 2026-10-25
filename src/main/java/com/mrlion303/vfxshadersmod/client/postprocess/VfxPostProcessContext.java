package com.mrlion303.vfxshadersmod.client.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Entrada y salida separadas para un pase de posprocesamiento. */
public record VfxPostProcessContext(
        RenderTarget source,
        RenderTarget destination,
        ResourceLocation dimension,
        float partialTick,
        long gameTime
) {
    public VfxPostProcessContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(dimension, "dimension");
        if (source == destination) {
            throw new IllegalArgumentException("El posprocesado requiere targets de entrada y salida distintos");
        }
    }
}
