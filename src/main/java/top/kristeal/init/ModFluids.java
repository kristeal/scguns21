package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.kristeal.Reference;
import top.kristeal.fluid.ViciousAcidFluid;
import top.kristeal.fluid.ViciousAcidFluidType;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Reference.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Reference.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> VICIOUS_ACID_FLUID_TYPE = FLUID_TYPES.register("vicious_acid",
            () -> new ViciousAcidFluidType(
                    ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/vicious_acid_still"),
                    ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/vicious_acid_flow")
            ));

    public static final DeferredHolder<Fluid, FlowingFluid> VICIOUS_ACID_SOURCE = FLUIDS.register("vicious_acid_source",
            ViciousAcidFluid.Source::new);

    public static final DeferredHolder<Fluid, FlowingFluid> VICIOUS_ACID_FLOWING = FLUIDS.register("vicious_acid_flowing",
            ViciousAcidFluid.Flowing::new);
}
