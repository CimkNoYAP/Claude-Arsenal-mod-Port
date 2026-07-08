package dev.doctor4t.arsenal.index;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public interface ArsenalEnchantments {
    RegistryKey<Enchantment> SPEWING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("spewing"));
    RegistryKey<Enchantment> REELING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Arsenal.id("reeling"));

    static int getLevel(World world, RegistryKey<Enchantment> key, ItemStack stack) {
        if (world == null || stack.isEmpty()) return 0;
        Registry<Enchantment> reg = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        if (reg == null) return 0;
        return reg.getEntry(key).map(e -> EnchantmentHelper.getLevel(e, stack)).orElse(0);
    }

    static int getEquipmentLevel(World world, RegistryKey<Enchantment> key, LivingEntity entity) {
        if (world == null) return 0;
        Registry<Enchantment> reg = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        if (reg == null) return 0;
        return reg.getEntry(key).map(e -> EnchantmentHelper.getEquipmentLevel(e, entity)).orElse(0);
    }
}
