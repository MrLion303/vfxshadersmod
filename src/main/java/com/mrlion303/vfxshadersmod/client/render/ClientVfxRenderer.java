package com.mrlion303.vfxshadersmod.client.render;

import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Interfaz opcional para efectos que dibujan en el cliente.
 *
 * Cada implementación decide en qué etapas del mundo debe dibujar. Las clases
 * comunes del motor no deben depender de esta interfaz ni de clases gráficas.
 */
public interface ClientVfxRenderer {
    void render(RenderLevelStageEvent event, float partialTick);
}
