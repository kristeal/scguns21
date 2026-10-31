package top.kristeal.common.exosuit;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Helper class for managing ExoSuit upgrade data
 */
public class ExoSuitData {

    public static final HolderLookup.Provider REGISTRY_ACCESS =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    // NBT keys for storing upgrade data
    private static final String UPGRADES_TAG = "ExoSuitUpgrades";
    private static final String HELMET_UPGRADES = "HelmetUpgrades";
    private static final String CHEST_UPGRADES = "ChestUpgrades";
    private static final String LEG_UPGRADES = "LegUpgrades";
    private static final String BOOT_UPGRADES = "BootUpgrades";

    public static CompoundTag getCustomTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void setCustomTag(ItemStack stack, CompoundTag tag) {
        if (!stack.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static CompoundTag saveItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return (CompoundTag) stack.save(REGISTRY_ACCESS, new CompoundTag());
    }

    public static ItemStack loadItemStack(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(REGISTRY_ACCESS, tag);
    }

    /**
     * Gets the upgrade data compound from an exosuit piece
     */
    public static CompoundTag getUpgradeData(ItemStack exosuitPiece) {
        if (exosuitPiece.isEmpty()) {
            return new CompoundTag();
        }
        return getCustomTag(exosuitPiece).getCompound(UPGRADES_TAG);
    }

    /**
     * Sets upgrade data on an exosuit piece
     */
    public static void setUpgradeData(ItemStack exosuitPiece, CompoundTag upgradeData) {
        if (!exosuitPiece.isEmpty()) {
            CompoundTag root = getCustomTag(exosuitPiece);
            root.put(UPGRADES_TAG, upgradeData);
            setCustomTag(exosuitPiece, root);
        }
    }

    /**
     * Checks if an exosuit piece has any upgrades installed
     */
    public static boolean hasUpgrades(ItemStack exosuitPiece) {
        CompoundTag upgrades = getUpgradeData(exosuitPiece);
        if (upgrades.contains("Upgrades")) {
            ListTag upgradeList = upgrades.getList("Upgrades", 10);
            return !upgradeList.isEmpty();
        }
        return false;
    }

    /**
     * Gets an upgrade item from a specific slot
     */
    public static ItemStack getUpgradeInSlot(ItemStack exosuitPiece, int slot) {
        CompoundTag upgradeData = getUpgradeData(exosuitPiece);

        if (upgradeData.contains("Upgrades")) {
            ListTag upgradeList = upgradeData.getList("Upgrades", 10);

            for (int i = 0; i < upgradeList.size(); i++) {
                CompoundTag slotTag = upgradeList.getCompound(i);
                if (slotTag.getInt("Slot") == slot) {
                    if (slotTag.contains("Item")) {
                        return loadItemStack(slotTag.getCompound("Item"));
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

}
