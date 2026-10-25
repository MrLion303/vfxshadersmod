package com.mrlion303.vfxshadersmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Punto de entrada del mod. La inicialización del motor gráfico se añadirá
 * por etapas para mantener el arranque común libre de dependencias cliente.
 */
@Mod(VfxShadersMod.MOD_ID)
public final class VfxShadersMod {
    public static final String MOD_ID = "vfxshadersmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VfxShadersMod() {
        LOGGER.info("VFX Shaders Mod inicializado. El motor está en desarrollo.");
    }
}
