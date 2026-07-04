package dev.doctor4t.arsenal.index;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public interface ArsenalEnchantments {
    // In 1.21, enchantments are data-driven (JSON files in data/arsenal/enchantment/).
    // These RegistryKeys reference those JSON definitions.
    RegistryKey<Enchantment> SPEWING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("spewing"));
    RegistryKey<Enchantment> REELING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("reeling"));

    /** Get the level of an enchantment on a specific ItemStack. */
    static int getLevel(World world, RegistryKey<Enchantment> key, ItemStack stack) {
        if (world == null || stack.isEmpty()) return 0;
        return world.getRegistryManager()
            .getOrThrow(RegistryKeys.ENCHANTMENT)
            .getEntry(key)
            .map(entry -> EnchantmentHelper.getLevel(entry, stack))
            .orElse(0);
    }

    /** Get the highest level of an enchantment across all of an entity's equipped items. */
    static int getEquipmentLevel(World world, RegistryKey<Enchantment> key, LivingEntity entity) {
        if (world == null) return 0;
        return world.getRegistryManager()
            .getOrThrow(RegistryKeys.ENCHANTMENT)
            .getEntry(key)
            .map(entry -> EnchantmentHelper.getEquipmentLevel(entry, entity))
            .orElse(0);
    }
}
