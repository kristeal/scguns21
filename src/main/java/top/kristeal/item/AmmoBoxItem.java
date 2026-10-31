package top.kristeal.item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Config;
import top.kristeal.item.ammo_boxes.CreativeAmmoBoxItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Port notes: the contents live in the vanilla DataComponents.BUNDLE_CONTENTS component instead of the
 * reference's "Items" NBT list. The 1.20.1 class imitated the bundle (BundleTooltip, bundle sounds, removeOne from the
 * front), so the stored list keeps the same order and merge rules. Vanilla's BundleContents.Mutable is not used, because
 * its tryInsert caps the contents at one bundle's weight; add/removeOne below keep the reference's capacity logic
 * (getMaxItemCount) and write a new immutable BundleContents back. The unused Forge/Curios capability imports were dropped.
 */
public abstract class AmmoBoxItem extends Item {
    private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);

    public AmmoBoxItem(Item.Properties properties) {
        super(properties);
    }


    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (stack.getCount() == 1 && action == ClickAction.SECONDARY) {
            ItemStack itemStackInSlot = slot.getItem();
            if (itemStackInSlot.isEmpty()) {
                this.playRemoveOneSound(player);
                removeOne(stack).ifPresent(removedStack -> add(stack, slot.safeInsert(removedStack)));
            } else if (canStore(itemStackInSlot)) {
                int maxInsertCount = getMaxItemCount(stack) - getTotalItemCount(stack);
                int itemsToInsert = Math.min(itemStackInSlot.getCount(), maxInsertCount);
                int insertedItems = add(stack, slot.safeTake(itemStackInSlot.getCount(), itemsToInsert, player));
                if (insertedItems > 0) {
                    this.playInsertSound(player);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess slotAccess) {
        if (stack.getCount() != 1) {
            return false;
        } else if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (otherStack.isEmpty()) {
                removeOne(stack).ifPresent(removedStack -> {
                    this.playRemoveOneSound(player);
                    slotAccess.set(removedStack);
                });
            } else if (canStore(otherStack)) {
                int maxInsertCount = getMaxItemCount(stack) - getTotalItemCount(stack);
                int itemsToInsert = Math.min(otherStack.getCount(), maxInsertCount);
                int insertedItems = add(stack, otherStack.copyWithCount(itemsToInsert));
                if (insertedItems > 0) {
                    this.playInsertSound(player);
                    otherStack.shrink(insertedItems);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getTotalItemCount(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.min(1 + 12 * getTotalItemCount(stack) / getMaxItemCount(stack), 13);
    }

    protected abstract ResourceLocation getAmmoTag();

    /** Whether this box accepts the stack. Defaults to the ammo tag; boxes with wider rules override it. */
    public boolean canStore(ItemStack stack) {
        return stack.is(ItemTags.create(getAmmoTag()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    public static int add(ItemStack pouchStack, ItemStack insertedStack) {
        if (!insertedStack.isEmpty() && ((AmmoBoxItem) pouchStack.getItem()).canStore(insertedStack)) {
            List<ItemStack> contents = getStoredContents(pouchStack);

            int maxItemCount = getMaxItemCount(pouchStack);
            int itemsToInsert = Math.min(insertedStack.getCount(), maxItemCount - getTotalItemCount(pouchStack));

            if (itemsToInsert == 0) {
                return 0;
            }

            for (int i = 0; i < contents.size(); i++) {
                ItemStack existingStack = contents.get(i);
                if (ItemStack.isSameItemSameComponents(existingStack, insertedStack)) {
                    int remainingSpace = Math.min(existingStack.getMaxStackSize() - existingStack.getCount(), itemsToInsert);
                    existingStack.grow(remainingSpace);
                    itemsToInsert -= remainingSpace;
                    if (itemsToInsert <= 0) {
                        break;
                    }
                }
            }

            while (itemsToInsert > 0) {
                int countToInsert = Math.min(insertedStack.getMaxStackSize(), itemsToInsert);
                contents.add(insertedStack.copyWithCount(countToInsert));
                itemsToInsert -= countToInsert;
            }

            setContents(pouchStack, contents);
            return insertedStack.getCount() - itemsToInsert;
        } else {
            return 0;
        }
    }

    public static int getTotalItemCount(ItemStack stack) {
        return getContents(stack).mapToInt(ItemStack::getCount).sum();
    }

    private static Optional<ItemStack> removeOne(ItemStack stack) {
        List<ItemStack> contents = getStoredContents(stack);
        if (contents.isEmpty()) {
            return Optional.empty();
        } else {
            ItemStack itemStack = contents.remove(0);
            setContents(stack, contents);
            return Optional.of(itemStack);
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        // Port note: 1.21 BundleTooltip takes BundleContents; its weight (the reference passed the total item count, with 64
        // meaning full) is now computed from the stacks, where 1 means one full bundle.
        return Optional.of(new BundleTooltip(new BundleContents(getContents(stack).toList())));
    }
    @Override
    public void onDestroyed(@NotNull ItemEntity itemEntity) {
        ItemUtils.onContainerDestroyed(itemEntity, getContents(itemEntity.getItem()).toList());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {

        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable(getDescriptionKey()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.scguns.hold_shift").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
    protected abstract String getDescriptionKey();
    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    protected void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    public static int getMaxItemCount(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof AmmoBoxItem) {
            double multiplier = Config.COMMON.gameplay.ammoBoxCapacityMultiplier.get();
            return (int) (((AmmoBoxItem) item).getBaseMaxItemCount() * multiplier);
        }
        return 256;
    }

    protected abstract int getBaseMaxItemCount();



    public static Stream<ItemStack> getContents(ItemStack stack) {
        if (stack.getItem() instanceof CreativeAmmoBoxItem) {
            TagKey<Item> ammoTag = ItemTags.create(((CreativeAmmoBoxItem)stack.getItem()).getAmmoTag());
            return BuiltInRegistries.ITEM.stream()
                    .filter(item -> item.builtInRegistryHolder().is(ammoTag))
                    .map(item -> new ItemStack(item, Integer.MAX_VALUE));
        }
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return Stream.empty();
        }
        return contents.itemCopyStream();
    }

    /** Port helper: a mutable copy of the stored stacks (empty when the box has no contents). */
    private static List<ItemStack> getStoredContents(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? new ArrayList<>() : new ArrayList<>(contents.itemCopyStream().toList());
    }

    /**
     * Port helper: replaces the stored stacks. An empty list removes the component, like the reference's
     * removeTagKey(TAG_ITEMS). Also used by ExoSuitAmmoHelper when it takes ammo out of a box.
     */
    public static void setContents(ItemStack stack, List<ItemStack> contents) {
        List<ItemStack> nonEmpty = contents.stream().filter(s -> !s.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            stack.remove(DataComponents.BUNDLE_CONTENTS);
        } else {
            stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(nonEmpty));
        }
    }
}
