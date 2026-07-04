package dev.doctor4t.arsenal.network;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SweepPayload(int color, int shadowColor, double x, double y, double z) implements CustomPayload {
    public static final CustomPayload.Id<SweepPayload> ID = new CustomPayload.Id<>(Arsenal.id("sweep"));
    public static final PacketCodec<RegistryByteBuf, SweepPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER, SweepPayload::color,
        PacketCodecs.INTEGER, SweepPayload::shadowColor,
        PacketCodecs.DOUBLE, SweepPayload::x,
        PacketCodecs.DOUBLE, SweepPayload::y,
        PacketCodecs.DOUBLE, SweepPayload::z,
        SweepPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
