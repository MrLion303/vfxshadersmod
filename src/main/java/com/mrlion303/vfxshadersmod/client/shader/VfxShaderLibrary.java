package com.mrlion303.vfxshadersmod.client.shader;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.postprocess.BlackHoleLensingPass;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Carga shaders GLSL del mod y conserva las instancias al recargar recursos. */
@Mod.EventBusSubscriber(
        modid = VfxShadersMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class VfxShaderLibrary {
    private static ShaderInstance basicColor;
    private static ShaderInstance blackHoleLensing;
    private static ShaderInstance portalWindow;
    private static ShaderInstance cloudVortex;
    private static ShaderInstance blackHoleSurface;
    private static ShaderInstance blackHoleDisk;
    private static ShaderInstance trampolineWave;

    private VfxShaderLibrary() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceLocation colorId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/basic_color");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), colorId, DefaultVertexFormat.POSITION_COLOR),
                shader -> basicColor = shader
        );

        ResourceLocation lensingId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/black_hole_lensing");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), lensingId, DefaultVertexFormat.POSITION_TEX),
                shader -> blackHoleLensing = shader
        );
        ResourceLocation portalId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/portal_window");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), portalId, DefaultVertexFormat.POSITION_TEX),
                shader -> portalWindow = shader
        );

        ResourceLocation vortexId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/cloud_vortex");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), vortexId, DefaultVertexFormat.POSITION_TEX),
                shader -> cloudVortex = shader
        );

        ResourceLocation blackHoleSurfaceId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/black_hole_surface");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), blackHoleSurfaceId, DefaultVertexFormat.POSITION_TEX),
                shader -> blackHoleSurface = shader
        );

        ResourceLocation blackHoleDiskId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/black_hole_disk");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), blackHoleDiskId, DefaultVertexFormat.POSITION_TEX),
                shader -> blackHoleDisk = shader
        );

        ResourceLocation trampolineWaveId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/trampoline_wave");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), trampolineWaveId, DefaultVertexFormat.POSITION_TEX),
                shader -> trampolineWave = shader
        );

        BlackHoleLensingPass.releaseTarget();
        com.mrlion303.vfxshadersmod.client.postprocess.TrampolineWaveDistortionPass.releaseTarget();
    }

    /** Puede ser null antes de terminar la carga de shaders. */
    public static ShaderInstance basicColor() {
        return basicColor;
    }

    /** Shader del pase de deformación de pantalla para agujeros negros. */
    public static ShaderInstance blackHoleLensing() {
        return blackHoleLensing;
    }

    /** Shader que recorta el interior del portal como un disco y dibuja la galaxia. */
    public static ShaderInstance portalWindow() {
        return portalWindow;
    }

    /** Shader volumétrico procedural para el vórtice de nubes. */
    public static ShaderInstance cloudVortex() {
        return cloudVortex;
    }

    /** Shader procedural del agujero negro celeste. */
    public static ShaderInstance blackHoleSurface() {
        return blackHoleSurface;
    }

    /** Shader del gas de acreción aplicado a una malla anular 3D. */
    public static ShaderInstance blackHoleDisk() {
        return blackHoleDisk;
    }

    /** Shader de pantalla que deforma las superficies alcanzadas por la onda. */
    public static ShaderInstance trampolineWave() {
        return trampolineWave;
    }
}
