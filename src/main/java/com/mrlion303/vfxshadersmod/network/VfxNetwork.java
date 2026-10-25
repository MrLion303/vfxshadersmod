package com.mrlion303.vfxshadersmod.network;

import com.mrlion303.vfxshadersmod.VfxShadersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Canal para convertir los comandos de eventos en efectos visibles por los clientes. */
public final class VfxNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VfxShadersMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered;

    private VfxNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
                0,
                VfxCommandPacket.class,
                VfxCommandPacket::encode,
                VfxCommandPacket::decode,
                VfxCommandPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        registered = true;
    }

    public static void sendToDimension(ServerLevel level, VfxCommandPacket packet) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }
}
