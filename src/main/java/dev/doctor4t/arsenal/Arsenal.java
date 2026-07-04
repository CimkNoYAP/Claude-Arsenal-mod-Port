package dev.doctor4t.arsenal;

import dev.doctor4t.arsenal.cca.BackWeaponComponent;
import dev.doctor4t.arsenal.index.*;
import dev.doctor4t.arsenal.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class Arsenal implements ModInitializer {
    public static final String MOD_ID = "arsenal";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ArsenalEntities.initialize();
        ArsenalItems.initialize();
        ArsenalSounds.initialize();
        ArsenalParticles.initialize();
        ArsenalStatusEffects.initialize();

        // Register packet types (C2S = client to server)
        PayloadTypeRegistry.playC2S().register(HoldWeaponPayload.ID, HoldWeaponPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapWeaponPayload.ID, SwapWeaponPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapInventoryPayload.ID, SwapInventoryPayload.CODEC);
        // Register packet types (S2C = server to client)
        PayloadTypeRegistry.playS2C().register(SweepPayload.ID, SweepPayload.CODEC);

        // Server-side receivers
        ServerPlayNetworking.registerGlobalReceiver(HoldWeaponPayload.ID, (payload, context) -> {
            BackWeaponComponent.setHoldingBackWeaponServer(context.player(), payload.hold());
        });

        ServerPlayNetworking.registerGlobalReceiver(SwapWeaponPayload.ID, (payload, context) -> {
            var player = context.player();
            if (!player.isSpectator()) {
                boolean toggled = BackWeaponComponent.isHoldingBackWeapon(player);
                BackWeaponComponent.setHoldingBackWeaponServer(player, false);
                ItemStack itemStack = BackWeaponComponent.getBackWeapon(player);
                boolean success = BackWeaponComponent.setBackWeapon(player, player.getStackInHand(Hand.MAIN_HAND));
                if (success) {
                    player.setStackInHand(Hand.MAIN_HAND, itemStack);
                }
                player.clearActiveItem();
                BackWeaponComponent.setHoldingBackWeaponServer(player, toggled);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SwapInventoryPayload.ID, (payload, context) -> {
            var player = context.player();
            int slotId = payload.slotId();
            if (!player.isSpectator()) {
                if (!player.currentScreenHandler.isValid(slotId)) {
                    return;
                }
                Slot slot = player.currentScreenHandler.getSlot(slotId);
                ItemStack itemStack = BackWeaponComponent.getBackWeapon(player);
                boolean success = BackWeaponComponent.setBackWeapon(player, slot.getStack());
                if (success) {
                    slot.setStack(itemStack);
                }
            }
        });
    }
}
