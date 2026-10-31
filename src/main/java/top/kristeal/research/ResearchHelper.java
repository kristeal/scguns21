package top.kristeal.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import top.kristeal.Config;
import top.kristeal.progression.ProgressionMode;
import top.kristeal.init.ModDataComponents;
import top.kristeal.item.GunItem;

import java.util.List;
import java.util.Optional;

/**
 * Research progression: reads and writes a blueprint's research and looks up research recipes.
 * Works on both sides (research recipes and the server config are synced to clients).
 */
public final class ResearchHelper {
    public enum State {
        /** Below the prototype threshold: nothing can be crafted. */
        LOCKED,
        /** At or above the prototype threshold: a prototype can be crafted. */
        PROTOTYPE,
        /** Fully researched: the real item can be crafted. */
        RESEARCHED
    }

    private ResearchHelper() {}

    /* ---- recipes ---- */

    public static List<RecipeHolder<ResearchRecipe>> getRecipes(RecipeManager recipes) {
        return recipes.getAllRecipesFor(ResearchRecipe.Type.INSTANCE);
    }

    /** All research recipes for the given blueprint item, sorted by entry id. */
    public static List<ResearchRecipe> getRecipesFor(RecipeManager recipes, Item blueprint) {
        return getRecipes(recipes).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.getBlueprint() == blueprint)
                .sorted((a, b) -> a.getEntryId().compareTo(b.getEntryId()))
                .toList();
    }

    public static Optional<ResearchRecipe> find(RecipeManager recipes, Item blueprint, Item entry) {
        return getRecipes(recipes).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.getBlueprint() == blueprint && recipe.getEntry() == entry)
                .findFirst();
    }

    /* ---- blueprint state ---- */

    public static BlueprintResearch getResearch(ItemStack blueprint) {
        return blueprint.getOrDefault(ModDataComponents.RESEARCH.get(), BlueprintResearch.EMPTY);
    }

    public static int getSteps(ItemStack blueprint, Item entry) {
        return getResearch(blueprint).getSteps(BuiltInRegistries.ITEM.getKey(entry));
    }

    /** Sets the finished step count of an entry (clamped to 0..steps); an emptied research removes the component. */
    public static void setSteps(ItemStack blueprint, ResearchRecipe recipe, int steps) {
        ResourceLocation entryId = recipe.getEntryId();
        BlueprintResearch research = getResearch(blueprint).withSteps(entryId, Math.max(0, Math.min(steps, recipe.getSteps())));
        if (research.isEmpty()) {
            blueprint.remove(ModDataComponents.RESEARCH.get());
        } else {
            blueprint.set(ModDataComponents.RESEARCH.get(), research);
        }
    }

    /** Research progress of the recipe's entry on this blueprint, 0.0 to 1.0. */
    public static float getProgress(ItemStack blueprint, ResearchRecipe recipe) {
        return Math.min(1.0F, getSteps(blueprint, recipe.getEntry()) / (float) recipe.getSteps());
    }

    public static State getState(ItemStack blueprint, ResearchRecipe recipe) {
        int steps = getSteps(blueprint, recipe.getEntry());
        if (steps >= recipe.getSteps()) {
            return State.RESEARCHED;
        }
        return steps / (float) recipe.getSteps() >= prototypeThreshold() ? State.PROTOTYPE : State.LOCKED;
    }

    /**
     * What the crafting stations may build for an entry of this blueprint. {@code RESEARCHED} when research is
     * switched off or the entry has no research recipe (nothing to research); {@code PROTOTYPE} only for guns, since
     * other entries (the ExoSuit pieces) have no prototype form and need the full research.
     */
    public static State getCraftState(RecipeManager recipes, ItemStack blueprint, Item entry) {
        if (!isResearchRequired()) {
            return State.RESEARCHED;
        }
        Optional<ResearchRecipe> recipe = find(recipes, blueprint.getItem(), entry);
        if (recipe.isEmpty()) {
            return State.RESEARCHED;
        }
        State state = getState(blueprint, recipe.get());
        boolean prototypeAllowed = entry instanceof GunItem && prototypesEnabled();
        return state == State.PROTOTYPE && !prototypeAllowed ? State.LOCKED : state;
    }

    /* ---- config (safe before the server config has loaded, e.g. tooltips in menus) ---- */

    /** Research gates the Gun Bench only in Research mode (Raid mode builds everything from the blueprint, as the reference did). */
    public static boolean isResearchRequired() {
        if (!ProgressionMode.isResearch()) {
            return false;
        }
        return !Config.isServerConfigLoaded() || Config.SERVER.research.requireResearch.get();
    }

    public static boolean prototypesEnabled() {
        return !Config.isServerConfigLoaded() || Config.SERVER.research.prototypesEnabled.get();
    }

    public static double materialCostMultiplier() {
        return Config.isServerConfigLoaded() ? Config.SERVER.research.materialCostMultiplier.get() : 1.0;
    }

    public static double prototypeThreshold() {
        return Config.isServerConfigLoaded() ? Config.SERVER.research.prototypeThreshold.get() : 0.5;
    }

    public static double prototypeDamageMultiplier() {
        return Config.isServerConfigLoaded() ? Config.SERVER.research.prototypeDamageMultiplier.get() : 0.75;
    }

    /** FE the Research Station uses per tick while researching (0: no power needed). */
    public static int getEnergyPerTick() {
        return Config.isServerConfigLoaded() ? Config.SERVER.research.researchEnergyPerTick.get() : 32;
    }

    /** Ticks one step of this recipe takes on this server. */
    public static int getStepTime(ResearchRecipe recipe) {
        double multiplier = Config.isServerConfigLoaded() ? Config.SERVER.research.researchTimeMultiplier.get() : 1.0;
        return Math.max(1, (int) Math.round(recipe.getStepTime() * multiplier));
    }
}
