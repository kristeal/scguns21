package top.kristeal.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import top.kristeal.Config;
import top.kristeal.init.ModCompat;

public record MachineRecipesCondition() implements ICondition {
    public static final MachineRecipesCondition INSTANCE = new MachineRecipesCondition();
    public static final MapCodec<MachineRecipesCondition> CODEC = MapCodec.unit(INSTANCE);

    /** True if Create is present and the config asks for the machines to be disabled. */
    public static boolean machinesDisabled() {
        return ModCompat.isCreateLoaded() && Config.isDisableMachinesWithCreate();
    }

    @Override
    public boolean test(IContext context) {
        return !machinesDisabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
