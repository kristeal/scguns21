package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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
import top.kristeal.block.PoweredMechanicalPressBlock;
import top.kristeal.client.screen.PoweredMechanicalPressMenu;
import top.kristeal.client.screen.PoweredMechanicalPressRecipe;
import top.kristeal.init.ModBlockEntities;
import top.kristeal.item.MoldItem;

import java.util.Optional;

public class PoweredMechanicalPressBlockEntity extends BlockEntity implements MenuProvider {

    public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            assert level != null;
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                if (isInputSlot(slot) || isMoldSlot(slot)) {
                    if (!isRecipeValid()) {
                        resetProgress();
                    }
                }
            }
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Handle mold items insertion
            if (stack.getItem() instanceof MoldItem) {
                ItemStack moldStack = itemHandler.getStackInSlot(MOLD_SLOT);
                if (moldStack.isEmpty() || (moldStack.isDamageableItem() && moldStack.getDamageValue() < moldStack.getMaxDamage())) {
                    return super.insertItem(MOLD_SLOT, stack, simulate);
                }
            }

            return super.insertItem(slot, stack, simulate);
        }
    };

    private boolean isInputSlot(int slot) {
        return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT;
    }

    private boolean isMoldSlot(int slot) {
        return slot == MOLD_SLOT;
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
    public static final int LAST_INPUT_SLOT = 2;
    public static final int MOLD_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    private float pressPosition = 0.0f;
    private final float pressSpeed = 0.04f;
    private boolean movingDown = true;

    public PoweredMechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored();
                    case 3 -> energyStorage.getMaxEnergyStored();
                    default -> 0;
                };
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
        return Component.translatable("container.powered_mechanical_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PoweredMechanicalPressMenu(id, inv, this, this.data);
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
            return new TopItemHandler(itemHandler);
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
        tag.putInt("powered_mechanical_press.progress", progress);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("powered_mechanical_press.progress");
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

    public static void tick(Level level, BlockPos pos, BlockState state, PoweredMechanicalPressBlockEntity blockEntity) {
        boolean wasLit = state.getValue(PoweredMechanicalPressBlock.LIT);
        boolean isLit = false;

        if (!level.isClientSide) {
            boolean hasValidRecipe = blockEntity.hasRecipe();
            boolean canOutput = blockEntity.canOutput();

            if (hasValidRecipe && blockEntity.hasEnoughEnergy(50) && canOutput) {
                isLit = true;
                blockEntity.progress++;
                blockEntity.consumeEnergy(50);
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
                level.setBlock(pos, state.setValue(PoweredMechanicalPressBlock.LIT, isLit), 3);
            }
        }

        if (state.getValue(PoweredMechanicalPressBlock.LIT)) {
            blockEntity.updatePressPosition();
        }
    }


    private boolean canOutput() {
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        RecipeWrapper inventory = createRecipeInput();
        Optional<PoweredMechanicalPressRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);

        if (match.isPresent()) {
            ItemStack resultItem = match.get().getResultItem(level.registryAccess());
            if (outputStack.isEmpty() || (outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }


    public float getPressPosition(float partialTicks, boolean isLit) {
        if (isLit) {
            return pressPosition + (movingDown ? -pressSpeed : pressSpeed) * partialTicks;
        } else {
            return pressPosition;
        }
    }


    public void updatePressPosition() {
        if (movingDown) {
            pressPosition -= pressSpeed;
            float endPosition = -0.25f;
            if (pressPosition <= endPosition) {
                movingDown = false;
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.05f, 0.60f);
                }
            }
        } else {
            pressPosition += pressSpeed;
            float startPosition = 0.0f;
            if (pressPosition >= startPosition) {
                movingDown = true;
            }
        }
    }

    private boolean hasRecipe() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<PoweredMechanicalPressRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);

        if (match.isPresent()) {
            PoweredMechanicalPressRecipe recipe = match.get();
            this.maxProgress = recipe.getProcessingTime();
            return true;
        }
        return false;
    }
    private void craftItem() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<PoweredMechanicalPressRecipe> match = level.getRecipeManager()
                .getRecipeFor(PoweredMechanicalPressRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        if (match.isPresent()) {
            PoweredMechanicalPressRecipe recipe = match.get();
            ItemStack resultItem = recipe.getResultItem(level.registryAccess());
            ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputStack.isEmpty() || (outputStack.getItem() == resultItem.getItem() && outputStack.getCount() + resultItem.getCount() <= outputStack.getMaxStackSize())) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    for (int i = FIRST_INPUT_SLOT; i <= LAST_INPUT_SLOT; i++) {
                        if (ingredient.test(itemHandler.getStackInSlot(i))) {
                            itemHandler.extractItem(i, 1, false);
                            break;
                        }
                    }
                }

                if (recipe.requiresMold()) {
                    ItemStack moldStack = itemHandler.getStackInSlot(MOLD_SLOT);
                    if (!moldStack.isEmpty() && moldStack.isDamageableItem()) {
                        moldStack.hurtAndBreak(1, (ServerLevel) level, null, item -> {});
                        itemHandler.setStackInSlot(MOLD_SLOT, moldStack);
                    }
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
     * Port note: 1.21 recipes take a RecipeInput. This wraps the input slots and the mold slot (index 3, as in the reference container) in slot order,
     * replacing the reference's copied SimpleContainer (which held the same stack references).
     */
    private RecipeWrapper createRecipeInput() {
        return new RecipeWrapper(new RangedWrapper(itemHandler, FIRST_INPUT_SLOT, MOLD_SLOT + 1));
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private boolean isRecipeValid() {
        RecipeWrapper inventory = createRecipeInput();

        Optional<PoweredMechanicalPressRecipe> currentRecipe = getCurrentRecipe();
        if (currentRecipe.isPresent()) {
            PoweredMechanicalPressRecipe recipe = currentRecipe.get();
            assert level != null;
            return recipe.matches(inventory, level);
        }
        return false;
    }

    private Optional<PoweredMechanicalPressRecipe> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        RecipeManager recipeManager = level.getRecipeManager();
        RecipeWrapper inventory = createRecipeInput();
        return recipeManager.getAllRecipesFor(PoweredMechanicalPressRecipe.Type.INSTANCE).stream()
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
            if (slot == FIRST_INPUT_SLOT || slot == LAST_INPUT_SLOT || stack.getItem() instanceof MoldItem) {
                return itemHandler.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT) {
                return itemHandler.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FIRST_INPUT_SLOT || slot == LAST_INPUT_SLOT || stack.getItem() instanceof MoldItem;
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
    private class TopItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public TopItemHandler(ItemStackHandler itemHandler) {
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
            if (isItemValid(slot, stack)) {
                return itemHandler.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // Disable extraction from the top
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Ensure items are only inserted into input and mold slots
            if (stack.getItem() instanceof MoldItem) {
                return slot == MOLD_SLOT && (itemHandler.getStackInSlot(MOLD_SLOT).isEmpty() ||
                        (itemHandler.getStackInSlot(MOLD_SLOT).isDamageableItem() &&
                                itemHandler.getStackInSlot(MOLD_SLOT).getDamageValue() < itemHandler.getStackInSlot(MOLD_SLOT).getMaxDamage()));
            }
            return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT;
        }
    }

}
