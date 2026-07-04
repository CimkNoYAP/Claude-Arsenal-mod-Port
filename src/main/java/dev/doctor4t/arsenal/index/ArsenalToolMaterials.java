package dev.doctor4t.arsenal.index;

import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

/**
 * In 1.21.1, ToolMaterial is a record:
 * record ToolMaterial(TagKey<Block> incorrectBlocksForDrops, int durability,
 *                     float speed, float attackDamageBonus, int enchantmentValue,
 *                     TagKey<Item> repairItems)
 */
public interface ArsenalToolMaterials {
    // INCORRECT_FOR_NETHERITE_TOOL = only bedrock/barriers can't be mined
    // (high-tier, matching original MiningLevels.NETHERITE)
    ToolMaterial SCYTHE = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        2031,   // durability
        9.0F,   // mining speed
        4.0F,   // attack damage bonus
        28,     // enchantment value
        ItemTags.REPAIRS_IRON_TOOLS
    );

    ToolMaterial ANCHORBLADE = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        2031,
        9.0F,
        4.0F,
        28,
        ItemTags.REPAIRS_IRON_TOOLS
    );
}
