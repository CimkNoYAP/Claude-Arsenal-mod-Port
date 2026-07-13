package dev.doctor4t.arsenal.entity;

import dev.doctor4t.arsenal.index.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AnchorbladeEntity extends PersistentProjectileEntity {
    private static final TrackedData<Byte> ANCHOR_FLAGS = DataTracker.registerData(AnchorbladeEntity.class, TrackedDataHandlerRegistry.BYTE);

    public int returnTimer;

    public AnchorbladeEntity(EntityType<? extends AnchorbladeEntity> type, World world) {
        super(type, world);
    }

    public AnchorbladeEntity(World world, LivingEntity owner, ItemStack stack) {
        // weapon must be EMPTY or RangedWeaponItem/TridentItem — pass EMPTY, projectile = stack
        super(ArsenalEntities.ANCHORBLADE, owner, world, ItemStack.EMPTY, stack.copy());
        this.setNoGravity(true);
        this.setReeling(ArsenalEnchantments.getLevel(world, ArsenalEnchantments.REELING, stack) > 0);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ArsenalItems.ANCHORBLADE);
    }

    public ItemStack getStack() {
        ItemStack s = this.asItemStack();
        return s.isEmpty() ? new ItemStack(ArsenalItems.ANCHORBLADE) : s;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ANCHOR_FLAGS, (byte) 0);
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();
        double d = 2;
        if (!this.getWorld().isClient) {
            if (owner == null || !owner.isAlive()) { this.discard(); return; }
            if (this.hasDealtDamage() || this.isNoClip()) {
                this.setNoClip(true);
                Vec3d vec3d = owner.getEyePos().subtract(this.getPos());
                double length = vec3d.length();
                this.setVelocity(vec3d.normalize().multiply(Math.min(length, d * 3)));
            }
            if (this.getPos().distanceTo(owner.getPos()) > 30) this.setDealtDamage(true);
        }
        if (this.inGround && !this.hasDealtDamage()) {
            if (this.hasReeling()) {
                if (this.returnTimer++ > 100) this.setDealtDamage(true);
                if (owner == null) { this.setDealtDamage(true); return; }
                Vec3d vec3d = this.getPos().subtract(owner.getEyePos());
                owner.setVelocity(owner.getVelocity().multiply(0.95).add(vec3d.normalize().multiply((float)(d / 5f))));
                owner.fallDistance = 0;
            } else {
                float radius = 5f;
                this.getWorld().addParticle(ArsenalParticles.SHOCKWAVE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                for (LivingEntity hit : this.getWorld().getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(radius), LivingEntity::isAlive)) {
                    float strength = (float)(1f * (1.0 - hit.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
                    if (strength > 0) {
                        Vec3d dir = hit.getPos().add(0, hit.getHeight()/2f, 0).subtract(this.getPos());
                        float prox = (float)MathHelper.lerp(MathHelper.clamp(dir.length()/radius, 0, 1), 1, 0);
                        Vec3d vel = dir.normalize().multiply(prox * strength);
                        hit.addVelocity(vel.x, vel.y, vel.z);
                        hit.fallDistance = 0;
                    }
                }
                this.setDealtDamage(true);
            }
        }
        super.tick();
    }

    @Override public void setPitch(float p) { if (!this.hasDealtDamage()) super.setPitch(p); }
    @Override public void setYaw(float y) { if (!this.hasDealtDamage()) super.setYaw(y); }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        Entity hitEntity = result.getEntity();
        float damage = 10F;
        Entity owner = this.getOwner();
        this.setDealtDamage(true);
        hitEntity.timeUntilRegen = 0;
        if (hitEntity.damage(this.getWorld().getDamageSources().create(ArsenalDamageTypes.ANCHOR, this, owner), damage)) {
            if (hitEntity.getType() == EntityType.ENDERMAN) return;
            if (hitEntity instanceof LivingEntity le) {
                float strength = (float)(1f * (1.0 - le.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
                if (strength > 0) {
                    Vec3d dir = this.hasReeling()
                        ? owner.getPos().subtract(le.getPos()).multiply(strength/10f)
                        : le.getPos().subtract(owner.getPos()).normalize().multiply(strength);
                    le.addVelocity(dir.x, dir.y, dir.z);
                }
                this.onHit(le);
            }
            if (owner instanceof PlayerEntity p && !p.isCreative())
                p.getItemCooldownManager().set(ArsenalItems.ANCHORBLADE, 40);
        }
        this.setVelocity(this.getVelocity().multiply(-0.01, -0.1, -0.01));
        this.playSound(this.getHitSound(), 1.0f, 1.0f);
    }

    @Override protected boolean tryPickup(PlayerEntity p) { return this.isOwner(p); }
    @Override protected float getDragInWater() { return 0.99F; }
    @Override protected SoundEvent getHitSound() { return ArsenalSounds.ENTITY_ANCHORBLADE_LAND; }
    @Override public boolean shouldRender(double cx, double cy, double cz) { return true; }

    public boolean hasDealtDamage() { return getAnchorFlag(0); }
    public void setDealtDamage(boolean v) { setAnchorFlag(0, v); }
    public boolean hasReeling() { return getAnchorFlag(1); }
    public void setReeling(boolean v) { setAnchorFlag(1, v); }
    public boolean isRecalled() { return getAnchorFlag(2); }
    public void setRecalled(boolean v) { if (v) setDealtDamage(true); setAnchorFlag(2, v); }

    private boolean getAnchorFlag(int f) { return (this.dataTracker.get(ANCHOR_FLAGS) >> f & 1) == 1; }
    private void setAnchorFlag(int f, boolean v) {
        byte cur = this.dataTracker.get(ANCHOR_FLAGS);
        this.dataTracker.set(ANCHOR_FLAGS, v ? (byte)(cur|(1<<f)) : (byte)(cur&~(1<<f)));
    }
}
