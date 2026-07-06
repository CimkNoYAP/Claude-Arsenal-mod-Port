package dev.doctor4t.arsenal.datagen;

import dev.doctor4t.arsenal.index.ArsenalEntities;
import dev.doctor4t.arsenal.index.ArsenalItems;
import dev.doctor4t.arsenal.index.ArsenalStatusEffects;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ArsenalLangGen extends FabricLanguageProvider {
    protected ArsenalLangGen(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> lookup) {
        super(dataOutput, lookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup lookup, TranslationBuilder builder) {
        builder.add(ArsenalItems.ANCHORBLADE, "Anchorblade");
        builder.add(ArsenalEntities.ANCHORBLADE, "Anchorblade");
        builder.add(ArsenalItems.SCYTHE, "Scythe");
        builder.add(ArsenalEntities.BLOOD_SCYTHE, "Blood Scythe");
        builder.add(ArsenalItems.WEAPON_RACK, "Weapon Rack");
        builder.add(ArsenalEntities.WEAPON_RACK, "Weapon Rack");
        // STUN is a RegistryEntry - use its translation key directly
        builder.add(ArsenalStatusEffects.STUN.value().getTranslationKey(), "Stun");
        builder.add("tooltip.supporter_only", "Cosmetics are reserved to Ko-Fi and YouTube members only.");
        builder.add("tooltip.arsenal.hidden", "Press [Shift] to show lore");
        builder.add("enchantment.arsenal.spewing", "Spewing");
        builder.add("enchantment.arsenal.reeling", "Reeling");
        builder.add("key.arsenal.select_weapon", "Hold Back Weapon");
        builder.add("key.arsenal.swap_weapon", "Swap Weapons");
        builder.add("category.arsenal", "Arsenal");
    }
}
