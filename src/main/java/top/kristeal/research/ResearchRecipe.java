package top.kristeal.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Research progression: what researching one blueprint entry costs.
 * <pre>
 * { "type": "scguns:research", "blueprint": "scguns:diamond_steel_blueprint", "entry": "scguns:lockewood",
 *   "materials": [ { "item": "scguns:diamond_steel_ingot", "count": 6 } ], "steps": 10, "time": 4800 }
 * </pre>
 * Research runs in {@code steps} equal steps of {@code time / steps} ticks each; every step takes its share of each
 * material (cumulative rounding, so the last step has used exactly the total). The input is the blueprint stack.
 */
public class ResearchRecipe implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_STEPS = 10;
    /** The Research Station has one material slot per material. */
    public static final int MAX_MATERIALS = 6;

    private final Item blueprint;
    private final Item entry;
    private final List<SizedIngredient> materials;
    private final int steps;
    private final int time;

    public ResearchRecipe(Item blueprint, Item entry, List<SizedIngredient> materials, int steps, int time) {
        this.blueprint = blueprint;
        this.entry = entry;
        this.materials = List.copyOf(materials);
        this.steps = steps;
        this.time = time;
    }

    public Item getBlueprint() {
        return blueprint;
    }

    public Item getEntry() {
        return entry;
    }

    public ResourceLocation getEntryId() {
        return BuiltInRegistries.ITEM.getKey(entry);
    }

    public List<SizedIngredient> getMaterials() {
        return materials;
    }

    public int getSteps() {
        return steps;
    }

    /** Total research time in ticks (before the server's researchTimeMultiplier). */
    public int getTime() {
        return time;
    }

    /** Ticks one step takes (before the server's researchTimeMultiplier); at least 1. */
    public int getStepTime() {
        return Math.max(1, time / steps);
    }

    /**
     * How many of each material (same order as {@link #getMaterials()}) step {@code step} (1-based) takes.
     * Cumulative rounding up: by step k, ceil(count * k / steps) of each material has been used.
     */
    public List<Integer> getStepCounts(int step) {
        List<Integer> counts = new ArrayList<>(materials.size());
        for (int i = 0; i < materials.size(); i++) {
            int total = getTotalCount(i);
            counts.add(usedBy(total, step) - usedBy(total, step - 1));
        }
        return counts;
    }

    /** How many of each material is still needed after {@code doneSteps} finished steps. */
    public List<Integer> getRemainingCounts(int doneSteps) {
        List<Integer> counts = new ArrayList<>(materials.size());
        for (int i = 0; i < materials.size(); i++) {
            int total = getTotalCount(i);
            counts.add(total - usedBy(total, doneSteps));
        }
        return counts;
    }

    /** The whole research's count of material {@code index} on this server (the server's materialCostMultiplier, at least 1). */
    public int getTotalCount(int index) {
        return Math.max(1, (int) Math.ceil(materials.get(index).count() * ResearchHelper.materialCostMultiplier()));
    }

    private int usedBy(int total, int step) {
        if (step <= 0) return 0;
        if (step >= steps) return total;
        return (total * step + steps - 1) / steps;
    }

    @Override
    public boolean matches(SingleRecipeInput input, @NotNull Level level) {
        return input.item().is(blueprint);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, @NotNull HolderLookup.Provider registries) {
        return new ItemStack(entry);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        return new ItemStack(entry);
    }

    /** Not a crafting-book recipe: keeps it out of the recipe book (and its unknown-category warning). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<ResearchRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "research";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<ResearchRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<ResearchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("blueprint").forGetter(ResearchRecipe::getBlueprint),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("entry").forGetter(ResearchRecipe::getEntry),
                SizedIngredient.FLAT_CODEC.listOf(1, MAX_MATERIALS).fieldOf("materials").forGetter(ResearchRecipe::getMaterials),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("steps", DEFAULT_STEPS).forGetter(ResearchRecipe::getSteps),
                ExtraCodecs.POSITIVE_INT.fieldOf("time").forGetter(ResearchRecipe::getTime)
        ).apply(instance, ResearchRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ResearchRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), ResearchRecipe::getBlueprint,
                ByteBufCodecs.registry(Registries.ITEM), ResearchRecipe::getEntry,
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchRecipe::getMaterials,
                ByteBufCodecs.VAR_INT, ResearchRecipe::getSteps,
                ByteBufCodecs.VAR_INT, ResearchRecipe::getTime,
                ResearchRecipe::new);

        @Override
        public @NotNull MapCodec<ResearchRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ResearchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
