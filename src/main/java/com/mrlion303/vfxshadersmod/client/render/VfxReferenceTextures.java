package com.mrlion303.vfxshadersmod.client.render;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;

/** Carga mapas de referencia generados por Gradle y los conecta con shaders VFX. */
public final class VfxReferenceTextures {
    private VfxReferenceTextures() {
    }

    public static void bind(ShaderInstance shader, String sampler, String texturePath) {
        if (shader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation location = new ResourceLocation(VfxShadersMod.MOD_ID, texturePath);
        var manager = minecraft.getTextureManager();
        if (!(manager.getTexture(location) instanceof SimpleTexture)) {
            manager.register(location, new SimpleTexture(location));
        }
        var texture = manager.getTexture(location);
        shader.setSampler(sampler, (java.util.function.IntSupplier) texture::getId);
    }
}
