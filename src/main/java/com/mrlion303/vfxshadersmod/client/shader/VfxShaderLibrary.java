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
        BlackHoleLensingPass.releaseTarget();
    }

    /** Puede ser null antes de terminar la carga de shaders. */
    public static ShaderInstance basicColor() {
        return basicColor;
    }

    /** Shader del pase de deformación de pantalla para agujeros negros. */
    public static ShaderInstance blackHoleLensing() {
        return blackHoleLensing;
    }
}
