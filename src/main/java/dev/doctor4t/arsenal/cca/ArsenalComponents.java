package dev.doctor4t.arsenal.cca;

import dev.doctor4t.arsenal.Arsenal;
import net.minecraft.entity.player.PlayerEntity;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.item.ItemComponentInitializer;
import org.ladysnake.cca.api.v3.item.ItemComponentMigrationRegistry;

public class ArsenalComponents implements EntityComponentInitializer, ItemComponentInitializer {
    public static final ComponentKey<BackWeaponComponent> BACK_WEAPON_COMPONENT =
        ComponentRegistry.getOrCreate(Arsenal.id("back_weapon"), BackWeaponComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, BACK_WEAPON_COMPONENT)
            .respawnStrategy(RespawnCopyStrategy.CHARACTER)
            .end(BackWeaponComponent::new);
    }

    // Required by ItemComponentInitializer in CCA 6.x
    // Item components (WeaponOwnerComponent) now use Minecraft's CUSTOM_DATA system directly
    @Override
    public void registerItemComponentMigrations(ItemComponentMigrationRegistry registry) {
        // No migrations needed - we use DataComponentTypes.CUSTOM_DATA directly
    }
}
