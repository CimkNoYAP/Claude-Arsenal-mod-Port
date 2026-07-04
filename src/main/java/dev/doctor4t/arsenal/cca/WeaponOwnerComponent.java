package dev.doctor4t.arsenal.cca;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.item.ItemComponent;

import java.util.UUID;

public class WeaponOwnerComponent extends ItemComponent {
    private static final String OWNER = "owner";

    public WeaponOwnerComponent(ItemStack stack) { super(stack); }

    public @Nullable UUID getOwner() { return this.getUuid(OWNER); }
    public void setOwner(UUID uuid) { this.putUuid(OWNER, uuid); }
}
