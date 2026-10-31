package top.kristeal.client.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import top.kristeal.common.GunStackData;
import top.kristeal.research.ResearchHelper;

import java.util.List;

/**
 * Ported to NeoForge 1.21.1: the recipe id now lives in RecipeHolder, the input is a RecipeWrapper over the bench's
 * 12-slot container (the reference copied it into a SimpleContainer holding the same stacks), and serialization uses
 * codecs. The "ingredients" object keeps the reference's 10 named part keys (slot order below) plus "blueprint".
 */
public class GunBenchRecipe implements Recipe<RecipeWrapper> {
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final Ingredient blueprint;

    public GunBenchRecipe(ItemStack output, NonNullList<Ingredient> recipeItems, Ingredient blueprint) {
        this.output = output;
        this.recipeItems = recipeItems;
        this.blueprint = blueprint;
    }
    @Override
    public boolean matches(RecipeWrapper container, Level level) {
        // Check if the blueprint matches
        ItemStack blueprintStack = container.getItem(GunBenchMenu.SLOT_BLUEPRINT);
        if (!blueprint.test(blueprintStack)) {
            return false;
        }
        for (int i = 0; i < recipeItems.size(); i++) {
            ItemStack stackInSlot = container.getItem(i);
            Ingredient requiredIngredient = recipeItems.get(i);
            if (!requiredIngredient.isEmpty() && !requiredIngredient.test(stackInSlot)) {
                return false;
            } else if (requiredIngredient.isEmpty() && !stackInSlot.isEmpty()) {
                return false;
            }
        }
        // Research progression: the entry must be researched far enough
        return ResearchHelper.getCraftState(level.getRecipeManager(), blueprintStack, output.getItem()) != ResearchHelper.State.LOCKED;
    }

    /**
     * Research progression: the bench's result for the blueprint in the input. The real item at full
     * research, a prototype (same item with the {@code scguns:prototype} component) at the prototype stage.
     */
    public ItemStack assembleResearched(RecipeWrapper container, Level level) {
        ItemStack result = assemble(container, level.registryAccess());
        ItemStack blueprintStack = container.getItem(GunBenchMenu.SLOT_BLUEPRINT);
        if (ResearchHelper.getCraftState(level.getRecipeManager(), blueprintStack, result.getItem()) == ResearchHelper.State.PROTOTYPE) {
            GunStackData.setPrototype(result, true);
        }
        return result;
    }


    @Override
    public ItemStack assemble(RecipeWrapper container, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    public Ingredient getBlueprint() {
        return blueprint;
    }

    public static class Type implements RecipeType<GunBenchRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "gun_bench";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<GunBenchRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        /** JSON keys of the 10 part slots, in slot order (index 0-9), as read by the reference fromJson. */
        private static final String[] SLOT_KEYS = {
                "gun_top_internal_1", "gun_top_internal_2", "gun_top_barrel_1", "gun_top_barrel_2",
                "gun_internal_1", "gun_internal_2", "gun_barrel_1", "gun_barrel_2", "gun_grip", "gun_magazine"};

        private static MapCodec<Ingredient> slot(int index) {
            return Ingredient.CODEC.optionalFieldOf(SLOT_KEYS[index], Ingredient.EMPTY);
        }

        private record Parts(NonNullList<Ingredient> inputs, Ingredient blueprint) {
            private Ingredient get(int i) {
                return inputs.get(i);
            }
        }

        private static final Codec<Parts> INGREDIENTS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                slot(0).forGetter(p -> p.get(0)), slot(1).forGetter(p -> p.get(1)), slot(2).forGetter(p -> p.get(2)),
                slot(3).forGetter(p -> p.get(3)), slot(4).forGetter(p -> p.get(4)), slot(5).forGetter(p -> p.get(5)),
                slot(6).forGetter(p -> p.get(6)), slot(7).forGetter(p -> p.get(7)), slot(8).forGetter(p -> p.get(8)),
                slot(9).forGetter(p -> p.get(9)),
                Ingredient.CODEC.optionalFieldOf("blueprint", Ingredient.EMPTY).forGetter(Parts::blueprint)
        ).apply(instance, (i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, blueprint) ->
                new Parts(NonNullList.of(Ingredient.EMPTY, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9), blueprint)));

        private static final MapCodec<GunBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(recipe -> new Parts(recipe.recipeItems, recipe.blueprint))
        ).apply(instance, (output, parts) -> new GunBenchRecipe(output, parts.inputs(), parts.blueprint())));

        private static final StreamCodec<RegistryFriendlyByteBuf, GunBenchRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.recipeItems,
                Ingredient.CONTENTS_STREAM_CODEC, GunBenchRecipe::getBlueprint,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                (inputs, blueprint, output) -> new GunBenchRecipe(output, NonNullList.copyOf(inputs), blueprint));

        @Override
        public @NotNull MapCodec<GunBenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, GunBenchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
