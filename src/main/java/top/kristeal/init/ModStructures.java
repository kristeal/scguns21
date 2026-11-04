package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;
import top.kristeal.world.CogChambers;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<CogChambers>> CHAMBER =
            REGISTRY.register("cogchambers", () -> () -> CogChambers.CODEC);

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }
}
