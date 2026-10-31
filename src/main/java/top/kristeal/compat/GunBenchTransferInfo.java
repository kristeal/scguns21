package top.kristeal.compat;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;
import top.kristeal.client.screen.GunBenchMenu;
import top.kristeal.client.screen.GunBenchRecipe;
import top.kristeal.client.screen.ModMenuTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GunBenchTransferInfo implements IRecipeTransferInfo<GunBenchMenu, GunBenchRecipe> {

    @Override
    public Class<GunBenchMenu> getContainerClass() {
        return GunBenchMenu.class;
    }

    @Override
    public Optional<MenuType<GunBenchMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.GUN_BENCH.get());
    }

    @Override
    public RecipeType<GunBenchRecipe> getRecipeType() {
        return GunBenchCategory.GUN_BENCH_TYPE;
    }

    @Override
    public boolean canHandle(GunBenchMenu container, GunBenchRecipe recipe) {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(GunBenchMenu container, GunBenchRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        for (int i = 0; i < ingredients.size(); i++) {
            if (!ingredients.get(i).isEmpty()) {
                slots.add(container.getSlot(i));
            }
        }

        if (!recipe.getBlueprint().isEmpty()) {
            // Port fix: the reference used container.getSlot(SLOT_BLUEPRINT), i.e. menu slot 11, but the menu adds
            // the blueprint slot at index 10 (and again at 12) and the output at 11, so JEI refused to move the blueprint.
            slots.add(findBlueprintSlot(container));
        }

        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(GunBenchMenu container, GunBenchRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        // Port fix: only the player's inventory. The reference started at menu index 12, which is the second
        // blueprint slot, so JEI could also pull ingredients out of the bench.
        for (Slot slot : container.slots) {
            if (slot.container instanceof Inventory) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static Slot findBlueprintSlot(GunBenchMenu container) {
        for (Slot slot : container.slots) {
            if (!(slot.container instanceof Inventory) && slot.getContainerSlot() == GunBenchMenu.SLOT_BLUEPRINT) {
                return slot;
            }
        }
        throw new IllegalStateException("Gun Bench menu has no blueprint slot");
    }
}