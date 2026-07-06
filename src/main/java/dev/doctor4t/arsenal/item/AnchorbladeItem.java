package dev.doctor4t.arsenal.item;

import dev.doctor4t.arsenal.cca.WeaponOwnerComponent;
import dev.doctor4t.arsenal.entity.AnchorbladeEntity;
import dev.doctor4t.arsenal.index.ArsenalCosmetics;
import dev.doctor4t.arsenal.index.ArsenalEnchantments;
import dev.doctor4t.arsenal.index.ArsenalSounds;
import dev.doctor4t.arsenal.util.AnchorOwner;
import dev.doctor4t.arsenal.util.SweepParticleUtil;
import dev.doctor4t.ratatouille.item.CustomHitParticleItem;
import dev.doctor4t.ratatouille.item.CustomHitSoundItem;
import dev.doctor4t.ratatouille.util.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

public class AnchorbladeItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {
    public AnchorbladeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, BlockTags.PICKAXE_MINEABLE,
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
                    .build()));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof AnchorOwner owner) {
            boolean reeling = ArsenalEnchantments.getLevel(world, ArsenalEnchantments.REELING, stack) > 0;
            if (owner.arsenal$isAnchorActive(hand, reeling)) {
                owner.arsenal$getAnchor(hand, reeling).setRecalled(
                    user.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND).isEmpty());
                return TypedActionResult.fail(stack);
            }
            int riptide = ArsenalEnchantments.getLevel(world, Enchantments.RIPTIDE, stack);
            if (riptide <= 0 || user.isTouchingWaterOrRain()) {
                if (!world.isClient) {
                    EquipmentSlot activeSlot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.damage(1, user, activeSlot);
                    if (riptide == 0) {
                        AnchorbladeEntity anchor = new AnchorbladeEntity(world, user, stack);
                        anchor.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 2.5F, 1.0F);
                        owner.arsenal$setAnchor(hand, anchor);
                        world.spawnEntity(anchor);
                        world.playSoundFromEntity(null, anchor, ArsenalSounds.ITEM_ANCHORBLADE_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        return TypedActionResult.success(user.getStackInHand(hand));
                    }
                }
                user.incrementStat(Stats.USED.getOrCreateStat(this));
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @org.jetbrains.annotations.Nullable World world, List<Text> tooltip, TooltipContext context) {
        Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(stack));
        if (skin != null && skin != Skin.DEFAULT) {
            tooltip.add(Text.literal(skin.tooltipName != null ? skin.tooltipName : TextUtils.formatValueString(skin.getName()))
                .styled(s -> s.withColor(skin.firstColor)));
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
            SweepParticleUtil.sendSweepPacketToClient(sw, new Pair<>(skin.firstColor, skin.lastColor),
                player.getX() - MathHelper.sin((float)(player.getYaw()*(Math.PI/180F))),
                player.getBodyY(0.5D),
                player.getZ() + MathHelper.cos((float)(player.getYaw()*(Math.PI/180F))));
        }
    }
    @Override public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_ANCHORBLADE_HIT, 1.0F, (float)(1.0F + player.getRandom().nextGaussian()/10f));
    }
    @Override public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) { return !miner.isCreative(); }
    @Override public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof PlayerEntity player) new WeaponOwnerComponent(stack).setOwner(player.getUuid());
    }

    public enum Skin {
        DEFAULT(0xFF7E7E7E, 0xFF505050, null, null),
        TITAN(0xFF708090, 0xFF465060, "Titan-class", null),
        VOID(0xFF191970, 0xFF0D0D40, null, "tooltip.arsenal.anchorblade_void"),
        LUXINTRUS(0xFF4B0082, 0xFF2D004D, "Luxintrus", null),
        CARRION(0xFFE9DFB8, 0xFF9D806E, null, null),
        GILDED(0xFFF1BC5A, 0xFFE28634, null, null),
        WINSWEEP(0xFF00BFFF, 0xFF0080AA, "Winsweep", null),
        AMBESSA(0xFFB8860B, 0xFF7A5900, "Ambessa", null);

        public final int firstColor, lastColor;
        public final @org.jetbrains.annotations.Nullable String lore, tooltipName;
        public final Identifier anchorbladeEntityModel;

        Skin(int fc, int lc, @org.jetbrains.annotations.Nullable String tn, @org.jetbrains.annotations.Nullable String l) {
            firstColor=fc; lastColor=lc; tooltipName=tn; lore=l;
            anchorbladeEntityModel = Identifier.of("arsenal", "geo/anchorblade_" + name().toLowerCase(Locale.ROOT) + ".geo.json");
        }
        public String getName() { return name().toLowerCase(Locale.ROOT); }
        public static @org.jetbrains.annotations.Nullable Skin fromString(String n) {
            for (Skin s : values()) if (s.getName().equalsIgnoreCase(n)) return s; return null;
        }
        public static Skin getNext(Skin s) { Skin[] v=values(); return v[(s.ordinal()+1)%v.length]; }
    }
}
