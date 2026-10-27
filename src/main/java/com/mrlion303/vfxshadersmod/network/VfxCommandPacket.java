package com.mrlion303.vfxshadersmod.network;

import com.mrlion303.vfxshadersmod.client.ClientVfxNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Orden del servidor para activar o limpiar un efecto en el cliente. */
public record VfxCommandPacket(
        String operation,
        String effect,
        ResourceLocation dimension,
        float diameter,
        double x,
        double y,
        double z,
        String option
) {
    public static final String ENABLE = "enable";
    public static final String DISABLE = "disable";
    public static final String TOGGLE = "toggle";
    public static final String DISABLE_ALL = "disable_all";
    public static final String PLACE_BLACKHOLE = "place_blackhole";
    public static final String CLEAR_BLACKHOLES = "clear_blackholes";
    public static final String ENABLE_CONFIGURED = "enable_configured";
    public static final String SCAN_ONCE = "scan_once";
    public static final String PLAYER_EFFECT = "player_effect";
    public static final String TRAMPOLIN_ONCE = "trampolin_once";

    public VfxCommandPacket(
            String operation,
            String effect,
            ResourceLocation dimension,
            float diameter,
            Vec3 position
    ) {
        this(operation, effect, dimension, diameter, position.x, position.y, position.z, "");
    }

    public VfxCommandPacket(
            String operation, String effect, ResourceLocation dimension,
            float diameter, Vec3 position, String option
    ) {
        this(operation, effect, dimension, diameter, position.x, position.y, position.z, option);
    }

    public VfxCommandPacket(
            String operation, String effect, ResourceLocation dimension,
            float diameter, double x, double y, double z
    ) {
        this(operation, effect, dimension, diameter, x, y, z, "");
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(operation, 32);
        buffer.writeUtf(effect, 128);
        buffer.writeResourceLocation(dimension);
        buffer.writeFloat(diameter);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeUtf(option == null ? "" : option, 64);
    }

    public static VfxCommandPacket decode(FriendlyByteBuf buffer) {
        return new VfxCommandPacket(
                buffer.readUtf(32),
                buffer.readUtf(128),
                buffer.readResourceLocation(),
                buffer.readFloat(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(64)
        );
    }

    public static void handle(VfxCommandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientVfxNetworkHandler.handle(packet)));
        context.setPacketHandled(true);
    }
}
