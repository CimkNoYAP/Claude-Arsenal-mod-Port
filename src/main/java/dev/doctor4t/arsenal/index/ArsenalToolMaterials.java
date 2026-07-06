package dev.doctor4t.arsenal.index;

import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Lazy;

import java.util.function.Supplier;

public enum ArsenalToolMaterials implements ToolMaterial {
    SCYTHE(2031, 9.0F, 8.0F, 28, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    ANCHORBLADE(2031, 9.0F, 8.0F, 28, () -> Ingredient.ofItems(Items.IRON_INGOT));

    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Lazy<Ingredient> repairIngredient;

    ArsenalToolMaterials(int durability, float miningSpeed, float attackDamage, int enchantability, Supplier<Ingredient> repairIngredient) {
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredient = new Lazy<>(repairIngredient);
    }

    @Override public int getDurability() { return durability; }
    @Override public float getMiningSpeedMultiplier() { return miningSpeed; }
    @Override public float getAttackDamage() { return attackDamage; }
    @Override public int getEnchantability() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return repairIngredient.get(); }
}
