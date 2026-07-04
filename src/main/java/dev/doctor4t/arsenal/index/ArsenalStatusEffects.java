package dev.doctor4t.arsenal.index;

import dev.doctor4t.arsenal.Arsenal;
import dev.doctor4t.arsenal.effect.StunStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ArsenalStatusEffects {
    Map<RegistryEntry<StatusEffect>, Identifier> EFFECTS = new LinkedHashMap<>();

    RegistryEntry<StatusEffect> STUN = create("stun", new StunStatusEffect());

    static void initialize() {
        EFFECTS.keySet().forEach(entry ->
            Registry.register(Registries.STATUS_EFFECT, EFFECTS.get(entry), entry.value()));
    }

    private static RegistryEntry<StatusEffect> create(String name, StatusEffect effect) {
        Identifier id = Arsenal.id(name);
        RegistryEntry<StatusEffect> entry = Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
        EFFECTS.put(entry, id);
        return entry;
    }
}
