package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Config;
import top.kristeal.client.screen.PolarGeneratorMenu;
import top.kristeal.init.ModBlockEntities;

import javax.annotation.Nullable;

public class PolarGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    private final EnergyStorage energyStorage = new EnergyStorage(24000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
                sync();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
                sync();
            }
            return extracted;
        }
    };

    private final IEnergyStorage internalEnergy = energyStorage;
    private final IEnergyStorage externalEnergy = new EnergyStorage(energyStorage.getMaxEnergyStored()) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final ItemStackHandler itemHandler = createHandler();
    private final IItemHandlerModifiable manualHandler = itemHandler;
    private final IItemHandler topHandler = new InputItemHandler(itemHandler);
    private final IItemHandler sideHandler = new SideItemHandler(itemHandler);
    private int burnTime;
    private int burnTimeTotal;
    private static final float WHEEL_ROTATION_SPEED = 20.0f;
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTimeTotal;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POLAR_GENERATOR.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.polar_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new PolarGeneratorMenu(id, playerInventory, this, data);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                sync();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (slot == 0) {
                    return super.insertItem(slot, stack, simulate);
                }
                return stack;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    /**
     * Port note: replaces the Forge getCapability override (same side mapping); registered in ModCapabilities.
     */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return manualHandler;
        } else if (side == Direction.UP) {
            return topHandler;
        } else {
            return sideHandler;
        }
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? internalEnergy : externalEnergy;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("polar_generator.burnTime", burnTime);
        tag.putInt("polar_generator.burnTimeTotal", burnTimeTotal);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        burnTime = tag.getInt("polar_generator.burnTime");
        burnTimeTotal = tag.getInt("polar_generator.burnTimeTotal");
        energyStorage.deserializeNBT(registries, tag.get("Energy"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, PolarGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.burnTime > 0) {
                blockEntity.burnTime--;
                // Use the config value instead of hardcoded 50
                blockEntity.energyStorage.receiveEnergy(Config.COMMON.gameplay.energyProductionRate.get(), false);
                blockEntity.setChanged();
                blockEntity.sync();
            }
            for (Direction direction : Direction.values()) {
                BlockEntity adjacentEntity = level.getBlockEntity(pos.relative(direction));
                if (adjacentEntity != null) {
                    IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
                    if (handler != null) {
                        if (handler.canReceive()) {
                            // Use the config value here as well for consistent energy transfer
                            int extracted = blockEntity.energyStorage.extractEnergy(
                                    Config.COMMON.gameplay.energyProductionRate.get(), true);
                            int accepted = handler.receiveEnergy(extracted, false);
                            blockEntity.energyStorage.extractEnergy(accepted, false);
                            blockEntity.setChanged();
                            blockEntity.sync();
                        }
                    }
                }
            }

            if (blockEntity.burnTime == 0 && blockEntity.energyStorage.getEnergyStored() < blockEntity.energyStorage.getMaxEnergyStored()) {
                ItemStack fuelStack = blockEntity.itemHandler.getStackInSlot(0);
                if (!fuelStack.isEmpty()) {
                    int burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
                    if (burnTime > 0) {
                        blockEntity.burnTime = burnTime;
                        blockEntity.burnTimeTotal = burnTime;
                        ItemStack containerItem = fuelStack.getCraftingRemainingItem();
                        fuelStack.shrink(1);
                        if (!containerItem.isEmpty() && fuelStack.isEmpty()) {
                            blockEntity.itemHandler.setStackInSlot(0, containerItem);
                        }
                        else if (!containerItem.isEmpty() && !fuelStack.isEmpty()) {
                            ItemStack remainder = blockEntity.itemHandler.insertItem(0, containerItem, false);
                            if (!remainder.isEmpty() && blockEntity.level != null) {
                                net.minecraft.world.Containers.dropItemStack(blockEntity.level,
                                        blockEntity.worldPosition.getX() + 0.5,
                                        blockEntity.worldPosition.getY() + 1.0,
                                        blockEntity.worldPosition.getZ() + 0.5,
                                        remainder);
                            }
                        }

                        blockEntity.setChanged();
                        blockEntity.sync();
                    }
                }
            }

            boolean isLit = blockEntity.burnTime > 0;
            if (state.getValue(BlockStateProperties.LIT) != isLit) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isLit), 3);
            }
        }
    }

    public float getWheelRotation(float partialTicks) {
        assert level != null;
        return (level.getGameTime() + partialTicks) * WHEEL_ROTATION_SPEED % 360;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    private static class InputItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public InputItemHandler(ItemStackHandler itemHandler) {
            this.itemHandler = itemHandler;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0) {
                return itemHandler.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0;
        }
    }

    private static class SideItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public SideItemHandler(ItemStackHandler itemHandler) {
            this.itemHandler = itemHandler;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0) {
                return itemHandler.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0;
        }
    }
}
