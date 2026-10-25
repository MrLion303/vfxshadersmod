package com.mrlion303.vfxshadersmod;

import com.mojang.logging.LogUtils;
import com.mrlion303.vfxshadersmod.network.VfxNetwork;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Punto de entrada del mod. */
@Mod(VfxShadersMod.MOD_ID)
public final class VfxShadersMod {
    public static final String MOD_ID = "vfxshadersmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VfxShadersMod() {
        VfxNetwork.register();
        LOGGER.info("VFX Shaders Mod inicializado. Canal de comandos de eventos listo.");
    }
}
