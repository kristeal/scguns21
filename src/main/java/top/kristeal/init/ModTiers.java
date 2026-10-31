package top.kristeal.init;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;


public class ModTiers {

    public static final Tier ANTHRALITE = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL,   // was getLevel() == 2
            600,                                 // was getUses()
            7.0F,                                // was getSpeed()
            2.5F,                                // was getAttackDamageBonus()
            10,                                  // was getEnchantmentValue()
            () -> Ingredient.of(ModItems.ANTHRALITE_INGOT.get())
    );

    public static final Tier ANCIENT_BRASS = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            250,
            6.0F,
            3.0F,
            14,
            () -> Ingredient.of(ModItems.ANCIENT_BRASS.get())
    );
}

