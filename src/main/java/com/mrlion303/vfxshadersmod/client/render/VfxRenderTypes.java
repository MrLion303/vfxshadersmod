package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * Tipos de renderizado compartidos por los VFX. Se mantienen aquí para que
 * cada efecto no tenga que construir y configurar sus propios RenderType.
 */
public final class VfxRenderTypes extends RenderType {
    private static final RenderType COLOR_LINES = RenderType.create(
            VfxShadersMod.MOD_ID + ":color_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::basicColor))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private VfxRenderTypes() {
        super(
                VfxShadersMod.MOD_ID + ":internal",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.LINES,
                256,
                false,
                false,
                () -> { },
                () -> { }
        );
    }

    public static RenderType colorLines() {
        return COLOR_LINES;
    }
}
