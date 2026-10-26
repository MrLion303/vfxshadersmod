package com.mrlion303.vfxshadersmod.client;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import com.mrlion303.vfxshadersmod.client.effect.BlackHoleEffect;
import com.mrlion303.vfxshadersmod.client.effect.BlackHolePlacement;
import com.mrlion303.vfxshadersmod.client.effect.StarsspeedEffect;
import com.mrlion303.vfxshadersmod.client.effect.GroundAuroraEffect;
import com.mrlion303.vfxshadersmod.client.effect.TerrainScanEffect;
import com.mrlion303.vfxshadersmod.util.VfxColors;
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
            case VfxCommandPacket.ENABLE_CONFIGURED -> configureAndEnable(packet);
            case VfxCommandPacket.SCAN_ONCE -> {
                int color = VfxColors.parseOrDefault(packet.option(), 0x3478FF);
                TerrainScanEffect.configure(packet.position(), color);
                ClientVfxManager.disable(TerrainScanEffect.ID);
                ClientVfxManager.enable(TerrainScanEffect.ID);
            }
            default -> {
                // Ignora operaciones desconocidas para permitir añadir mensajes en futuras versiones.
            }
        }
    }

    private static void configureAndEnable(VfxCommandPacket packet) {
        int color = VfxColors.parseOrDefault(packet.option(), 0x3478FF);
        EffectId id = effectId(packet.effect());
        switch (packet.effect()) {
            case "starsspeed" -> StarsspeedEffect.configureColor(color);
            case "ground_aurora" -> GroundAuroraEffect.configure(packet.position(), color);
            default -> {
                // Los efectos sin opciones usan su configuración normal.
            }
        }
        ClientVfxManager.disable(id);
        ClientVfxManager.enable(id);
    }

    private static EffectId effectId(String effect) {
        return EffectId.of(VfxShadersMod.MOD_ID, effect);
    }
}
