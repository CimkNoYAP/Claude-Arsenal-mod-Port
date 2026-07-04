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
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
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
import java.util.function.Consumer;

public class ScytheItem extends MiningToolItem implements CustomHitParticleItem, CustomHitSoundItem, ArsenalWeaponItem {
    private static final UUID REACH_MODIFIER_UUID = UUID.fromString("911af262-067d-4da2-854c-20f03cc2dd8b");
    private static final EntityAttributeModifier REACH_MODIFIER = new EntityAttributeModifier(
        Identifier.of("arsenal", "scythe_attack_range"), 0.5, EntityAttributeModifier.Operation.ADD_VALUE);

    public ScytheItem(ToolMaterial material, float damage, float speed, Settings settings) {
        super(damage, speed, material, BlockTags.HOE_MINEABLE, settings);
    }

    @Override
    public void appendAttributeModifiers(EquipmentSlot slot, Consumer<AttributeEntry> attributeConsumer) {
        super.appendAttributeModifiers(slot, attributeConsumer);
        if (slot == EquipmentSlot.MAINHAND) {
            attributeConsumer.accept(new AttributeEntry(ReachEntityAttributes.ATTACK_RANGE, REACH_MODIFIER));
        }
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (ArsenalEnchantments.getEquipmentLevel(world, ArsenalEnchantments.SPEWING, player) > 0) {
            float f = 1.0f;
            if (!world.isClient) {
                BloodScytheEntity bloodScythe = new BloodScytheEntity(world, player);
                bloodScythe.setOwner(player);
                bloodScythe.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, f * 3.0f, 1.0f);
                bloodScythe.setDamage(bloodScythe.getDamage());
                player.getStackInHand(hand).damage(1, player, EquipmentSlot.MAINHAND);
                bloodScythe.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;

                ArrayList<StatusEffectInstance> statusEffectsHalved = new ArrayList<>();
                float absorption = player.getAbsorptionAmount();
                for (StatusEffectInstance statusEffect : player.getStatusEffects()) {
                    StatusEffectInstance statusHalved = new StatusEffectInstance(
                        statusEffect.getEffectType(), statusEffect.getDuration() / 2,
                        statusEffect.getAmplifier(), statusEffect.isAmbient(),
                        statusEffect.shouldShowParticles(), statusEffect.shouldShowIcon());
                    bloodScythe.addEffect(statusHalved);
                    statusEffectsHalved.add(statusHalved);
                }
                player.clearStatusEffects();
                for (StatusEffectInstance sei : statusEffectsHalved) {
                    player.addStatusEffect(sei);
                }
                player.setAbsorptionAmount(absorption);

                player.damage(world.getDamageSources().create(ArsenalDamageTypes.SPEWING), 3f);
                player.getItemCooldownManager().set(this, 20);
                world.spawnEntity(bloodScythe);

                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    Skin skin = Skin.DEFAULT;
                    Skin toSkin = Skin.fromString(ArsenalCosmetics.getSkin(player.getMainHandStack()));
                    if (toSkin != null) skin = toSkin;
                    Pair<Integer, Integer> colorPair = new Pair<>(skin.color, skin.shadowColor);
                    SweepParticleUtil.sendSweepPacketToClient(serverWorld, colorPair,
                        player.getX() + -MathHelper.sin((float)(player.getYaw() * (Math.PI / 180F))),
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
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type) {
        Skin skin = Skin.fromString(ArsenalCosmetics.getSkin(stack));
        if (skin != null && skin != Skin.DEFAULT) {
            tooltip.accept(Text.literal(skin.tooltipName != null ? skin.tooltipName : TextUtils.formatValueString(skin.getName()))
                .styled(style -> style.withColor(skin.color)));
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
            Pair<Integer, Integer> colorPair = new Pair<>(skin.color, skin.shadowColor);
            SweepParticleUtil.sendSweepPacketToClient(serverWorld, colorPair,
                player.getX() + -MathHelper.sin((float)(player.getYaw() * (Math.PI / 180F))),
                player.getBodyY(0.5D),
                player.getZ() + MathHelper.cos((float)(player.getYaw() * (Math.PI / 180F))));
        }
    }

    @Override
    public void playHitSound(PlayerEntity player) {
        player.playSound(ArsenalSounds.ITEM_SCYTHE_HIT, 1.0F, (float)(1.0F + player.getRandom().nextGaussian() / 10f));
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
        DEFAULT(0xFFD9D9D9, 0xFF7F8885, null, null),
        CLOWN(0xFFD90420, 0xFF8C0420, null, "tooltip.arsenal.scythe_clown"),
        CARRION(0xFFE9DFB8, 0xFF9D806E, null, null),
        GILDED(0xFFF1BC5A, 0xFFE28634, null, null),
        ROZE(0xFFB70066, 0xFF710949, null, null),
        FOLLY(0xFFFF005A, 0xFFBC0045, "Folly Tree Branch", null),
        SCISSORS(0xFFB9B1AF, 0xFF6F686F, null, null);

        public final int color;
        public final int shadowColor;
        public final @org.jetbrains.annotations.Nullable String lore;
        public final @org.jetbrains.annotations.Nullable String tooltipName;

        Skin(int color, int shadowColor, @org.jetbrains.annotations.Nullable String tooltipName,
             @org.jetbrains.annotations.Nullable String lore) {
            this.color = color; this.shadowColor = shadowColor;
            this.lore = lore; this.tooltipName = tooltipName;
        }

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
