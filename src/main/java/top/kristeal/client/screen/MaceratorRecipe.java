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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * Ported to NeoForge 1.21.1: the recipe id now lives in RecipeHolder, the input is a RecipeWrapper over the
 * machine's input slots (replacing the copied SimpleContainer), and JSON/network serialization uses codecs.
 */
public class MaceratorRecipe implements Recipe<RecipeWrapper> {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final int processingTime;

    public MaceratorRecipe(NonNullList<Ingredient> inputItems, ItemStack output, int processingTime) {
        this.inputItems = inputItems;
        this.output = output;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level world) {
        if (world.isClientSide()) {
            return false;
        }

        NonNullList<Ingredient> requiredIngredients = NonNullList.create();
        requiredIngredients.addAll(inputItems);

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stackInSlot = inv.getItem(i);
            if (!stackInSlot.isEmpty()) {
                boolean matched = false;
                for (Ingredient ingredient : requiredIngredients) {
                    if (ingredient.test(stackInSlot)) {
                        requiredIngredients.remove(ingredient);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }

        return requiredIngredients.isEmpty();
    }

    @Override
    public ItemStack assemble(RecipeWrapper inv, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    public int getProcessingTime() {
        return processingTime;
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
        return inputItems;
    }

    public static class Type implements RecipeType<MaceratorRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "macerating";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<MaceratorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<MaceratorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.inputItems),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.fieldOf("processingTime").forGetter(MaceratorRecipe::getProcessingTime)
        ).apply(instance, (ingredients, output, processingTime) ->
                new MaceratorRecipe(NonNullList.copyOf(ingredients), output, processingTime)));

        private static final StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputItems,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                ByteBufCodecs.INT, MaceratorRecipe::getProcessingTime,
                (ingredients, output, processingTime) -> new MaceratorRecipe(NonNullList.copyOf(ingredients), output, processingTime));

        @Override
        public @NotNull MapCodec<MaceratorRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
