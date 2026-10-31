package top.kristeal.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MoldItem extends Item {

    public MoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainingItem = stack.copy();
        remainingItem.setDamageValue(remainingItem.getDamageValue() + 1);
        if (remainingItem.getDamageValue() >= remainingItem.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return remainingItem;
    }
}
