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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import top.kristeal.block.MaceratorBlock;
import top.kristeal.client.screen.MaceratorMenu;
import top.kristeal.client.screen.MaceratorRecipe;
import org.jetbrains.annotations.Nullable;
import top.kristeal.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaceratorBlockEntity extends BlockEntity implements MenuProvider {

    public final ItemStackHandler itemHandler = new ItemStackHandler(6) {
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

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT) {
                ItemStack existingStack = itemHandler.getStackInSlot(slot);
                if (!existingStack.isEmpty() && !ItemStack.isSameItem(existingStack, stack)) {
                    return stack;
                }
            }
            return super.insertItem(slot, stack, simulate);
        }
    };

    private boolean isInputSlot(int slot) {
        return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT;
    }

    private final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private static final float WHEEL_ROTATION_SPEED = 20.0f;
    public static final int FIRST_INPUT_SLOT = 0;
    public static final int LAST_INPUT_SLOT = 3;
    public static final int FUEL_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACERATOR.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return progress;
                    case 1: return maxProgress;
                    case 2: return burnTime;
                    case 3: return maxBurnTime;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: progress = value; break;
                    case 1: maxProgress = value; break;
                    case 2: burnTime = value; break;
                    case 3: maxBurnTime = value; break;
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
        return Component.translatable("container.macerator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        MaceratorMenu menu = new MaceratorMenu(id, inv, this, this.data);
        return menu;
    }

    /**
     * Port note: replaces the Forge getCapability override; registered in ModCapabilities
     * (Capabilities.ItemHandler.BLOCK). The side mapping is unchanged from the reference.
     */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return itemHandler;
        }
        Direction blockFacing = getBlockState().getValue(MaceratorBlock.FACING);

        if (side == Direction.DOWN) {
            return new OutputItemHandler(itemHandler);
        } else if (side == Direction.UP) {
            return new InputItemHandler(itemHandler);
        } else if (side == blockFacing.getOpposite()) {
            return new FuelItemHandler(itemHandler);
        } else {
            return new InputItemHandler(itemHandler);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("macerator.progress", progress);
        tag.putInt("macerator.burnTime", burnTime);
        tag.putInt("macerator.maxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("macerator.progress");
        burnTime = tag.getInt("macerator.burnTime");
        maxBurnTime = tag.getInt("macerator.maxBurnTime");
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

    public static void tick(Level level, BlockPos pos, BlockState state, MaceratorBlockEntity blockEntity) {
        boolean wasLit = state.getValue(MaceratorBlock.LIT);
        boolean isLit = false;

        if (!level.isClientSide) {
            boolean hasValidRecipe = blockEntity.hasRecipe();

            if (blockEntity.hasFuel()) {
                blockEntity.burnTime--;
                isLit = true;
            } else if (hasValidRecipe && blockEntity.canBurnFuel()) {
                blockEntity.burnFuel();
                isLit = true;
            } else {
                if (blockEntity.progress > 0) {
                    blockEntity.resetProgress();
                }
            }
            if (hasValidRecipe && blockEntity.hasFuel()) {
                blockEntity.progress++;
                if (blockEntity.progress >= blockEntity.maxProgress) {
                    blockEntity.craftItem();
                    blockEntity.resetProgress();
                }
            } else if (!hasValidRecipe) {
                blockEntity.resetProgress();
            }

            if (wasLit != isLit) {
                level.setBlock(pos, state.setValue(MaceratorBlock.LIT, isLit), 3);
            }
        }
    }

    private boolean hasRecipe() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<MaceratorRecipe> match = level.getRecipeManager()
                .getRecipeFor(MaceratorRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        return match.isPresent();
    }

    private void craftItem() {
        RecipeWrapper inventory = createRecipeInput();
        assert level != null;
        Optional<MaceratorRecipe> match = level.getRecipeManager()
                .getRecipeFor(MaceratorRecipe.Type.INSTANCE, inventory, level).map(RecipeHolder::value);
        if (match.isPresent()) {
            MaceratorRecipe recipe = match.get();
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

        Optional<MaceratorRecipe> currentRecipe = getCurrentRecipe();
        if (currentRecipe.isPresent()) {
            MaceratorRecipe recipe = currentRecipe.get();
            return recipe.matches(inventory, level);
        }
        return false;
    }

    private Optional<MaceratorRecipe> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        RecipeManager recipeManager = level.getRecipeManager();
        RecipeWrapper inventory = createRecipeInput();
        return recipeManager.getAllRecipesFor(MaceratorRecipe.Type.INSTANCE).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.matches(inventory, level))
                .findFirst();
    }

    private boolean canBurnFuel() {
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        return !fuelStack.isEmpty() && fuelStack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    private void burnFuel() {
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        this.burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
        this.maxBurnTime = this.burnTime;
        if (fuelStack.hasCraftingRemainingItem()) {
            itemHandler.setStackInSlot(FUEL_SLOT, fuelStack.getCraftingRemainingItem());
        } else {
            fuelStack.shrink(1);
            if (fuelStack.isEmpty()) {
                itemHandler.setStackInSlot(FUEL_SLOT, ItemStack.EMPTY);
            }
        }
    }

    private boolean hasFuel() {
        return this.burnTime > 0;
    }

    public float getWheelRotation(float partialTicks) {
        assert level != null;
        return (level.getGameTime() + partialTicks) * WHEEL_ROTATION_SPEED % 360;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /////CAPABILITIES
    private class InputItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public InputItemHandler(ItemStackHandler itemHandler) {
            this.itemHandler = itemHandler;
        }

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

    private class FuelItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler itemHandler;

        public FuelItemHandler(ItemStackHandler itemHandler) {
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
            if (slot == FUEL_SLOT && stack.getBurnTime(RecipeType.SMELTING) > 0) {
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
            return slot == FUEL_SLOT && stack.getBurnTime(RecipeType.SMELTING) > 0;
        }
    }

    private class OutputItemHandler implements IItemHandlerModifiable {
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
        public @NotNull ItemStack getStackInSlot(int i) {
            return itemHandler.getStackInSlot(i);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
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