package top.kristeal.item.exosuit;

import top.kristeal.Config;

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

import java.util.List;

public class ExoSuitCoreItem extends Item {
    private final CoreTier tier;

    public ExoSuitCoreItem(Properties properties, CoreTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CoreTier getTier() {
        return this.tier;
    }

    public static IEnergyStorage createEnergyStorage(ItemStack stack, CoreTier tier) {
        return new SimpleExoSuitEnergyStorage(stack, tier.getCapacity());
    }

    public static class SimpleExoSuitEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;

        public SimpleExoSuitEnergyStorage(ItemStack stack, int capacity) {
            this.stack = stack;
            this.capacity = capacity;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int currentEnergy = getEnergyFromNBT();
            int energyReceived = Math.min(capacity - currentEnergy, maxReceive);

            if (!simulate && energyReceived > 0) {
                setEnergyToNBT(currentEnergy + energyReceived);
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int currentEnergy = getEnergyFromNBT();
            int energyExtracted = Math.min(currentEnergy, maxExtract);

            if (!simulate && energyExtracted > 0) {
                setEnergyToNBT(currentEnergy - energyExtracted);
            }
            return energyExtracted;
        }

        @Override
        public int getEnergyStored() {
            return getEnergyFromNBT();
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

        private int getEnergyFromNBT() {
            CompoundTag tag = ExoSuitData.getCustomTag(stack);
            return tag.contains("Energy", Tag.TAG_INT) ? tag.getInt("Energy") : 0;
        }

        private void setEnergyToNBT(int energy) {
            CompoundTag tag = ExoSuitData.getCustomTag(stack);
            tag.putInt("Energy", Math.max(0, Math.min(energy, capacity)));
            ExoSuitData.setCustomTag(stack, tag);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energyStored = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);
        if (maxEnergy == 0) return 0;
        return Math.round(13.0F * energyStored / maxEnergy);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int maxEnergy = getMaxEnergyStored(stack);
        if (maxEnergy <= 0) return 0xFF4444;
        float ratio = (float) getEnergyStored(stack) / maxEnergy;

        if (ratio > 0.66f) {
            return 0x00FFFF;
        } else if (ratio > 0.33f) {
            return 0xFFFF00;
        } else {
            return 0xFF4444;
        }
    }

    public int getEnergyStored(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null) {
            return storage.getEnergyStored();
        }
        CompoundTag tag = ExoSuitData.getCustomTag(stack);
        return tag.contains("Energy", Tag.TAG_INT) ? tag.getInt("Energy") : 0;
    }

    public int getMaxEnergyStored(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null) {
            return storage.getMaxEnergyStored();
        }
        return this.tier.getCapacity();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int energyStored = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);

        tooltip.add(Component.translatable("tooltip.scguns.energy")
                .append(": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%,d", energyStored))
                        .withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" / ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%,d", maxEnergy) + " FE")
                        .withStyle(ChatFormatting.BLUE)));
    }

    public enum CoreTier {
        BASIC("Basic"),
        ADVANCED("Advanced"),
        ELITE("Elite");

        private final String displayName;

        CoreTier(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCapacity() {
            return switch (this) {
                case BASIC -> Config.COMMON.exoSuitCores.basicCoreCapacity.get();
                case ADVANCED -> Config.COMMON.exoSuitCores.advancedCoreCapacity.get();
                case ELITE -> Config.COMMON.exoSuitCores.eliteCoreCapacity.get();
            };
        }
    }
}
