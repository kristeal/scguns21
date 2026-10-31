package top.kristeal.item;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import top.kristeal.init.ModTags;

/**
 * Create: Ironworks compat: an all-in-one tool that mines anything an axe, hoe, pickaxe or shovel can
 * ({@code #scguns:mineable/paxel}) and reports all of their abilities. Registered only when Create: Ironworks is loaded.
 * <p>
 * Port note: the reference extended TieredItem with hand-written correct-tool, speed and attribute code; 1.21 builds the
 * same from the tier's Tool component, so it is a DiggerItem over a block tag. Attack damage +4.5 and speed -3.05 are
 * the reference's values.
 */
public class AnthralitePaxelItem extends DiggerItem {
    public AnthralitePaxelItem(Tier tier, Properties properties) {
        super(tier, ModTags.Blocks.MINEABLE_WITH_PAXEL, properties.attributes(DiggerItem.createAttributes(tier, 4.5F, -3.05F)));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
    }
}
