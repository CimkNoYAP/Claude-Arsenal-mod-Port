package dev.doctor4t.arsenal.cca;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * In CCA 6.x, item components were removed.
 * Weapon owner/skin data is stored directly in the ItemStack's CUSTOM_DATA component.
 */
public class WeaponOwnerComponent {
    private static final String OWNER = "arsenal_owner";
    private final ItemStack stack;

    public WeaponOwnerComponent(ItemStack stack) {
        this.stack = stack;
    }

    public @Nullable UUID getOwner() {
        NbtComponent nbt = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return null;
        NbtCompound compound = nbt.copyNbt();
        if (!compound.containsUuid(OWNER)) return null;
        return compound.getUuid(OWNER);
    }

    public void setOwner(UUID uuid) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> {
            NbtCompound compound = comp.copyNbt();
            compound.putUuid(OWNER, uuid);
            return NbtComponent.of(compound);
        });
    }
}
