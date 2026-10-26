package com.mrlion303.vfxshadersmod.client;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** Física local predictiva para que los efectos de movimiento no produzcan tirones. */
public final class ClientPlayerMovementEffects {
    private static boolean moonJumpEnabled;
    private static boolean waterWalkEnabled;

    private ClientPlayerMovementEffects() {
    }

    public static void setEnabled(String effect, boolean enabled) {
        if ("moonjump".equals(effect)) {
            moonJumpEnabled = enabled;
        } else if ("waterwalk".equals(effect) || "caminar_agua".equals(effect)) {
            waterWalkEnabled = enabled;
        }
    }

    public static void tick(Player player) {
        if (player == null || !player.isAlive() || player.isPassenger()) {
            return;
        }

        if (moonJumpEnabled && !player.getAbilities().flying && !player.isFallFlying() && !player.onGround()) {
            Vec3 velocity = player.getDeltaMovement();
            // Compensa la gravedad solo en Y: la velocidad horizontal queda intacta.
            player.setDeltaMovement(velocity.x, velocity.y + 0.052D, velocity.z);
        }

        if (waterWalkEnabled) {
            keepPlayerOnWater(player);
        }
    }

    private static void keepPlayerOnWater(Player player) {
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > 0.06D) {
            return;
        }

        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.02D, player.getZ());
        BlockPos waterPos = feet;
        FluidState fluid = player.level().getFluidState(waterPos);
        if (!fluid.is(FluidTags.WATER)) {
            waterPos = feet.below();
            fluid = player.level().getFluidState(waterPos);
        }
        if (!fluid.is(FluidTags.WATER)) {
            return;
        }

        double surfaceY = waterPos.getY() + fluid.getHeight(player.level(), waterPos);
        if (player.getY() > surfaceY + 0.025D || player.getY() < waterPos.getY() - 0.15D) {
            return;
        }

        if (player.getY() < surfaceY - 0.025D) {
            player.setPos(player.getX(), surfaceY, player.getZ());
        }
        if (motion.y < 0.0D) {
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }
}
