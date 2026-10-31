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

import java.util.Iterator;

/**
 * Ported to NeoForge 1.21.1 (see MaceratorRecipe for the conversion notes).
 * Note: as in the reference, PoweredMaceratorBlockEntity processes MaceratorRecipe, not this type.
 */
public class PoweredMaceratorRecipe implements Recipe<RecipeWrapper> {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final int processingTime;
    private final int energyUse; // New field for energy use

    public PoweredMaceratorRecipe(NonNullList<Ingredient> inputItems, ItemStack output, int processingTime, int energyUse) {
        this.inputItems = inputItems;
        this.output = output;
        this.processingTime = processingTime;
        this.energyUse = energyUse; // Initialize the new field
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

    public int getEnergyUse() {
        return energyUse; // Getter for the new field
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

    public static class Type implements RecipeType<PoweredMaceratorRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "powered_macerating";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<PoweredMaceratorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<PoweredMaceratorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.inputItems),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(PoweredMaceratorRecipe::getProcessingTime),
                Codec.INT.optionalFieldOf("energyUse", 1000).forGetter(PoweredMaceratorRecipe::getEnergyUse) // Deserialize the energy use
        ).apply(instance, (ingredients, output, processingTime, energyUse) ->
                new PoweredMaceratorRecipe(NonNullList.copyOf(ingredients), output, processingTime, energyUse)));

        private static final StreamCodec<RegistryFriendlyByteBuf, PoweredMaceratorRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputItems,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                ByteBufCodecs.INT, PoweredMaceratorRecipe::getProcessingTime,
                ByteBufCodecs.INT, PoweredMaceratorRecipe::getEnergyUse, // Serialize the energy use
                (ingredients, output, processingTime, energyUse) ->
                        new PoweredMaceratorRecipe(NonNullList.copyOf(ingredients), output, processingTime, energyUse));

        @Override
        public @NotNull MapCodec<PoweredMaceratorRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PoweredMaceratorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
