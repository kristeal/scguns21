package top.kristeal.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import top.kristeal.init.ModTiers;

public class CogMaceItem extends SwordItem {

    public CogMaceItem(Properties properties) {
        super(ModTiers.ANTHRALITE, properties.attributes(AxeItem.createAttributes(Tiers.IRON, 2f, -3.2f)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.resetFallDistance();
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return super.hurtEnemy(stack, target, attacker);
    }
}