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

import static top.kristeal.blockentity.PoweredMechanicalPressBlockEntity.MOLD_SLOT;

/**
 * Ported to NeoForge 1.21.1 (see MaceratorRecipe for the conversion notes).
 * The optional "mold" field defaults to Ingredient.EMPTY, as the reference's fromJson did.
 */
public class PoweredMechanicalPressRecipe implements Recipe<RecipeWrapper> {
    private final NonNullList<Ingredient> inputItems;
    private final Ingredient moldItem;
    private final ItemStack output;
    private final int processingTime;
    private final int energyUse; // New field for energy use

    public PoweredMechanicalPressRecipe(NonNullList<Ingredient> inputItems, Ingredient moldItem, ItemStack output, int processingTime, int energyUse) {
        this.inputItems = inputItems;
        this.moldItem = moldItem;
        this.output = output;
        this.processingTime = processingTime;
        this.energyUse = energyUse; // Initialize the new field
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

        boolean moldMatched = moldItem.isEmpty() || moldItem.test(inv.getItem(MOLD_SLOT)); // Check mold slot if mold is present

        for (int i = 0; i < inv.size(); i++) {
            if (i == MOLD_SLOT) continue; // Skip mold slot
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

    public Ingredient getMoldItem() {
        return moldItem;
    }

    public static class Type implements RecipeType<PoweredMechanicalPressRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "powered_mechanical_pressing";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<PoweredMechanicalPressRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<PoweredMechanicalPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.inputItems),
                Ingredient.CODEC.optionalFieldOf("mold", Ingredient.EMPTY).forGetter(PoweredMechanicalPressRecipe::getMoldItem),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(PoweredMechanicalPressRecipe::getProcessingTime),
                Codec.INT.optionalFieldOf("energyUse", 1000).forGetter(PoweredMechanicalPressRecipe::getEnergyUse) // Deserialize the energy use
        ).apply(instance, (ingredients, moldItem, output, processingTime, energyUse) ->
                new PoweredMechanicalPressRecipe(NonNullList.copyOf(ingredients), moldItem, output, processingTime, energyUse)));

        private static final StreamCodec<RegistryFriendlyByteBuf, PoweredMechanicalPressRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputItems,
                Ingredient.CONTENTS_STREAM_CODEC, PoweredMechanicalPressRecipe::getMoldItem,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                ByteBufCodecs.INT, PoweredMechanicalPressRecipe::getProcessingTime,
                ByteBufCodecs.INT, PoweredMechanicalPressRecipe::getEnergyUse, // Serialize the energy use
                (ingredients, moldItem, output, processingTime, energyUse) ->
                        new PoweredMechanicalPressRecipe(NonNullList.copyOf(ingredients), moldItem, output, processingTime, energyUse));

        @Override
        public @NotNull MapCodec<PoweredMechanicalPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PoweredMechanicalPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
