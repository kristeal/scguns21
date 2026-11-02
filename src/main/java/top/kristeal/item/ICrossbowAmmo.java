package top.kristeal.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.kristeal.entity.throwable.ThrowableItemEntity;

public interface ICrossbowAmmo
{
    ThrowableItemEntity createThrowable(Level level, LivingEntity shooter, ItemStack stack);
    int loadedItemIndex();
}
