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
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Ported to NeoForge 1.21.1 (see MaceratorRecipe for the conversion notes).
 * The optional "mold" field defaults to Ingredient.EMPTY, as the reference's fromJson did.
 */
public class MechanicalPressRecipe implements Recipe<RecipeWrapper> {
    private final NonNullList<Ingredient> inputItems;
    private final Ingredient moldItem;
    private final ItemStack output;
    private final int processingTime;

    public MechanicalPressRecipe(NonNullList<Ingredient> inputItems, Ingredient moldItem, ItemStack output, int processingTime) {
        this.inputItems = inputItems;
        this.moldItem = moldItem;
        this.output = output;
        this.processingTime = processingTime;
    }
    public boolean requiresMold() {
        return !moldItem.isEmpty();
    }
    @Override
    public boolean matches(RecipeWrapper inv, Level world) {
        if (world.isClientSide()) {
            return false;
        }

        NonNullList<Ingredient> requiredIngredients = NonNullList.create();
        requiredIngredients.addAll(inputItems);

        boolean moldMatched = moldItem.isEmpty() || moldItem.test(inv.getItem(3)); // Check mold slot if mold is present

        for (int i = 0; i < inv.size(); i++) {
            if (i == 3) continue; // Skip mold slot
            ItemStack stackInSlot = inv.getItem(i);
            if (!stackInSlot.isEmpty()) {
                boolean matched = false;
                Iterator<Ingredient> iterator = requiredIngredients.iterator();
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    if (ingredient.test(stackInSlot)) {
                        iterator.remove();
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    continue;
                }
            }
        }

        return requiredIngredients.isEmpty() && moldMatched;
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

    public Ingredient getMoldItem() {
        return moldItem;
    }

    public static class Type implements RecipeType<MechanicalPressRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mechanical_pressing";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<MechanicalPressRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<MechanicalPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.inputItems),
                Ingredient.CODEC.optionalFieldOf("mold", Ingredient.EMPTY).forGetter(MechanicalPressRecipe::getMoldItem),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.fieldOf("processingTime").forGetter(MechanicalPressRecipe::getProcessingTime)
        ).apply(instance, (ingredients, moldItem, output, processingTime) ->
                new MechanicalPressRecipe(NonNullList.copyOf(ingredients), moldItem, output, processingTime)));

        private static final StreamCodec<RegistryFriendlyByteBuf, MechanicalPressRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputItems,
                Ingredient.CONTENTS_STREAM_CODEC, MechanicalPressRecipe::getMoldItem,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                ByteBufCodecs.INT, MechanicalPressRecipe::getProcessingTime,
                (ingredients, moldItem, output, processingTime) ->
                        new MechanicalPressRecipe(NonNullList.copyOf(ingredients), moldItem, output, processingTime));

        @Override
        public @NotNull MapCodec<MechanicalPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MechanicalPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
