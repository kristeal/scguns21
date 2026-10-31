package top.kristeal.client.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.blockentity.ResearchStationBlockEntity;
import top.kristeal.init.ModBlocks;
import top.kristeal.item.BlueprintItem;
import top.kristeal.research.ResearchHelper;
import top.kristeal.research.ResearchRecipe;

import java.util.List;

/**
 * Research progression: the Research Station menu. Selecting an entry and starting/stopping use
 * vanilla menu button clicks ({@link #clickMenuButton}), so no custom packet is needed.
 */
public class ResearchStationMenu extends AbstractContainerMenu {
    /** Button id that starts or stops the research. */
    public static final int BUTTON_TOGGLE = 0;
    /** Button ids from this value select entry (id - BUTTON_SELECT). */
    public static final int BUTTON_SELECT = 100;

    public static final int BLUEPRINT_X = 173, BLUEPRINT_Y = 81;
    public static final int MATERIALS_X = 36, MATERIALS_Y = 81, MATERIAL_SPACING = 19;
    public static final int INVENTORY_X = 34, INVENTORY_Y = 140;

    private static final int STATION_SLOTS = ResearchStationBlockEntity.SLOT_COUNT;
    private static final int PLAYER_SLOTS = 36;

    public final ResearchStationBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public ResearchStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(id, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(ResearchStationBlockEntity.DATA_COUNT));
    }

    public ResearchStationMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESEARCH_STATION.get(), id);
        checkContainerDataCount(data, ResearchStationBlockEntity.DATA_COUNT);
        this.blockEntity = (ResearchStationBlockEntity) entity;
        this.data = data;
        this.access = ContainerLevelAccess.create(inventory.player.level(), entity.getBlockPos());

        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, ResearchStationBlockEntity.BLUEPRINT_SLOT, BLUEPRINT_X, BLUEPRINT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof BlueprintItem;
            }
        });
        for (int m = 0; m < ResearchStationBlockEntity.MATERIAL_SLOTS; m++) {
            int material = m;
            this.addSlot(new SlotItemHandler(blockEntity.itemHandler, ResearchStationBlockEntity.FIRST_MATERIAL_SLOT + m,
                    MATERIALS_X + m * MATERIAL_SPACING, MATERIALS_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // the menu's synced selection, so the client predicts the same result as the server
                    return ResearchStationBlockEntity.accepts(getSelectedRecipe(), material, stack);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }

        addDataSlots(data);
    }

    public ItemStack getBlueprint() {
        return blockEntity.itemHandler.getStackInSlot(ResearchStationBlockEntity.BLUEPRINT_SLOT);
    }

    public int getProgress() {
        return data.get(ResearchStationBlockEntity.DATA_PROGRESS);
    }

    public int getStepTime() {
        return data.get(ResearchStationBlockEntity.DATA_STEP_TIME);
    }

    public boolean isRunning() {
        return data.get(ResearchStationBlockEntity.DATA_RUNNING) != 0;
    }

    /** Stored FE. */
    public int getEnergy() {
        return data.get(ResearchStationBlockEntity.DATA_ENERGY);
    }

    public int getSelectedIndex() {
        return data.get(ResearchStationBlockEntity.DATA_SELECTED);
    }

    /** The research entries of the blueprint in the slot, in list order. */
    public List<ResearchRecipe> getEntries() {
        Level level = blockEntity.getLevel();
        ItemStack blueprint = getBlueprint();
        if (level == null || blueprint.isEmpty()) {
            return List.of();
        }
        return ResearchHelper.getRecipesFor(level.getRecipeManager(), blueprint.getItem());
    }

    @Nullable
    public ResearchRecipe getSelectedRecipe() {
        List<ResearchRecipe> entries = getEntries();
        int index = getSelectedIndex();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    /** The stack in material slot {@code material}. */
    public ItemStack getMaterialStack(int material) {
        return blockEntity.itemHandler.getStackInSlot(ResearchStationBlockEntity.FIRST_MATERIAL_SLOT + material);
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_TOGGLE) {
            blockEntity.toggleRunning();
            return true;
        }
        if (id >= BUTTON_SELECT) {
            blockEntity.select(id - BUTTON_SELECT);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < STATION_SLOTS) {
            if (!this.moveItemStackTo(stack, STATION_SLOTS, STATION_SLOTS + PLAYER_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof BlueprintItem && !this.slots.get(0).hasItem()) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 1, STATION_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.RESEARCH_STATION.get());
    }
}
