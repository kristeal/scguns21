package top.kristeal.item.exosuit;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DamageableUpgradeItem extends Item {

    public DamageableUpgradeItem(Properties properties) {
        super(properties);
    }

    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return super.getMaxDamage(stack);
    }

    public boolean canBeDepleted() {
        return true;
    }

    public void onUpgradeDamaged(ItemStack stack, int damage) {
        stack.setDamageValue(Math.min(stack.getDamageValue() + damage, stack.getMaxDamage()));
    }

    public boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }
}
