package top.kristeal.blockentity;

import top.kristeal.progression.ProgressionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.client.screen.ResearchStationMenu;
import top.kristeal.init.ModBlockEntities;
import top.kristeal.item.BlueprintItem;
import top.kristeal.research.ResearchHelper;
import top.kristeal.research.ResearchRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Research progression: the Research Station. Slot 0 holds a blueprint; slots 1-6 are material slots,
 * slot 1 + m taking only material m of the selected entry. While running, the selected entry advances one step at a
 * time: a step takes {@link ResearchHelper#getStepTime} ticks while every material slot holds that step's share, which
 * is taken when the step finishes; the finished step is written to the blueprint at once. Hoppers and other automation
 * insert materials only.
 */
public class ResearchStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int FIRST_MATERIAL_SLOT = 1;
    public static final int MATERIAL_SLOTS = ResearchRecipe.MAX_MATERIALS;
    public static final int SLOT_COUNT = FIRST_MATERIAL_SLOT + MATERIAL_SLOTS;

    /** Menu data slots: step progress, step time, running flag, selected entry index (-1 = none). */
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_STEP_TIME = 1;
    public static final int DATA_RUNNING = 2;
    public static final int DATA_SELECTED = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_COUNT = 5;

    /**
     * Stored FE. Kept at or below 32,767 because menu data is synced to the client as 16-bit values. Up to 1,000 FE/t
     * can be inserted from any side; nothing can extract it.
     */
    public static final int ENERGY_CAPACITY = 32_000;
    public static final int ENERGY_MAX_RECEIVE = 1_000;

    public final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == BLUEPRINT_SLOT && !writingResearch && level != null && !level.isClientSide()) {
                onBlueprintChanged();
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == BLUEPRINT_SLOT ? stack.getItem() instanceof BlueprintItem
                    : accepts(getSelectedRecipe(), slot - FIRST_MATERIAL_SLOT, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == BLUEPRINT_SLOT ? 1 : super.getSlotLimit(slot);
        }
    };

    /** What automation sees: the material buffer, insert only. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return MATERIAL_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot + FIRST_MATERIAL_SLOT);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot + FIRST_MATERIAL_SLOT, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot + FIRST_MATERIAL_SLOT);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return itemHandler.isItemValid(slot + FIRST_MATERIAL_SLOT, stack);
        }
    };

    private final StationEnergy energy = new StationEnergy();

    /** Receive-only storage; the station takes its own power with {@link StationEnergy#use}. */
    private class StationEnergy extends EnergyStorage {
        StationEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        boolean use(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }

    /** True while the station writes research to its own blueprint, so that write doesn't reset the selection. */
    private boolean writingResearch;
    /** Items from slots an older save had (the 3x3 buffer); dropped on the next tick. */
    private final List<ItemStack> leftovers = new ArrayList<>();
    @Nullable
    private ResourceLocation selectedEntry;
    private boolean running;
    private int progress;
    private int stepTime;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_STEP_TIME -> stepTime;
                case DATA_RUNNING -> running ? 1 : 0;
                case DATA_SELECTED -> getSelectedIndex();
                case DATA_ENERGY -> energy.getEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // server-authoritative; the client copy lives in the menu's SimpleContainerData
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_STATION.get(), pos, state);
    }

    /* ---- menu ---- */

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.scguns.research_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new ResearchStationMenu(id, inventory, this, data);
    }

    /** Registered in ModCapabilities (Capabilities.ItemHandler.BLOCK); every side gets the insert-only buffer. */
    /** Registered in ModCapabilities (Capabilities.EnergyStorage.BLOCK): every side accepts FE. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return automationHandler;
    }

    /** True if {@code stack} can go into material slot {@code material} while {@code recipe} is selected. */
    public static boolean accepts(@Nullable ResearchRecipe recipe, int material, ItemStack stack) {
        return recipe != null && material >= 0 && material < recipe.getMaterials().size()
                && recipe.getMaterials().get(material).ingredient().test(stack);
    }

    /* ---- selection and control (called from the menu's button clicks, server side) ---- */

    private List<ResearchRecipe> getEntries() {
        ItemStack blueprint = itemHandler.getStackInSlot(BLUEPRINT_SLOT);
        if (level == null || blueprint.isEmpty()) {
            return List.of();
        }
        return ResearchHelper.getRecipesFor(level.getRecipeManager(), blueprint.getItem());
    }

    private int getSelectedIndex() {
        if (selectedEntry == null) {
            return -1;
        }
        List<ResearchRecipe> entries = getEntries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getEntryId().equals(selectedEntry)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private ResearchRecipe getSelectedRecipe() {
        int index = getSelectedIndex();
        return index < 0 ? null : getEntries().get(index);
    }

    public void select(int index) {
        List<ResearchRecipe> entries = getEntries();
        if (index < 0 || index >= entries.size()) {
            return;
        }
        ResourceLocation entry = entries.get(index).getEntryId();
        if (!entry.equals(selectedEntry)) {
            selectedEntry = entry;
            running = false;
            progress = 0;
            setChanged();
        }
    }

    public void toggleRunning() {
        ResearchRecipe recipe = getSelectedRecipe();
        running = !running && recipe != null && !isComplete(recipe);
        setChanged();
    }

    private boolean isComplete(ResearchRecipe recipe) {
        return ResearchHelper.getSteps(itemHandler.getStackInSlot(BLUEPRINT_SLOT), recipe.getEntry()) >= recipe.getSteps();
    }

    /** A new or removed blueprint stops the research; the selection follows the blueprint's active recipe if it has one. */
    private void onBlueprintChanged() {
        running = false;
        progress = 0;
        selectedEntry = null;
        ItemStack blueprint = itemHandler.getStackInSlot(BLUEPRINT_SLOT);
        ResourceLocation activeRecipe = blueprint.isEmpty() ? null : BlueprintItem.getActiveRecipe(blueprint);
        if (activeRecipe != null && level != null) {
            RecipeManager recipes = level.getRecipeManager();
            recipes.byKey(activeRecipe).ifPresent(holder -> {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                ResearchHelper.find(recipes, blueprint.getItem(), result.getItem())
                        .ifPresent(recipe -> selectedEntry = recipe.getEntryId());
            });
        }
    }

    /* ---- ticking ---- */

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchStationBlockEntity station) {
        if (level.isClientSide() || !ProgressionMode.isResearch()) {
            return; // Raid mode: the station is idle
        }
        if (!station.leftovers.isEmpty()) {
            station.leftovers.forEach(stack -> Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), stack));
            station.leftovers.clear();
        }
        ResearchRecipe recipe = station.getSelectedRecipe();
        station.stepTime = recipe == null ? 0 : ResearchHelper.getStepTime(recipe);
        if (!station.running) {
            return;
        }
        if (recipe == null || station.isComplete(recipe)) {
            station.running = false;
            station.progress = 0;
            station.setChanged();
            return;
        }

        ItemStack blueprint = station.itemHandler.getStackInSlot(BLUEPRINT_SLOT);
        int nextStep = ResearchHelper.getSteps(blueprint, recipe.getEntry()) + 1;
        if (!station.consumeMaterials(recipe, nextStep, true)) {
            return; // paused until the materials are there
        }
        int energyCost = ResearchHelper.getEnergyPerTick();
        if (energyCost > 0 && !station.energy.use(energyCost)) {
            return; // paused until there is power
        }

        station.progress++;
        if (station.progress >= station.stepTime) {
            station.consumeMaterials(recipe, nextStep, false);
            ItemStack updated = blueprint.copy();
            ResearchHelper.setSteps(updated, recipe, nextStep);
            station.writingResearch = true;
            station.itemHandler.setStackInSlot(BLUEPRINT_SLOT, updated);
            station.writingResearch = false;
            station.progress = 0;
            if (nextStep >= recipe.getSteps()) {
                station.running = false;
            }
        }
        station.setChanged();
    }

    /** Checks (simulate) or takes the materials of one step, each from its own material slot. */
    private boolean consumeMaterials(ResearchRecipe recipe, int step, boolean simulate) {
        List<SizedIngredient> materials = recipe.getMaterials();
        List<Integer> counts = recipe.getStepCounts(step);
        for (int m = 0; m < materials.size(); m++) {
            ItemStack stack = itemHandler.getStackInSlot(FIRST_MATERIAL_SLOT + m);
            int needed = counts.get(m);
            if (needed > 0 && (stack.getCount() < needed || !materials.get(m).ingredient().test(stack))) {
                return false;
            }
        }
        if (!simulate) {
            for (int m = 0; m < materials.size(); m++) {
                itemHandler.extractItem(FIRST_MATERIAL_SLOT + m, counts.get(m), false);
            }
        }
        return true;
    }

    /* ---- saving ---- */

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (selectedEntry != null) {
            tag.putString("selected", selectedEntry.toString());
        }
        tag.putBoolean("running", running);
        tag.putInt("progress", progress);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (itemHandler.getSlots() != SLOT_COUNT) {
            // a save from before the 6 material slots: keep the blueprint, drop everything else
            List<ItemStack> saved = new ArrayList<>();
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                saved.add(itemHandler.getStackInSlot(i));
            }
            itemHandler.setSize(SLOT_COUNT);
            if (!saved.isEmpty()) {
                itemHandler.setStackInSlot(BLUEPRINT_SLOT, saved.getFirst());
            }
            saved.stream().skip(1).filter(stack -> !stack.isEmpty()).forEach(leftovers::add);
        }
        selectedEntry = tag.contains("selected") ? ResourceLocation.tryParse(tag.getString("selected")) : null;
        running = tag.getBoolean("running");
        progress = tag.getInt("progress");
        energy.set(tag.getInt("energy"));
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (level != null) {
            Containers.dropContents(level, worldPosition, inventory);
        }
    }
}
