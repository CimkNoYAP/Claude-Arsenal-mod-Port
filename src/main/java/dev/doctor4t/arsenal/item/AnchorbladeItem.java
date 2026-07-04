package dev.doctor4t.arsenal.item;

import dev.doctor4t.arsenal.cca.ArsenalComponents;
import dev.doctor4t.arsenal.cca.WeaponOwnerComponent;
import dev.doctor4t.arsenal.entity.AnchorbladeEntity;
import dev.doctor4t.arsenal.index.ArsenalCosmetics;
import dev.doctor4t.arsenal.index.ArsenalEnchantments;
import dev.doctor4t.arsenal.index.ArsenalItems;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

// In 1.21.1, PickaxeItem's constructor is PickaxeItem(ToolMaterial, Settings) with
// hardcoded 1.0F damage and -2.8F speed. We extend MiningToolItem directly to keep
// the original weapon-level damage/speed values.
public class AnchorbladeItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {
    public AnchorbladeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(attackDamage, attackSpeed, material, BlockTags.PICKAXE_MINEABLE, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockState blockStateClicked = context.getWorld().getBlockState(context.getBlockPos());
        PlayerEntity user = context.getPlayer();
        if (user != null && user.isSneaking()
            && (blockStateClicked.isIn(BlockTags.ANVIL) || blockStateClicked.isOf(Blocks.SMITHING_TABLE))
            && context.getWorld().isClient) {
            if (ArsenalCosmetics.isSupporter(user.getUuid())) {
                WeaponOwnerComponent weaponOwnerComponent = ArsenalComponents.WEAPON_OWNER_COMPONENT.get(user.getStackInHand(context.getHand()));
                Skin currentSkin = Skin.fromString(ArsenalCosmetics.getSkin(context.getStack()));
                if (currentSkin == null) currentSkin = Skin.DEFAULT;
                ArsenalCosmetics.setSkin(weaponOwnerComponent.getOwner(), context.getStack(), Skin.getNext(currentSkin).getName());
                context.getPlayer().playSound(SoundEvents.BLOCK_SMITHING_TABLE_USE, 0.5f, 1.0f);
                return ActionResult.SUCCESS;
            } else {
                if (context.getWorld().isClient) {
                    user.sendMessage(Text.translatable("tooltip.supporter_only").styled(style -> style.withColor(0xCC0000)));
                    context.getPlayer().playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1.0f);
                }
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(context);
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
            // Check for riptide enchantment
            int riptide = ArsenalEnchantments.getLevel(world, Enchantments.RIPTIDE, stack);
            if (riptide <= 0 || user.isTouchingWaterOrRain()) {
                if (!world.isClient) {
                    EquipmentSlot activeSlot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.damage(1, user, activeSlot);
                    if (riptide == 0) {
                        AnchorbladeEntity anchorbladeEntity = new AnchorbladeEntity(world, user, stack);
                        anchorbladeEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                            2.5F + (float) riptide * 0.5F, 1.0F);
                        owner.arsenal$setAnchor(hand, anchorbladeEntity);
                        world.spawnEntity(anchorbladeEntity);
                        world.playSoundFromEntity(null, anchorbladeEntity,
                            ArsenalSounds.ITEM_ANCHORBLADE_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        return TypedActionResult.success(user.getStackInHand(hand));
                    }
                }
                user.incrementStat(Stats.USED.getOrCreateStat(this));
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type) {
        Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(stack));
        if (skin != null && skin != Skin.DEFAULT) {
            tooltip.accept(Text.literal(skin.tooltipName != null ? skin.tooltipName : TextUtils.formatValueString(skin.getName()))
                .styled(style -> style.withColor(skin.getFirstColor())));
            if (skin.lore != null) {
                if (Screen.hasShiftDown()) {
                    MutableText translatable = Text.translatable(skin.lore);
                    for (String line : translatable.getString().split("\n")) {
                        tooltip.accept(Text.literal(line).styled(style -> style.withColor(Formatting.DARK_GRAY)));
                    }
                } else {
                    tooltip.accept(Text.translatable("tooltip.arsenal.hidden").styled(style -> style.withColor(Formatting.DARK_GRAY)));
                }
            }
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public void spawnHitParticles(PlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            Skin skin = Skin.DEFAULT;
            Skin toSkin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
            if (toSkin != null) skin = toSkin;
            Pair<Integer, Integer> colorPair = new Pair<>(skin.getFirstColor(), skin.getLastColor());
            SweepParticleUtil.sendSweepPacketToClient(serverWorld, colorPair,
                player.getX() + -MathHelper.sin((float)(player.getYaw() * (Math.PI / 180F))),
                player.getBodyY(0.5D),
                player.getZ() + MathHelper.cos((float)(player.getYaw() * (Math.PI / 180F))));
        }
    }

    @Override
    public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_ANCHORBLADE_HIT, 1.0F, (float)(1.0F + player.getRandom().nextGaussian() / 10f));
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative();
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof PlayerEntity player) {
            WeaponOwnerComponent weaponOwnerComponent = ArsenalComponents.WEAPON_OWNER_COMPONENT.get(stack);
            weaponOwnerComponent.setOwner(player.getUuid());
        }
    }

    public enum Skin {
        DEFAULT(0xFF7E7E7E, 0xFF505050, null, null),
        TITAN(0xFF708090, 0xFF465060, "Titan-class", null),
        VOID(0xFF191970, 0xFF0D0D40, null, "tooltip.arsenal.anchorblade_void");

        private final int firstColor;
        private final int lastColor;
        public final @org.jetbrains.annotations.Nullable String lore;
        public final @org.jetbrains.annotations.Nullable String tooltipName;

        Skin(int firstColor, int lastColor, @org.jetbrains.annotations.Nullable String tooltipName,
             @org.jetbrains.annotations.Nullable String lore) {
            this.firstColor = firstColor; this.lastColor = lastColor;
            this.lore = lore; this.tooltipName = tooltipName;
        }

        public int getFirstColor() { return firstColor; }
        public int getLastColor() { return lastColor; }
        public String getName() { return this.name().toLowerCase(Locale.ROOT); }

        @org.jetbrains.annotations.Nullable
        public static Skin fromString(String name) {
            for (Skin skin : Skin.values()) if (skin.getName().equalsIgnoreCase(name)) return skin;
            return null;
        }

        public static Skin getNext(Skin skin) {
            Skin[] values = Skin.values();
            return values[(skin.ordinal() + 1) % values.length];
        }
    }
}
