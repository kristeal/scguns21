package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.kristeal.util.EnergyViews;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.block.PoweredMaceratorBlock;
import top.kristeal.client.screen.MaceratorMenu;
import top.kristeal.client.screen.PoweredMaceratorRecipe;
import top.kristeal.client.screen.PoweredMaceratorMenu;
import top.kristeal.init.ModBlockEntities;

import java.util.Optional;

public class PoweredMaceratorBlockEntity extends BlockEntity implements MenuProvider {

    public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                if (isInputSlot(slot)) {
                    if (!isRecipeValid()) {
                        resetProgress();
                    }
                }
            }
        }
    };

    private boolean isInputSlot(int slot) {
        return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT;
    }

    private final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;
    private final EnergyStorage energyStorage = new EnergyStorage(16000) {
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
    public static final int FIRST_INPUT_SLOT = 0;
    public static final int LAST_INPUT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    private static final float WHEEL_ROTATION_SPEED = 20.0f;
    public PoweredMaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_MACERATOR.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return progress;
                    case 1: return maxProgress;
                    case 2: return energyStorage.getEnergyStored();
                    case 3: return energyStorage.getMaxEnergyStored();
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: progress = value; break;
                    case 1: maxProgress = value; break;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.powered_macerator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PoweredMaceratorMenu(id, inv, this, this.data);
    }

    /**
     * Port note: replaces the Forge getCapability override; registered in ModCapabilities
     * (Capabilities.ItemHandler.BLOCK). The side mapping is unchanged from the reference.
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

    private final IEnergyStorage externalEnergy = EnergyViews.receiveOnly(energyStorage);

    /** Port note: replaces the Forge ENERGY capability; registered in ModCapabilities (Capabilities.EnergyStorage.BLOCK). Receive-only so cable and connector mods push into it instead of draining it. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return externalEnergy;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("powered_macerator.progress", progress);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("powered_macerator.progress");
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
    public static void tick(Level level, BlockPos pos, BlockState state, PoweredMaceratorBlockEntity blockEntity) {
        boolean wasLit = state.getValue(PoweredMaceratorBlock.LIT);
        boolean isLit = false;

        if (!level.isClientSide) {
            // Fix for a reference bug: it looked up the plain macerating recipes at a fixed 50 FE/tick and 100 ticks. It now
            // uses its own powered_macerating recipes: their processingTime, and their energyUse (total FE per craft).
            Optional<PoweredMaceratorRecipe> recipe = blockEntity.getCurrentRecipe();
            ItemStack resultItem = blockEntity.getRecipeResult();
            int energyPerTick = recipe.map(r -> Math.max(1, Mth.ceil(r.getEnergyUse() / (float) Math.max(1, r.getProcessingTime())))).orElse(0);
            recipe.ifPresent(r -> blockEntity.maxProgress = Math.max(1, r.getProcessingTime()));

            if (recipe.isPresent() && blockEntity.hasEnoughEnergy(energyPerTick) && blockEntity.hasSpaceForOutput(resultItem)) {
                isLit = true;
                blockEntity.progress++;
                blockEntity.consumeEnergy(energyPerTick);
                if (blockEntity.progress >= blockEntity.maxProgress) {
                    blockEntity.craftItem();
                    blockEntity.resetProgress();
                }
            } else {
                if (blockEntity.progress > 0) {
                    blockEntity.resetProgress();
                }
            }

            if (wasLit != isLit) {
                level.setBlock(pos, state.setValue(PoweredMaceratorBlock.LIT, isLit), 3);
            }
        }
    }

    private ItemStack getRecipeResult() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<PoweredMaceratorRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMaceratorRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        return match.map(recipe -> recipe.getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private boolean hasSpaceForOutput(ItemStack output) {
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        return currentOutput.isEmpty() || (currentOutput.getItem() == output.getItem() && currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize());
    }

    private boolean hasRecipe() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<PoweredMaceratorRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMaceratorRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        return match.isPresent();
    }

    private void craftItem() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<PoweredMaceratorRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMaceratorRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        if (match.isPresent()) {
            PoweredMaceratorRecipe recipe = match.get();
            ItemStack resultItem = recipe.getResultItem(level.registryAccess());
            ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputStack.isEmpty() || (outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize())) {
                for (int i = FIRST_INPUT_SLOT; i <= LAST_INPUT_SLOT; i++) {
                    itemHandler.extractItem(i, 1, false);
                }
                if (outputStack.isEmpty()) {
                    itemHandler.setStackInSlot(OUTPUT_SLOT, resultItem.copy());
                } else {
                    outputStack.grow(resultItem.getCount());
                }
            }
        }
    }

    /**
     * Port note: 1.21 recipes take a RecipeInput. This wraps the input slots in slot order,
     * replacing the reference's copied SimpleContainer (which held the same stack references).
     */
    private RecipeWrapper createRecipeInput() {
        return new RecipeWrapper(new RangedWrapper(itemHandler, FIRST_INPUT_SLOT, LAST_INPUT_SLOT + 1));
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private boolean isRecipeValid() {
        RecipeWrapper inventory = createRecipeInput();

        Optional<PoweredMaceratorRecipe> currentRecipe = getCurrentRecipe();
        if (currentRecipe.isPresent()) {
            PoweredMaceratorRecipe recipe = currentRecipe.get();
            return recipe.matches(inventory, level);
        }
        return false;
    }

    private Optional<PoweredMaceratorRecipe> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        RecipeManager recipeManager = level.getRecipeManager();
        RecipeWrapper inventory = createRecipeInput();
        return recipeManager.getAllRecipesFor(PoweredMaceratorRecipe.Type.INSTANCE).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.matches(inventory, level))
                .findFirst();
    }

    private boolean hasEnoughEnergy(int amount) {
        return energyStorage.getEnergyStored() >= amount;
    }

    public void consumeEnergy(int amount) {
        energyStorage.extractEnergy(amount, false);
        setChanged();
        sync();
    }

    public void addEnergy(int amount) {
        energyStorage.receiveEnergy(amount, false);
        setChanged();
        sync();
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

    public float getWheelRotation(float partialTicks) {
        assert level != null;
        return (level.getGameTime() + partialTicks) * WHEEL_ROTATION_SPEED % 360;
    }


    private record InputItemHandler(ItemStackHandler itemHandler) implements IItemHandlerModifiable {

        @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT) {
                    itemHandler.setStackInSlot(slot, stack);
                }
            }

            @Override
            public int getSlots() {
                return LAST_INPUT_SLOT - FIRST_INPUT_SLOT + 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (slot >= 0 && slot < getSlots()) {
                    return itemHandler.getStackInSlot(FIRST_INPUT_SLOT + slot);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot >= 0 && slot < getSlots()) {
                    int actualSlot = FIRST_INPUT_SLOT + slot;
                    ItemStack result = itemHandler.insertItem(actualSlot, stack, simulate);
                    if (!result.equals(stack)) {
                        return result;
                    }
                }
                return insertIntoNextAvailableSlot(stack, simulate);
            }

            private ItemStack insertIntoNextAvailableSlot(ItemStack stack, boolean simulate) {
                for (int i = FIRST_INPUT_SLOT; i <= LAST_INPUT_SLOT; i++) {
                    ItemStack existingStack = itemHandler.getStackInSlot(i);
                    if (!existingStack.isEmpty() && ItemStack.isSameItem(existingStack, stack)) {
                        ItemStack result = itemHandler.insertItem(i, stack, simulate);
                        if (!result.equals(stack)) {
                            return result;
                        }
                    }
                }

                for (int i = FIRST_INPUT_SLOT; i <= LAST_INPUT_SLOT; i++) {
                    ItemStack existingStack = itemHandler.getStackInSlot(i);
                    if (existingStack.isEmpty()) {
                        return itemHandler.insertItem(i, stack, simulate);
                    }
                }

                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                if (slot >= 0 && slot < getSlots()) {
                    return itemHandler.getSlotLimit(FIRST_INPUT_SLOT + slot);
                }
                return 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot >= 0 && slot < getSlots();
            }
        }

    private record OutputItemHandler(ItemStackHandler itemHandler) implements IItemHandlerModifiable {

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
