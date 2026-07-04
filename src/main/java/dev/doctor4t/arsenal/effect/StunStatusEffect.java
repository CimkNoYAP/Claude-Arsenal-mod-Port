package dev.doctor4t.arsenal.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class StunStatusEffect extends StatusEffect {
    public StunStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFFD220);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        // Stun movement is handled by the mixin
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        // Nothing to clean up
    }
}
