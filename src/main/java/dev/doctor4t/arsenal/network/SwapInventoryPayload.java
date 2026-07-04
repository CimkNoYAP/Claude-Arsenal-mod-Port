package dev.doctor4t.arsenal.network;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SwapInventoryPayload(int slotId) implements CustomPayload {
    public static final CustomPayload.Id<SwapInventoryPayload> ID = new CustomPayload.Id<>(Arsenal.id("swap_inventory"));
    public static final PacketCodec<RegistryByteBuf, SwapInventoryPayload> CODEC =
        PacketCodecs.VAR_INT.xmap(SwapInventoryPayload::new, SwapInventoryPayload::slotId).cast();

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
