package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.shader.VfxShaderLibrary;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/** Tipos de renderizado compartidos por los VFX. */
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

    private static final RenderType COLOR_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":color_quads",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            16384,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::basicColor))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType COLOR_OPAQUE_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":color_opaque_quads",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            16384,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::basicColor))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType SKY_LINES = RenderType.create(
            VfxShadersMod.MOD_ID + ":sky_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.LINES,
            8192,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::basicColor))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5D)))
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType SKY_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":sky_quads",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::basicColor))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType CLOUD_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":cloud_vortex_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::cloudVortex))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType BLACK_HOLE_SKY_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":black_hole_sky_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::blackHoleSurface))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType BLACK_HOLE_WORLD_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":black_hole_world_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::blackHoleSurface))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType BLACK_HOLE_DISK_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":black_hole_disk_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::blackHoleDisk))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType BLACK_HOLE_SKY_DISK_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":black_hole_sky_disk_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::blackHoleDisk))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType PORTAL_QUADS = RenderType.create(
            VfxShadersMod.MOD_ID + ":portal_quads",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(VfxShaderLibrary::portalWindow))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
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

    /** Quads translúcidos que respetan la profundidad sin escribirla. */
    public static RenderType colorQuads() {
        return COLOR_QUADS;
    }

    /** Quads opacos que escriben profundidad, usados para el horizonte del agujero negro. */
    public static RenderType colorOpaqueQuads() {
        return COLOR_OPAQUE_QUADS;
    }

    /** Superficie procedural del agujero negro celeste, sin texturas externas. */
    public static RenderType blackHoleSkyQuads() {
        return BLACK_HOLE_SKY_QUADS;
    }

    /** Superficie procedural del agujero negro colocado en el mundo. */
    public static RenderType blackHoleWorldQuads() {
        return BLACK_HOLE_WORLD_QUADS;
    }

    /** Anillo de acreción 3D para agujeros negros colocados en el mundo. */
    public static RenderType blackHoleDiskQuads() {
        return BLACK_HOLE_DISK_QUADS;
    }

    /** Disco de acreción del agujero celeste: malla 3D sin bloquear la profundidad del mundo. */
    public static RenderType blackHoleSkyDiskQuads() {
        return BLACK_HOLE_SKY_DISK_QUADS;
    }

    /** Plano del portal con recorte en shader. */
    public static RenderType portalQuads() {
        return PORTAL_QUADS;
    }

    /** Geometría luminosa del cielo sin z-fighting contra la capa del firmamento. */
    public static RenderType skyLines() {
        return SKY_LINES;
    }

    /** Cúpulas y estelas celestes que no escriben profundidad. */
    public static RenderType skyQuads() {
        return SKY_QUADS;
    }

    /** Plano texturizado con nubes procedurales giratorias. */
    public static RenderType cloudQuads() {
        return CLOUD_QUADS;
    }
}
