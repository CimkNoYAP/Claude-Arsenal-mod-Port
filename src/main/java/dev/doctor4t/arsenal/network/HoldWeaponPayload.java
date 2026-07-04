package dev.doctor4t.arsenal.network;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record HoldWeaponPayload(boolean hold) implements CustomPayload {
    public static final CustomPayload.Id<HoldWeaponPayload> ID = new CustomPayload.Id<>(Arsenal.id("hold_weapon"));
    public static final PacketCodec<RegistryByteBuf, HoldWeaponPayload> CODEC =
        PacketCodecs.BOOL.xmap(HoldWeaponPayload::new, HoldWeaponPayload::hold).cast();

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
