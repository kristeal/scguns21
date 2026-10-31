package top.kristeal.init;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import top.kristeal.Reference;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: 1.21 enchantments are data-driven, so the reference's 16 registered Enchantment
 * classes become ResourceKeys here; their JSON definitions come in the enchantment batch, and until then every level
 * reads 0. The effects stay in gun code, which reads the level through {@link #getLevel}.
 */
public class ModEnchantments
{
    public static final ResourceKey<Enchantment> QUICK_HANDS = key("quick_hands");
    public static final ResourceKey<Enchantment> TRIGGER_FINGER = key("trigger_finger");
    public static final ResourceKey<Enchantment> LIGHTWEIGHT = key("lightweight");
    public static final ResourceKey<Enchantment> COLLATERAL = key("collateral");
    public static final ResourceKey<Enchantment> RECLAIMED = key("reclaimed");
    public static final ResourceKey<Enchantment> ACCELERATOR = key("accelerator");
    public static final ResourceKey<Enchantment> PUNCTURING = key("puncturing");
    public static final ResourceKey<Enchantment> SHELL_CATCHER = key("shell_catcher");
    public static final ResourceKey<Enchantment> BANZAI = key("banzai");
    public static final ResourceKey<Enchantment> HEAVY_SHOT = key("heavy_shot");
    public static final ResourceKey<Enchantment> ELEMENTAL_POP = key("elemental_pop");
    public static final ResourceKey<Enchantment> WATER_PROOF = key("waterproof");
    public static final ResourceKey<Enchantment> HOT_BARREL = key("hot_barrel");
    public static final ResourceKey<Enchantment> GUN_RUST = key("gun_rust");
    public static final ResourceKey<Enchantment> CORRODED = key("corroded");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Reference.id(name));
    }

    /**
     * Replaces 1.20.1 EnchantmentHelper.getItemEnchantmentLevel(Enchantment, ItemStack): the level of the enchantment
     * with this key on the stack, read from the stack's own enchantment holders (no registry access needed).
     */
    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getTagEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static boolean has(ItemStack stack, ResourceKey<Enchantment> key) {
        return getLevel(stack, key) > 0;
    }
}
