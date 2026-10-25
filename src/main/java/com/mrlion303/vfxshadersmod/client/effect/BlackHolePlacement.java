package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Crea agujeros negros en coordenadas exactas del mundo actual del cliente. */
public final class BlackHolePlacement {
    private BlackHolePlacement() {
    }

    public static WorldVfxInstance placeAt(float diameter, Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }
        if (!Float.isFinite(diameter)
                || diameter < BlackHoleEffect.MIN_DIAMETER
                || diameter > BlackHoleEffect.MAX_DIAMETER) {
            throw new IllegalArgumentException("El diámetro debe estar entre 1 y 100 bloques.");
        }

        return ClientWorldVfxInstances.spawn(
                BlackHoleEffect.ID,
                minecraft.level,
                position,
                Vec3.ZERO,
                1.0F,
                -1L,
                Map.of("diameter", (double) diameter)
        );
    }
}
