package top.kristeal.init;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Reference.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SCRAP = register("scrap",
            new int[]{5, 6, 6, 4}, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 0.05F,
            () -> Ingredient.of(Items.COPPER_INGOT));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> REDCOAT = register("redcoat",
            new int[]{2, 4, 3, 2}, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(Items.LEATHER));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADRIEN = register("adrien",
            new int[]{3, 6, 6, 4}, 8, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.1F,
            () -> Ingredient.of(ModItems.TREATED_IRON_INGOT.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ANTHRALITE = register("anthralite",
            new int[]{3, 5, 4, 3}, 12, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.05F,
            () -> Ingredient.of(ModItems.ANTHRALITE_INGOT.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DIAMOND_STEEL = register("diamond_steel",
            new int[]{3, 7, 5, 3}, 16, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.05F,
            () -> Ingredient.of(ModItems.DIAMOND_STEEL_INGOT.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TREATED_BRASS = register("treated_brass",
            new int[]{4, 5, 5, 4}, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.2F,
            () -> Ingredient.of(ModItems.TREATED_BRASS_INGOT.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ANCIENT_BRASS = register("ancient_brass",
            new int[]{3, 5, 4, 3}, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.15F,
            () -> Ingredient.of(ModItems.ANCIENT_BRASS.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXO_SUIT = register("exo_suit",
            new int[]{1, 1, 1, 1}, 6, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.0F, 0.0F,
            () -> Ingredient.of(ModItems.TREATED_IRON_INGOT.get()));

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, int[] defense,
                                                                         int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockback,
                                                                         Supplier<Ingredient> repairIngredient) {
        return MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.HELMET, defense[0]); // Fix: the reference arrays are helmet, chestplate, leggings, boots
                    map.put(ArmorItem.Type.CHESTPLATE, defense[1]);
                    map.put(ArmorItem.Type.LEGGINGS, defense[2]);
                    map.put(ArmorItem.Type.BOOTS, defense[3]);
                    map.put(ArmorItem.Type.BODY, defense[1]); // animal armor slot — rare; value rarely matters
                }),
                enchantability,
                equipSound,
                repairIngredient,
                List.of(),          // vanilla texture layers — GeckoLib renders these, so empty
                toughness,
                knockback
        ));

    }

    /*
     * Fix: 1.21 keeps armour durability on the item, not the material, and the port never set it, so mod armour
     * didn't wear down. These are the reference's numbers: BASE_DURABILITY[slot] * the material's multiplier
     * (the reference's 1.20.1 ModArmorMaterials enum: durability = BASE_DURABILITY[slot] * multiplier, where the slots are
     * helmet 8, chestplate 12, leggings 12, boots 9).
     */
    private static final java.util.Map<ArmorItem.Type, Integer> BASE_DURABILITY = java.util.Map.of(
            ArmorItem.Type.HELMET, 8, ArmorItem.Type.CHESTPLATE, 12, ArmorItem.Type.LEGGINGS, 12, ArmorItem.Type.BOOTS, 9);
    private static final java.util.Map<String, Integer> DURABILITY_MULTIPLIER = java.util.Map.of(
            "scrap", 6, "redcoat", 12, "adrien", 22, "anthralite", 32, "diamond_steel", 36,
            "treated_brass", 30, "ancient_brass", 16, "exo_suit", 200);

    /** Durability of a piece of this mod's armour, as in the reference. */
    public static int durability(DeferredHolder<ArmorMaterial, ArmorMaterial> material, ArmorItem.Type type) {
        return BASE_DURABILITY.get(type) * DURABILITY_MULTIPLIER.get(material.getId().getPath());
    }
}
