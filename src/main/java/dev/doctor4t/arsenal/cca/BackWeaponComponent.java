package dev.doctor4t.arsenal.cca;

import dev.doctor4t.arsenal.network.HoldWeaponPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class BackWeaponComponent implements AutoSyncedComponent {
    private final PlayerEntity player;
    private final SimpleInventory backWeapon = new SimpleInventory(1);
    private boolean holdingBackWeapon = false;

    public BackWeaponComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        ItemStack stack = ItemStack.fromNbtOrEmpty(registryLookup, tag.getCompound("backWeapon"));
        this.backWeapon.setStack(0, stack);
        this.holdingBackWeapon = tag.getBoolean("holdingBackWeapon");
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        ItemStack stack = this.backWeapon.getStack(0);
        if (!stack.isEmpty()) {
            tag.put("backWeapon", stack.toNbt(registryLookup));
        }
        tag.putBoolean("holdingBackWeapon", this.holdingBackWeapon);
    }

    public ItemStack getBackWeapon() { return this.backWeapon.getStack(0); }
    public static ItemStack getBackWeapon(PlayerEntity player) {
        return ArsenalComponents.BACK_WEAPON_COMPONENT.get(player).getBackWeapon();
    }

    public boolean setBackWeapon(ItemStack backWeapon) {
        this.backWeapon.setStack(0, backWeapon);
        ArsenalComponents.BACK_WEAPON_COMPONENT.sync(this.player);
        return true;
    }
    public static boolean setBackWeapon(PlayerEntity player, ItemStack backWeapon) {
        return ArsenalComponents.BACK_WEAPON_COMPONENT.get(player).setBackWeapon(backWeapon);
    }

    public SimpleInventory getBackWeaponInventory() { return this.backWeapon; }
    public static SimpleInventory getBackWeaponInventory(PlayerEntity player) {
        return ArsenalComponents.BACK_WEAPON_COMPONENT.get(player).getBackWeaponInventory();
    }

    public boolean isHoldingBackWeapon() { return this.holdingBackWeapon; }
    public static boolean isHoldingBackWeapon(PlayerEntity player) {
        return ArsenalComponents.BACK_WEAPON_COMPONENT.get(player).isHoldingBackWeapon();
    }

    /** Called server-side only (from network handler) */
    public static void setHoldingBackWeaponServer(PlayerEntity player, boolean holdingBackWeapon) {
        ArsenalComponents.BACK_WEAPON_COMPONENT.get(player).holdingBackWeapon = holdingBackWeapon;
        ArsenalComponents.BACK_WEAPON_COMPONENT.sync(player);
    }

    /** Called client-side — sends packet to server */
    public static void setHoldingBackWeapon(PlayerEntity player, boolean holdingBackWeapon) {
        if (player.getWorld().isClient()) {
            ClientPlayNetworking.send(new HoldWeaponPayload(holdingBackWeapon));
        } else {
            setHoldingBackWeaponServer(player, holdingBackWeapon);
        }
    }
}
