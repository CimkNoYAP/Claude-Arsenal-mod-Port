package dev.doctor4t.arsenal.network;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SwapWeaponPayload() implements CustomPayload {
    public static final CustomPayload.Id<SwapWeaponPayload> ID = new CustomPayload.Id<>(Arsenal.id("swap_weapon"));
    public static final PacketCodec<RegistryByteBuf, SwapWeaponPayload> CODEC =
        PacketCodec.unit(new SwapWeaponPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
