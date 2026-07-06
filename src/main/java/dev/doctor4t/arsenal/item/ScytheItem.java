package dev.doctor4t.arsenal.item;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import dev.doctor4t.arsenal.cca.ArsenalComponents;
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
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ScytheItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {
    private static final EntityAttributeModifier REACH_MODIFIER = new EntityAttributeModifier(
        UUID.fromString("911af262-067d-4da2-854c-20f03cc2dd8b"),
        "scythe_attack_range", 0.5, EntityAttributeModifier.Operation.ADD_VALUE);

    public ScytheItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, BlockTags.HOE_MINEABLE,
            settings.attributeModifiers(
                ItemAttributeModifiersComponent.builder()
                    .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                            attackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                            attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(ReachEntityAttributes.ATTACK_RANGE,
                        new EntityAttributeModifier(UUID.fromString("911af262-067d-4da2-854c-20f03cc2dd8b"),
                            "scythe_attack_range", 0.5, EntityAttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockState blockStateClicked = context.getWorld().getBlockState(context.getBlockPos());
        PlayerEntity user = context.getPlayer();
        if (user != null && user.isSneaking()
            && (blockStateClicked.isIn(BlockTags.ANVIL) || blockStateClicked.isOf(Blocks.SMITHING_TABLE))
            && context.getWorld().isClient) {
            WeaponOwnerComponent ownerComp = new WeaponOwnerComponent(user.getStackInHand(context.getHand()));
            if (ArsenalCosmetics.isSupporter(user.getUuid())) {
                Skin currentSkin = Skin.fromString(ArsenalCosmetics.getSkin(context.getStack()));
                if (currentSkin == null) currentSkin = Skin.DEFAULT;
                ArsenalCosmetics.setSkin(ownerComp.getOwner(), context.getStack(), Skin.getNext(currentSkin).getName());
                context.getPlayer().playSound(SoundEvents.BLOCK_SMITHING_TABLE_USE, 0.5f, 1.0f);
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("tooltip.supporter_only").styled(style -> style.withColor(0xCC0000)));
                context.getPlayer().playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1.0f);
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(context);
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
                ArrayList<StatusEffectInstance> statusEffectsHalved = new ArrayList<>();
                float absorption = player.getAbsorptionAmount();
                for (StatusEffectInstance statusEffect : player.getStatusEffects()) {
                    statusEffectsHalved.add(new StatusEffectInstance(statusEffect.getEffectType(),
                        statusEffect.getDuration() / 2, statusEffect.getAmplifier(),
                        statusEffect.isAmbient(), statusEffect.shouldShowParticles(), statusEffect.shouldShowIcon()));
                }
                player.clearStatusEffects();
                statusEffectsHalved.forEach(sei -> { bloodScythe.addEffect(sei); player.addStatusEffect(sei); });
                player.setAbsorptionAmount(absorption);
                player.damage(world.getDamageSources().create(ArsenalDamageTypes.SPEWING), 3f);
                player.getItemCooldownManager().set(this, 20);
                world.spawnEntity(bloodScythe);
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
                    if (skin == null) skin = Skin.DEFAULT;
                    SweepParticleUtil.sendSweepPacketToClient(serverWorld, new Pair<>(skin.color, skin.shadowColor),
                        player.getX() - MathHelper.sin((float)(player.getYaw() * (Math.PI / 180F))),
                        player.getBodyY(0.5D),
                        player.getZ() + MathHelper.cos((float)(player.getYaw() * (Math.PI / 180F))));
                }
            }
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ArsenalSounds.ITEM_SCYTHE_SPEWING, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return TypedActionResult.success(player.getStackInHand(hand));
        }
        return super.use(world, player, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, @org.jetbrains.annotations.Nullable World world, List<Text> tooltip, TooltipContext context) {
        Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(stack));
        if (skin != null && skin != Skin.DEFAULT) {
            tooltip.add(Text.literal(skin.tooltipName != null ? skin.tooltipName : TextUtils.formatValueString(skin.getName()))
                .styled(style -> style.withColor(skin.color)));
            if (skin.lore != null) {
                if (Screen.hasShiftDown()) {
                    for (String line : Text.translatable(skin.lore).getString().split("\n"))
                        tooltip.add(Text.literal(line).styled(s -> s.withColor(Formatting.DARK_GRAY)));
                } else {
                    tooltip.add(Text.translatable("tooltip.arsenal.hidden").styled(s -> s.withColor(Formatting.DARK_GRAY)));
                }
            }
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override public void spawnHitParticles(PlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld sw) {
            Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
            if (skin == null) skin = Skin.DEFAULT;
            SweepParticleUtil.sendSweepPacketToClient(sw, new Pair<>(skin.color, skin.shadowColor),
                player.getX() - MathHelper.sin((float)(player.getYaw() * (Math.PI / 180F))),
                player.getBodyY(0.5D),
                player.getZ() + MathHelper.cos((float)(player.getYaw() * (Math.PI / 180F))));
        }
    }
    @Override public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_SCYTHE_HIT, 1.0F, (float)(1.0F + player.getRandom().nextGaussian() / 10f));
    }
    @Override public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) { return !miner.isCreative(); }
    @Override public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof PlayerEntity player) new WeaponOwnerComponent(stack).setOwner(player.getUuid());
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
        Skin(int c, int sc, @org.jetbrains.annotations.Nullable String tn, @org.jetbrains.annotations.Nullable String l) {
            color=c; shadowColor=sc; tooltipName=tn; lore=l;
        }
        public String getName() { return name().toLowerCase(Locale.ROOT); }
        public static @org.jetbrains.annotations.Nullable Skin fromString(String n) {
            for (Skin s : values()) if (s.getName().equalsIgnoreCase(n)) return s; return null;
        }
        public static Skin getNext(Skin s) { Skin[] v=values(); return v[(s.ordinal()+1)%v.length]; }
    }
}
