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
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import java.util.function.Consumer;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import java.util.Locale;

public class AnchorbladeItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {
    public AnchorbladeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, BlockTags.PICKAXE_MINEABLE,
            settings.attributeModifiers(MiningToolItem.createAttributeModifiers(material, attackDamage, attackSpeed)));
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
                    stack.damage(1, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    if (riptide == 0) {
                        AnchorbladeEntity anchor = new AnchorbladeEntity(world, user, stack);
                        anchor.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 2.5f, 1f);
                        owner.arsenal$setAnchor(hand, anchor);
                        world.spawnEntity(anchor);
                        world.playSoundFromEntity(null, anchor, ArsenalSounds.ITEM_ANCHORBLADE_THROW, SoundCategory.PLAYERS, 1f, 1f);
                        return TypedActionResult.success(stack);
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
                .styled(s -> s.withColor(skin.firstColor)));
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
            SweepParticleUtil.sendSweepPacketToClient(sw, new Pair<>(skin.firstColor, skin.lastColor),
                player.getX() - MathHelper.sin((float)(player.getYaw()*(Math.PI/180F))),
                player.getBodyY(0.5), player.getZ() + MathHelper.cos((float)(player.getYaw()*(Math.PI/180F))));
        }
    }
    @Override public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_ANCHORBLADE_HIT, 1f, (float)(1f+player.getRandom().nextGaussian()/10f));
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
                user.playSound(SoundEvents.BLOCK_SMITHING_TABLE_USE, 0.5f, 1f);
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("tooltip.supporter_only").styled(s -> s.withColor(0xCC0000)));
                user.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1f);
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(ctx);
    }

    public enum Skin {
        DEFAULT(0xFF7E7E7E, 0xFF505050, null, null, Identifier.of("arsenal","textures/entity/chain.png")),
        LUXINTRUS(0xFF4B0082, 0xFF2D004D, "Luxintrus", null, Identifier.of("arsenal","textures/entity/chain_luxintrus.png")),
        CARRION(0xFFE9DFB8, 0xFF9D806E, null, null, Identifier.of("arsenal","textures/entity/chain_carrion.png")),
        GILDED(0xFFF1BC5A, 0xFFE28634, null, null, Identifier.of("arsenal","textures/entity/chain_gilded.png")),
        WINSWEEP(0xFF00BFFF, 0xFF0080AA, "Winsweep", null, Identifier.of("arsenal","textures/entity/chain_winsweep.png")),
        AMBESSA(0xFFB8860B, 0xFF7A5900, "Ambessa", null, Identifier.of("arsenal","textures/entity/chain_ambessa.png")),
        TITAN(0xFF708090, 0xFF465060, "Titan-class", null, Identifier.of("arsenal","textures/entity/chain.png")),
        VOID(0xFF191970, 0xFF0D0D40, null, "tooltip.arsenal.anchorblade_void", Identifier.of("arsenal","textures/entity/chain.png"));

        public final int firstColor, lastColor;
        public final @org.jetbrains.annotations.Nullable String lore, tooltipName;
        public final Identifier chainTexture;
        public final ModelIdentifier anchorbladeEntityModel;

        Skin(int fc, int lc, @org.jetbrains.annotations.Nullable String tn, @org.jetbrains.annotations.Nullable String l, Identifier chainTex) {
            firstColor=fc; lastColor=lc; tooltipName=tn; lore=l; chainTexture=chainTex;
            anchorbladeEntityModel = new ModelIdentifier(Identifier.of("arsenal", "anchorblade_" + name().toLowerCase(Locale.ROOT) + "_in_hand"), "");
        }
        public String getName() { return name().toLowerCase(Locale.ROOT); }
        public static @org.jetbrains.annotations.Nullable Skin fromString(String n) { for(Skin s:values()) if(s.getName().equalsIgnoreCase(n)) return s; return null; }
        public static Skin getNext(Skin s) { Skin[] v=values(); return v[(s.ordinal()+1)%v.length]; }
    }
}
