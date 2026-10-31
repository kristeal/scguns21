package top.kristeal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GlintedBlueprintItem extends BlueprintItem {
    public GlintedBlueprintItem(Properties properties) {
        // Port note: Item#isFireResistant() was removed in 1.20.5; fire resistance is now an item property
        super(properties.fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

}


