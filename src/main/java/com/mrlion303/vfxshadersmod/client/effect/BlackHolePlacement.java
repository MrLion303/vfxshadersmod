package com.mrlion303.vfxshadersmod.client.effect;

import com.mrlion303.vfxshadersmod.client.ClientWorldVfxInstances;
import com.mrlion303.vfxshadersmod.core.effect.WorldVfxInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Utilidades de colocación para los comandos locales de agujero negro. */
public final class BlackHolePlacement {
    private static final double FALLBACK_DISTANCE = 16.0D;

    private BlackHolePlacement() {
    }

    public static WorldVfxInstance spawnAtAim(float diameter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        Vec3 position;
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            position = hit.getLocation();
            if (hit instanceof BlockHitResult blockHit) {
                Vec3 normal = new Vec3(
                        blockHit.getDirection().getStepX(),
                        blockHit.getDirection().getStepY(),
                        blockHit.getDirection().getStepZ()
                );
                // Deja el horizonte del agujero fuera de la superficie seleccionada.
                position = position.add(normal.scale(diameter * 0.5D + 0.15D));
            }
        } else {
            position = minecraft.player.getEyePosition()
                    .add(minecraft.player.getLookAngle().scale(FALLBACK_DISTANCE));
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
