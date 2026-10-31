package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;
import top.kristeal.world.NiterPatchConfiguration;
import top.kristeal.world.NiterPatchFeature;
import top.kristeal.world.VentFeature;
import top.kristeal.world.VentFeatureConfiguration;

// The reference annotated this class with @Mod.EventBusSubscriber but it has no @SubscribeEvent methods,
// which NeoForge rejects at registration, so it is registered from ScorchedGuns instead.
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Reference.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<VentFeatureConfiguration>> VENT_FEATURE = FEATURES.register("vent",
            () -> new VentFeature(VentFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NiterPatchConfiguration>> NITER_PATCH = FEATURES.register("niter_patch",
            () -> new NiterPatchFeature(NiterPatchConfiguration.CODEC));


    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
