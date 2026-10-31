package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;
import top.kristeal.client.screen.*;
import top.kristeal.research.ResearchRecipe;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Reference.MOD_ID);

    /*
     * Port note: the reference kept each RecipeType as an unregistered static Type.INSTANCE.
     * In 1.21 recipe types are registry entries, so the same instances are registered under the same ids.
     */
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MechanicalPressRecipe>> MECHANICAL_PRESS_SERIALIZER =
            SERIALIZERS.register("mechanical_pressing", () -> MechanicalPressRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PoweredMechanicalPressRecipe>> POWERED_MECHANICAL_PRESS_SERIALIZER =
            SERIALIZERS.register("powered_mechanical_pressing", () -> PoweredMechanicalPressRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaceratorRecipe>> MACERATOR_SERIALIZER =
            SERIALIZERS.register("macerating", () -> MaceratorRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PoweredMaceratorRecipe>> POWERED_MACERATOR_SERIALIZER =
            SERIALIZERS.register("powered_macerating", () -> PoweredMaceratorRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GunBenchRecipe>> GUN_BENCH_SERIALIZER =
            SERIALIZERS.register("gun_bench", () -> GunBenchRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LightningBatteryRecipe>> LIGHTNING_BATTERY_SERIALIZER =
            SERIALIZERS.register("lightning_battery", () -> LightningBatteryRecipe.Serializer.INSTANCE);

    // Research progression
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResearchRecipe>> RESEARCH_SERIALIZER =
            SERIALIZERS.register("research", () -> ResearchRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MechanicalPressRecipe>> MECHANICAL_PRESS_TYPE =
            TYPES.register(MechanicalPressRecipe.Type.ID, () -> MechanicalPressRecipe.Type.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PoweredMechanicalPressRecipe>> POWERED_MECHANICAL_PRESS_TYPE =
            TYPES.register(PoweredMechanicalPressRecipe.Type.ID, () -> PoweredMechanicalPressRecipe.Type.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MaceratorRecipe>> MACERATOR_TYPE =
            TYPES.register(MaceratorRecipe.Type.ID, () -> MaceratorRecipe.Type.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PoweredMaceratorRecipe>> POWERED_MACERATOR_TYPE =
            TYPES.register(PoweredMaceratorRecipe.Type.ID, () -> PoweredMaceratorRecipe.Type.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<LightningBatteryRecipe>> LIGHTNING_BATTERY_TYPE =
            TYPES.register(LightningBatteryRecipe.Type.ID, () -> LightningBatteryRecipe.Type.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GunBenchRecipe>> GUN_BENCH_TYPE =
            TYPES.register(GunBenchRecipe.Type.ID, () -> GunBenchRecipe.Type.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ResearchRecipe>> RESEARCH_TYPE =
            TYPES.register(ResearchRecipe.Type.ID, () -> ResearchRecipe.Type.INSTANCE);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
