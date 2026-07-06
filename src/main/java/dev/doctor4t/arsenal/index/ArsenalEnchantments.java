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
    RegistryKey<Enchantment> SPEWING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("spewing"));
    RegistryKey<Enchantment> REELING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("reeling"));

    static int getLevel(World world, RegistryKey<Enchantment> key, ItemStack stack) {
        if (world == null || stack.isEmpty()) return 0;
        return world.getRegistryManager()
            .get(RegistryKeys.ENCHANTMENT)
            .flatMap(reg -> reg.getEntry(key))
            .map(entry -> EnchantmentHelper.getLevel(entry, stack))
            .orElse(0);
    }

    static int getEquipmentLevel(World world, RegistryKey<Enchantment> key, LivingEntity entity) {
        if (world == null) return 0;
        return world.getRegistryManager()
            .get(RegistryKeys.ENCHANTMENT)
            .flatMap(reg -> reg.getEntry(key))
            .map(entry -> EnchantmentHelper.getEquipmentLevel(entry, entity))
            .orElse(0);
    }
}
