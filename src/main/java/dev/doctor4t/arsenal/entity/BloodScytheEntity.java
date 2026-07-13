package dev.doctor4t.arsenal.entity;

import dev.doctor4t.arsenal.index.ArsenalDamageTypes;
import dev.doctor4t.arsenal.index.ArsenalEntities;
import dev.doctor4t.arsenal.index.ArsenalItems;
import dev.doctor4t.arsenal.index.ArsenalSounds;
import dev.doctor4t.arsenal.index.ArsenalStatusEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BloodScytheEntity extends PersistentProjectileEntity {
    private List<StatusEffectInstance> storedEffects = new ArrayList<>();

    public BloodScytheEntity(EntityType<? extends BloodScytheEntity> type, World world) {
        super(type, world);
    }

    public BloodScytheEntity(World world, LivingEntity owner) {
        // weapon must be EMPTY or RangedWeaponItem/TridentItem — pass EMPTY, projectile = scythe
        super(ArsenalEntities.BLOOD_SCYTHE, owner, world, ItemStack.EMPTY, new ItemStack(ArsenalItems.SCYTHE));
        this.setNoGravity(true);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ArsenalItems.SCYTHE);
    }

    public void addEffect(StatusEffectInstance effect) {
        storedEffects.add(effect);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            float damage = 5.0F;
            if (living.damage(this.getWorld().getDamageSources().create(ArsenalDamageTypes.SPEWING, this, this.getOwner()), damage)) {
                storedEffects.forEach(effect -> living.addStatusEffect(new StatusEffectInstance(
                    effect.getEffectType(), effect.getDuration(), effect.getAmplifier(),
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon())));
                living.addStatusEffect(new StatusEffectInstance(ArsenalStatusEffects.STUN, 20, 0, false, false, false));
            }
        }
        this.playSound(ArsenalSounds.ENTITY_BLOOD_SCYTHE_HIT, 1.0F, 1.0F);
        this.discard();
    }

    @Override protected SoundEvent getHitSound() { return ArsenalSounds.ENTITY_BLOOD_SCYTHE_HIT; }
    @Override public boolean shouldRender(double cx, double cy, double cz) { return true; }
    @Override protected boolean tryPickup(PlayerEntity player) { return false; }
}
