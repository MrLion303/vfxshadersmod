package com.mrlion303.vfxshadersmod.client.shader;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/**
 * Carga shaders GLSL del mod y conserva la instancia que Minecraft entrega
 * cuando termina de cargar o recargar los recursos gráficos.
 */
@Mod.EventBusSubscriber(
        modid = VfxShadersMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class VfxShaderLibrary {
    private static ShaderInstance basicColor;

    private VfxShaderLibrary() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceLocation shaderId = new ResourceLocation(VfxShadersMod.MOD_ID, "vfx/basic_color");
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), shaderId, DefaultVertexFormat.POSITION_COLOR),
                shader -> basicColor = shader
        );
    }

    /**
     * Puede ser null durante el arranque, antes de que Minecraft invoque el
     * callback de carga. Los RenderType solo deben solicitarlo al dibujar.
     */
    public static ShaderInstance basicColor() {
        return basicColor;
    }
}
