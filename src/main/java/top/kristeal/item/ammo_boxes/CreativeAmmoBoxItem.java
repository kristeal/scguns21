package top.kristeal.item.ammo_boxes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
// Port note: the reference imported top.ribs.scguns.common.Gun and util.GunModifierHelper here without using them; dropped
import top.kristeal.item.AmmoBoxItem;

import java.util.Optional;
import java.util.stream.Stream;

public class CreativeAmmoBoxItem extends AmmoBoxItem {
    private static final int CREATIVE_BAR_COLOR = Mth.color(0.4F, 0.4F, 0.7F);

    public CreativeAmmoBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public ResourceLocation getAmmoTag() {
        return ResourceLocation.fromNamespaceAndPath("scguns", "ammo");
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return CREATIVE_BAR_COLOR;
    }

    @Override
    protected int getBaseMaxItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    public static int add(ItemStack pouchStack, ItemStack insertedStack) {
        if (pouchStack.getItem() instanceof CreativeAmmoBoxItem) {
            return 0;
        }
        return AmmoBoxItem.add(pouchStack, insertedStack);
    }
    @Override
    protected String getDescriptionKey() {
        return "item.scguns.creative_ammo_box.description";
    }
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess slotAccess) {
        return false;
    }

    /**
     * Port fix: the creative contents are virtual stacks of {@code Integer.MAX_VALUE}; dropping them when the item
     * entity is destroyed spawned stacks 1.21 can't save. Nothing is dropped.
     */
    @Override
    public void onDestroyed(ItemEntity itemEntity) {
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }
}