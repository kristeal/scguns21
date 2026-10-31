package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.kristeal.util.EnergyViews;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.kristeal.block.LightningBattery;
import top.kristeal.client.screen.LightningBatteryMenu;
import top.kristeal.client.screen.LightningBatteryRecipe;
import top.kristeal.init.ModBlockEntities;
import top.kristeal.item.AirCanisterItem;

import javax.annotation.Nullable;
public class LightningBatteryBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_ENERGY = 32000;

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
                sync();
                updateBlockState();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
                sync();
                updateBlockState();
            }
            return extracted;
        }

        @Override
        public boolean canExtract() {
            return true; // Allows energy to be extracted
        }

        @Override
        public boolean canReceive() {
            return true; // Allows energy to be received
        }
    };

    private final IEnergyStorage internalEnergy = energyStorage;
    // What other mods see: the bottom face gives FE, every other face takes it. A two-way face made connector mods
    // (Create Crafts & Additions) always pull from the battery, so it could not be charged from a network. The battery
    // itself still pushes into receiving neighbours on every side.
    private final IEnergyStorage inputEnergy = EnergyViews.receiveOnly(energyStorage);
    private final IEnergyStorage outputEnergy = EnergyViews.extractOnly(energyStorage);

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private int processingTime;
    private int processingTimeTotal;

    public LightningBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTNING_BATTERY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.lightning_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LightningBatteryMenu(id, inv, this, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> processingTime;
                    case 1 -> processingTimeTotal;
                    case 2 -> energyStorage.getEnergyStored();
                    case 3 -> energyStorage.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> processingTime = value;
                    case 1 -> processingTimeTotal = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        });
    }



    /**
     * Port note: replaces the Forge getCapability override (same side mapping); registered in ModCapabilities.
     */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return itemHandler;
        } else if (side == Direction.DOWN) {
            return new OutputItemHandler(itemHandler);
        } else {
            return new InputItemHandler(itemHandler);
        }
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return internalEnergy;
        return side == Direction.DOWN ? outputEnergy : inputEnergy;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("ProcessingTime", processingTime);
        tag.putInt("ProcessingTimeTotal", processingTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.deserializeNBT(registries, tag.get("Energy"));
        processingTime = tag.getInt("ProcessingTime");
        processingTimeTotal = tag.getInt("ProcessingTimeTotal");
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

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public int getEnergy() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energyStorage.getMaxEnergyStored();
    }

    public void setEnergyStored(int i) {
        energyStorage.receiveEnergy(i, false);
        updateBlockState();
    }
    // Add this to the tick() method in LightningBatteryBlockEntity, replace the existing charging logic:

    public void tick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        boolean wasProcessing = isProcessing();

        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (!inputStack.isEmpty()) {
            IEnergyStorage itemEnergyCap = inputStack.getCapability(Capabilities.EnergyStorage.ITEM);

            if (itemEnergyCap != null) {
                {
                    IEnergyStorage itemEnergy = itemEnergyCap;
                    if (inputStack.getItem() instanceof AirCanisterItem) {
                        chargeAirCanister(inputStack, outputStack);
                    } else {
                        int energyToTransfer = Math.min(energyStorage.extractEnergy(100, true), itemEnergy.receiveEnergy(100, true));
                        if (energyToTransfer > 0) {
                            energyStorage.extractEnergy(energyToTransfer, false);
                            itemEnergy.receiveEnergy(energyToTransfer, false);
                            setChanged();
                            sync();
                        }
                        if (itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored()) {
                            if (outputStack.isEmpty()) {
                                itemHandler.setStackInSlot(OUTPUT_SLOT, inputStack.copy());
                                itemHandler.extractItem(INPUT_SLOT, 1, false);
                            }
                        }
                    }
                }
            } else {
                LightningBatteryRecipe recipe = getRecipe(inputStack);
                if (recipe != null) {
                    ItemStack recipeOutput = recipe.getResultItem(RegistryAccess.EMPTY);
                    if (outputStack.isEmpty() || (outputStack.is(recipeOutput.getItem()) && outputStack.getCount() + recipeOutput.getCount() <= outputStack.getMaxStackSize())) {
                        if (hasEnoughEnergy(recipe.getEnergyUse())) {
                            processingTime++;
                            processingTimeTotal = recipe.getProcessingTime();
                            if (processingTime >= processingTimeTotal) {
                                consumeEnergy(recipe.getEnergyUse());
                                inputStack.shrink(1);
                                if (outputStack.isEmpty()) {
                                    itemHandler.setStackInSlot(OUTPUT_SLOT, recipeOutput.copy());
                                } else {
                                    outputStack.grow(recipeOutput.getCount());
                                }
                                processingTime = 0;
                            }
                        } else {
                            processingTime = 0;
                        }
                    } else {
                        processingTime = 0;
                    }
                } else {
                    processingTime = 0;
                }
            }
        } else {
            processingTime = 0;
        }

        if (wasProcessing != isProcessing()) {
            setChanged();
            sync();
        } else {
            setChanged();
        }

        updateBlockState();
        for (Direction direction : Direction.values()) {
            BlockEntity adjacentEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (adjacentEntity != null) {
                IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(direction), direction.getOpposite());
                if (handler != null) {
                    if (handler.canReceive()) {
                        int extracted = energyStorage.extractEnergy(50, true);
                        int accepted = handler.receiveEnergy(extracted, false);
                        energyStorage.extractEnergy(accepted, false);
                        setChanged();
                        sync();
                    }
                }
            }
        }
    }
    private void chargeAirCanister(ItemStack inputStack, ItemStack outputStack) {
        IEnergyStorage airCap = inputStack.getCapability(Capabilities.EnergyStorage.ITEM);

        if (airCap != null) {
            int energyCostPerAir = 5;
            int airPerTick = 5;
            int energyCostPerTick = airPerTick * energyCostPerAir;
            if (energyStorage.getEnergyStored() >= energyCostPerTick &&
                    airCap.getEnergyStored() < airCap.getMaxEnergyStored()) {
                int maxAirToAdd = Math.min(airPerTick, airCap.getMaxEnergyStored() - airCap.getEnergyStored());
                int actualEnergyCost = maxAirToAdd * energyCostPerAir;
                if (energyStorage.getEnergyStored() >= actualEnergyCost) {
                    int airAdded = airCap.receiveEnergy(maxAirToAdd, false);

                    if (airAdded > 0) {
                        int actualCost = airAdded * energyCostPerAir;
                        energyStorage.extractEnergy(actualCost, false);
                        setChanged();
                        sync();
                    }
                }
            }

            if (airCap.getEnergyStored() >= airCap.getMaxEnergyStored()) {
                if (outputStack.isEmpty()) {
                    itemHandler.setStackInSlot(OUTPUT_SLOT, inputStack.copy());
                    itemHandler.extractItem(INPUT_SLOT, 1, false);
                }
            }
        }
    }


    private LightningBatteryRecipe getRecipe(ItemStack inputStack) {
        if (this.level == null) {
            return null;
        }
        return this.level.getRecipeManager().getRecipeFor(LightningBatteryRecipe.Type.INSTANCE, new SingleRecipeInput(inputStack), this.level)
                .map(RecipeHolder::value).orElse(null);
    }

    public boolean isProcessing() {
        return processingTime > 0 && processingTime < processingTimeTotal;
    }

    private boolean hasEnoughEnergy(int energyUse) {
        return energyStorage.getEnergyStored() >= energyUse;
    }

    private void consumeEnergy(int amount) {
        energyStorage.extractEnergy(amount, false);
    }

    private void updateBlockState() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            int energyStored = this.getEnergy();
            boolean isCharged = energyStored > 0;
            LightningBattery.ChargeLevel chargeLevel = calculateChargeLevel(energyStored);

            this.level.setBlock(this.worldPosition, state.setValue(LightningBattery.CHARGED, isCharged)
                    .setValue(LightningBattery.CHARGE_LEVEL, chargeLevel), 3);
        }
    }

    public static LightningBattery.ChargeLevel calculateChargeLevel(int energyStored) {
        if (energyStored > 24000) {
            return LightningBattery.ChargeLevel.HIGH;
        } else if (energyStored > 12000) {
            return LightningBattery.ChargeLevel.MID;
        } else if (energyStored > 0) {
            return LightningBattery.ChargeLevel.LOW;
        } else {
            return LightningBattery.ChargeLevel.NONE;
        }
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
            if (slot == INPUT_SLOT) {
                return itemHandler.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT;
        }
    }

    private static class OutputItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public OutputItemHandler(ItemStackHandler itemHandler) {
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
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == OUTPUT_SLOT) {
                return itemHandler.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
