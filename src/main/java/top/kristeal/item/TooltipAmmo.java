package top.kristeal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TooltipAmmo extends AmmoItem {
    private final int armorBypassAmount;
    private final String descriptionKey;

    public TooltipAmmo(Properties properties, int armorBypassAmount) {
        super(properties);
        this.armorBypassAmount = armorBypassAmount;
        this.descriptionKey = null;
    }

    public TooltipAmmo(Properties properties, String descriptionKey) {
        super(properties);
        this.armorBypassAmount = -1;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (armorBypassAmount >= 0) {
            tooltip.add(getArmorBypassTooltip());
        }
        if (descriptionKey != null) {
            tooltip.add(getDescriptionTooltip());
        }
    }

    private Component getArmorBypassTooltip() {
        return Component.translatable("tooltip.scguns.armor_bypass", armorBypassAmount)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    private Component getDescriptionTooltip() {
        assert descriptionKey != null;
        return Component.translatable(descriptionKey)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }
}
