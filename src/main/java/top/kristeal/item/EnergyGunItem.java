package top.kristeal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.interfaces.IEnergyGun;

import java.util.List;

/**
 * No reference gun is an EnergyGunItem; the class is ported so the energy path works for any
 * gun registered with it.
 * <p>
 * Port notes: the Forge {@code initCapabilities} provider is the NeoForge {@code Capabilities.EnergyStorage.ITEM}
 * capability, registered in {@code ModCapabilities} for every EnergyGunItem. The energy stays an int {@code "Energy"}
 * in the stack's custom data (the reference's NBT key), read and written through {@link ExoSuitData} as the air
 * canisters do.
 */
public class EnergyGunItem extends GunItem implements IEnergyGun {
    private final int capacity;

    public EnergyGunItem(Item.Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new ItemEnergyStorage(stack, this.capacity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // Always show the energy bar
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = getMaxEnergyStored(stack);
        return max <= 0 ? 0 : Math.round(13.0F * getEnergyStored(stack) / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00; // Green color for the energy bar
    }

    public int getEnergyStored(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage != null ? storage.getEnergyStored() : 0;
    }

    public int getMaxEnergyStored(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage != null ? storage.getMaxEnergyStored() : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int energyStored = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);

        tooltip.add(Component.translatable("info.scguns.energy")
                .append(": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(energyStored + " / " + maxEnergy + " FE").withStyle(ChatFormatting.GREEN)));
    }

    public static class ItemEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;
        private int energy;

        public ItemEnergyStorage(ItemStack stack, int capacity) {
            this.stack = stack;
            this.capacity = capacity;
            this.energy = loadEnergyFromNBT(); // Load energy from NBT when the capability is created
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(capacity - energy, maxReceive);
            if (!simulate) {
                energy += energyReceived;
                updateEnergyTag();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(energy, maxExtract);
            if (!simulate) {
                energy -= energyExtracted;
                updateEnergyTag();
            }
            return energyExtracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private void updateEnergyTag() {
            CompoundTag tag = ExoSuitData.getCustomTag(stack);
            tag.putInt("Energy", energy);
            ExoSuitData.setCustomTag(stack, tag);
        }

        private int loadEnergyFromNBT() {
            CompoundTag tag = ExoSuitData.getCustomTag(stack);
            return tag.contains("Energy", Tag.TAG_INT) ? tag.getInt("Energy") : 0;
        }
    }

}
