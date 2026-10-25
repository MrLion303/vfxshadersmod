package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Set;

/** Renderer de una aparición concreta; una definición puede tener muchas apariciones. */
public interface ClientWorldVfxRenderer {
    Set<RenderLevelStageEvent.Stage> renderStages();

    void render(VfxRenderContext context, WorldVfxInstance instance);
}
