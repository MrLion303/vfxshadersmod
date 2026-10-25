package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHolePlacement;
import com.mrlion303.vfxshadersmod.core.effect.EffectId;
import com.mrlion303.vfxshadersmod.network.VfxCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Aplica en el hilo del cliente las órdenes enviadas por comandos de servidor. */
public final class ClientVfxNetworkHandler {
    private ClientVfxNetworkHandler() {
    }

    public static void handle(VfxCommandPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.level.dimension().location().equals(packet.dimension())) {
            return;
        }

        ClientVfxCatalog.registerDefaults();
        switch (packet.operation()) {
            case VfxCommandPacket.ENABLE -> ClientVfxManager.enable(effectId(packet.effect()));
            case VfxCommandPacket.DISABLE -> ClientVfxManager.disable(effectId(packet.effect()));
            case VfxCommandPacket.TOGGLE -> ClientVfxManager.toggle(effectId(packet.effect()));
            case VfxCommandPacket.DISABLE_ALL -> ClientVfxManager.disableAll();
            case VfxCommandPacket.PLACE_BLACKHOLE ->
                    BlackHolePlacement.placeAt(packet.diameter(), new Vec3(packet.x(), packet.y(), packet.z()));
            case VfxCommandPacket.CLEAR_BLACKHOLES -> ClientVfxManager.disable(BlackHoleEffect.ID);
            default -> {
                // Ignora operaciones desconocidas para permitir añadir mensajes en futuras versiones.
            }
        }
    }

    private static EffectId effectId(String effect) {
        return EffectId.of(VfxShadersMod.MOD_ID, effect);
    }
}
