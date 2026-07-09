package dev.doctor4t.arsenal.item;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import dev.doctor4t.arsenal.cca.WeaponOwnerComponent;
import dev.doctor4t.arsenal.entity.BloodScytheEntity;
import dev.doctor4t.arsenal.index.ArsenalCosmetics;
import dev.doctor4t.arsenal.index.ArsenalDamageTypes;
import dev.doctor4t.arsenal.index.ArsenalEnchantments;
import dev.doctor4t.arsenal.index.ArsenalSounds;
import dev.doctor4t.arsenal.util.SweepParticleUtil;
import dev.doctor4t.ratatouille.item.CustomHitParticleItem;
import dev.doctor4t.ratatouille.item.CustomHitSoundItem;
import dev.doctor4t.ratatouille.util.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.AttributeModifierSlot;
import net.minecraft.item.ItemAttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScytheItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {

    public ScytheItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, BlockTags.HOE_MINEABLE,
            settings.attributeModifiers(
                ItemAttributeModifiersComponent.builder()
                    .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(Identifier.of("arsenal", "base_attack_damage"),
                            attackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(Identifier.of("arsenal", "base_attack_speed"),
                            attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .add(ReachEntityAttributes.ATTACK_RANGE,
                        new EntityAttributeModifier(Identifier.of("arsenal", "scythe_attack_range"),
                            0.5, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .build()));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (ArsenalEnchantments.getEquipmentLevel(world, ArsenalEnchantments.SPEWING, player) > 0) {
            if (!world.isClient) {
                BloodScytheEntity bloodScythe = new BloodScytheEntity(world, player);
                bloodScythe.setOwner(player);
                bloodScythe.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 3.0f, 1.0f);
                player.getStackInHand(hand).damage(1, player, EquipmentSlot.MAINHAND);
                bloodScythe.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                ArrayList<StatusEffectInstance> halved = new ArrayList<>();
                float abs = player.getAbsorptionAmount();
                for (StatusEffectInstance e : player.getStatusEffects())
                    halved.add(new StatusEffectInstance(e.getEffectType(), e.getDuration()/2, e.getAmplifier(), e.isAmbient(), e.shouldShowParticles(), e.shouldShowIcon()));
                player.clearStatusEffects();
                halved.forEach(e -> { bloodScythe.addEffect(e); player.addStatusEffect(e); });
                player.setAbsorptionAmount(abs);
                player.damage(world.getDamageSources().create(ArsenalDamageTypes.SPEWING), 3f);
                player.getItemCooldownManager().set(this, 20);
                world.spawnEntity(bloodScythe);
                if (world instanceof ServerWorld sw) {
                    Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
                    if (skin == null) skin = Skin.DEFAULT;
                    SweepParticleUtil.sendSweepPacketToClient(sw, new Pair<>(skin.color, skin.shadowColor),
                        player.getX() - MathHelper.sin((float)(player.getYaw()*(Math.PI/180F))),
                        player.getBodyY(0.5), player.getZ() + MathHelper.cos((float)(player.getYaw()*(Math.PI/180F))));
                }
            }
            world.playSound(null, player.getX(), player.getY(), player.getZ(), ArsenalSounds.ITEM_SCYTHE_SPEWING, net.minecraft.sound.SoundCategory.PLAYERS, 1f, 1f);
            return TypedActionResult.success(player.getStackInHand(hand));
        }
        return super.use(world, player, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(stack));
        if (skin != null && skin != Skin.DEFAULT) {
            tooltip.accept(Text.literal(skin.tooltipName != null ? skin.tooltipName : TextUtils.formatValueString(skin.getName()))
                .styled(s -> s.withColor(skin.color)));
            if (skin.lore != null) {
                if (Screen.hasShiftDown()) {
                    for (String line : Text.translatable(skin.lore).getString().split("\n"))
                        tooltip.accept(Text.literal(line).styled(s -> s.withColor(Formatting.DARK_GRAY)));
                } else {
                    tooltip.accept(Text.translatable("tooltip.arsenal.hidden").styled(s -> s.withColor(Formatting.DARK_GRAY)));
                }
            }
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override public void spawnHitParticles(PlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld sw) {
            Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
            if (skin == null) skin = Skin.DEFAULT;
            SweepParticleUtil.sendSweepPacketToClient(sw, new Pair<>(skin.color, skin.shadowColor),
                player.getX() - MathHelper.sin((float)(player.getYaw()*(Math.PI/180F))),
                player.getBodyY(0.5), player.getZ() + MathHelper.cos((float)(player.getYaw()*(Math.PI/180F))));
        }
    }
    @Override public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_SCYTHE_HIT, 1f, (float)(1f + player.getRandom().nextGaussian()/10f));
    }
    @Override public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) { return !miner.isCreative(); }
    @Override public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof PlayerEntity p) new WeaponOwnerComponent(stack).setOwner(p.getUuid());
    }
    @Override public ActionResult useOnBlock(ItemUsageContext ctx) {
        BlockState bs = ctx.getWorld().getBlockState(ctx.getBlockPos());
        PlayerEntity user = ctx.getPlayer();
        if (user != null && user.isSneaking() && (bs.isIn(BlockTags.ANVIL) || bs.isOf(Blocks.SMITHING_TABLE)) && ctx.getWorld().isClient) {
            if (ArsenalCosmetics.isSupporter(user.getUuid())) {
                Skin cur = Skin.fromString(ArsenalCosmetics.getSkin(ctx.getStack()));
                if (cur == null) cur = Skin.DEFAULT;
                ArsenalCosmetics.setSkin(new WeaponOwnerComponent(user.getStackInHand(ctx.getHand())).getOwner(), ctx.getStack(), Skin.getNext(cur).getName());
                user.playSound(net.minecraft.sound.SoundEvents.BLOCK_SMITHING_TABLE_USE, 0.5f, 1f);
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("tooltip.supporter_only").styled(s -> s.withColor(0xCC0000)));
                user.playSound(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1f);
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(ctx);
    }

    public enum Skin {
        DEFAULT(0xFFD9D9D9, 0xFF7F8885, null, null),
        CLOWN(0xFFD90420, 0xFF8C0420, null, "tooltip.arsenal.scythe_clown"),
        CARRION(0xFFE9DFB8, 0xFF9D806E, null, null),
        GILDED(0xFFF1BC5A, 0xFFE28634, null, null),
        ROZE(0xFFB70066, 0xFF710949, null, null),
        FOLLY(0xFFFF005A, 0xFFBC0045, "Folly Tree Branch", null),
        SCISSORS(0xFFB9B1AF, 0xFF6F686F, null, null);
        public final int color, shadowColor;
        public final @org.jetbrains.annotations.Nullable String lore, tooltipName;
        Skin(int c, int sc, @org.jetbrains.annotations.Nullable String tn, @org.jetbrains.annotations.Nullable String l) { color=c;shadowColor=sc;tooltipName=tn;lore=l; }
        public String getName() { return name().toLowerCase(Locale.ROOT); }
        public static @org.jetbrains.annotations.Nullable Skin fromString(String n) { for(Skin s:values()) if(s.getName().equalsIgnoreCase(n)) return s; return null; }
        public static Skin getNext(Skin s) { Skin[] v=values(); return v[(s.ordinal()+1)%v.length]; }
    }
}
}
