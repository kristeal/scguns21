package top.kristeal.compat;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.init.ModTiers;
import vectorwing.farmersdelight.common.item.KnifeItem;

/**
 * Holds every direct reference to Farmer's Delight classes. Only called when FD is loaded, so
 * KnifeItem is never class-loaded without it. Replaces the reference's reflective constructor lookup in ModItems.
 */
public class FarmersDelightItems {
    public static DeferredItem<Item> registerAnthraliteKnife(DeferredRegister.Items register) {
        // Same attack values as the reference (0.5F, -2.0F); FD 1.21 passes them as attributes instead of constructor args
        return register.register("anthralite_knife", () -> new KnifeItem(ModTiers.ANTHRALITE,
                new Item.Properties().attributes(KnifeItem.createAttributes(ModTiers.ANTHRALITE, 0.5F, -2.0F))));
    }
}
