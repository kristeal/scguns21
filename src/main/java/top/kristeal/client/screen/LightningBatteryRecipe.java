package top.kristeal.client.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ported to NeoForge 1.21.1: the recipe id now lives in RecipeHolder, the input is a SingleRecipeInput (the reference
 * wrapped the one input stack in a SimpleContainer), and serialization uses codecs. As in the reference's fromJson,
 * only the first entry of "ingredients" is used, and the energy cost is read from "requiredEnergy".
 */
public class LightningBatteryRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack output;
    private final int processingTime;
    private final int energyUse;

    public LightningBatteryRecipe(Ingredient input, ItemStack output, int processingTime, int energyUse) {
        this.input = input;
        this.output = output;
        this.processingTime = processingTime;
        this.energyUse = energyUse;
    }

    @Override
    public boolean matches(SingleRecipeInput container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SingleRecipeInput container, HolderLookup.Provider registries) {
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

    public Ingredient getInput() {
        return input;
    }

    public int getEnergyUse() {
        return energyUse;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<LightningBatteryRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "lightning_battery";

        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<LightningBatteryRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        // "ingredients" is a list in the JSON; the reference read only element 0
        private static final Codec<Ingredient> FIRST_INGREDIENT = Ingredient.CODEC_NONEMPTY.listOf().flatXmap(
                list -> list.isEmpty() ? DataResult.error(() -> "lightning_battery recipe needs at least one ingredient") : DataResult.success(list.get(0)),
                ingredient -> DataResult.success(List.of(ingredient)));

        private static final MapCodec<LightningBatteryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FIRST_INGREDIENT.fieldOf("ingredients").forGetter(LightningBatteryRecipe::getInput),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.fieldOf("processingTime").forGetter(LightningBatteryRecipe::getProcessingTime),
                Codec.INT.fieldOf("requiredEnergy").forGetter(LightningBatteryRecipe::getEnergyUse)
        ).apply(instance, LightningBatteryRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, LightningBatteryRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, LightningBatteryRecipe::getInput,
                ItemStack.STREAM_CODEC, recipe -> recipe.output,
                ByteBufCodecs.INT, LightningBatteryRecipe::getProcessingTime,
                ByteBufCodecs.INT, LightningBatteryRecipe::getEnergyUse,
                LightningBatteryRecipe::new);

        @Override
        public @NotNull MapCodec<LightningBatteryRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, LightningBatteryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
