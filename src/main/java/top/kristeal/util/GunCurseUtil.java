package top.kristeal.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.kristeal.init.ModEnchantments;
import top.kristeal.item.GunItem;

/**
 * Port notes: 1.21 enchantments are registry holders, so applying Gun Rust needs a registry lookup; the
 * caller passes one (the reference took only the stack and the random source). removeCurse clears the enchantments
 * component, as the reference removed the whole "Enchantments" tag.
 */
public class GunCurseUtil {
    private static final float MOB_GUN_CURSE_CHANCE = 0.85f;

    public static ItemStack applyCurseIfRoll(ItemStack stack, RandomSource random, HolderLookup.Provider registries) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return stack;
        }

        if (random.nextFloat() < MOB_GUN_CURSE_CHANCE) {
            registries.lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.GUN_RUST)
                    .ifPresent(gunRust -> stack.enchant(gunRust, 1));
        }

        return stack;
    }

    public static boolean isCursed(ItemStack stack) {
        return ModEnchantments.getLevel(stack, ModEnchantments.GUN_RUST) > 0;
    }

    public static void removeCurse(ItemStack stack) {
        if (isCursed(stack)) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
    }
}
