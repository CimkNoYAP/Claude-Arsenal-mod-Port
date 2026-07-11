package dev.doctor4t.arsenal.mixin;

import dev.doctor4t.arsenal.entity.AnchorbladeEntity;
import dev.doctor4t.arsenal.index.ArsenalStatusEffects;
import dev.doctor4t.arsenal.item.AnchorbladeItem;
import dev.doctor4t.arsenal.item.ScytheItem;
import dev.doctor4t.arsenal.util.AnchorOwner;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("WrongEntityDataParameterClass")
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements AnchorOwner {

    @Unique private static final TrackedData<Integer> BASIC_ANCHOR_MAIN   = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> REELING_ANCHOR_MAIN = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> BASIC_ANCHOR_OFF    = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> REELING_ANCHOR_OFF  = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void arsenal$initTrackers(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(BASIC_ANCHOR_MAIN,   -1);
        builder.add(REELING_ANCHOR_MAIN, -1);
        builder.add(BASIC_ANCHOR_OFF,    -1);
        builder.add(REELING_ANCHOR_OFF,  -1);
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void arsenal$anchorbladeUnderwaterSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (this.getMainHandStack().getItem() instanceof AnchorbladeItem && this.isSubmergedIn(FluidTags.WATER)) {
            cir.setReturnValue(cir.getReturnValue() * 2f);
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addCritParticles(Lnet/minecraft/entity/Entity;)V"))
    private void arsenal$scytheOnCrit(Entity target, CallbackInfo ci) {
        if (this.getStackInHand(Hand.MAIN_HAND).getItem() instanceof ScytheItem && target instanceof LivingEntity living) {
            float strength = (float)(0.25f * (1.0 - living.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
            living.addStatusEffect(new StatusEffectInstance(ArsenalStatusEffects.STUN, 10, 0, false, false, false));
            target.setVelocity(this.getPos().subtract(target.getPos()).multiply(strength));
            target.velocityModified = true;
        }
    }

    @Inject(method = "takeShieldHit", at = @At("HEAD"))
    private void arsenal$scytheBreaksShield(LivingEntity attacker, CallbackInfo ci) {
        if (attacker.getMainHandStack().getItem() instanceof ScytheItem) {
            PlayerEntity self = (PlayerEntity)(Object)this;
            for (Hand hand : Hand.values()) {
                if (self.getStackInHand(hand).getItem() instanceof ShieldItem) {
                    self.getItemCooldownManager().set(self.getStackInHand(hand).getItem(), 100);
                    self.clearActiveItem();
                    break;
                }
            }
        }
    }

    @Override
    public void arsenal$setAnchor(Hand hand, AnchorbladeEntity anchor) {
        if (hand == Hand.MAIN_HAND) this.dataTracker.set(anchor.hasReeling() ? REELING_ANCHOR_MAIN : BASIC_ANCHOR_MAIN, anchor.getId());
        else                        this.dataTracker.set(anchor.hasReeling() ? REELING_ANCHOR_OFF  : BASIC_ANCHOR_OFF,  anchor.getId());
    }

    @Override
    public AnchorbladeEntity arsenal$getAnchor(Hand hand, boolean reeling) {
        int id = hand == Hand.MAIN_HAND
            ? this.dataTracker.get(reeling ? REELING_ANCHOR_MAIN : BASIC_ANCHOR_MAIN)
            : this.dataTracker.get(reeling ? REELING_ANCHOR_OFF  : BASIC_ANCHOR_OFF);
        return this.getWorld().getEntityById(id) instanceof AnchorbladeEntity a ? a : null;
    }

    @Override
    public boolean arsenal$isAnchorActive(Hand hand, boolean reeling) {
        AnchorbladeEntity a = this.arsenal$getAnchor(hand, reeling);
        return a != null && a.isAlive();
    }
}
