package top.kristeal.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import top.kristeal.Reference;

import java.util.HashMap;
import java.util.Map;

// Port note: the reference applied this map through ItemRarityMixin (ItemStack#getRarity at RETURN). In 1.21
// rarity is the DataComponents.RARITY component, so the map is applied as each item's default component instead. Ids that
// aren't registered (the guns) are skipped and fill in once they are.
@EventBusSubscriber(modid = Reference.MOD_ID)
public class RarityUtils {
    private static final Map<ResourceLocation, Rarity> ITEM_RARITY_MAP = new HashMap<>();
    static {
        ITEM_RARITY_MAP.put(Reference.id("m22_waltz"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("waltz_conversion"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("osgood_50"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("grandle_og"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("grandle"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("cogloader"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("gale"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("jr_wristbreaker"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("jackhammer"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("howler"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("howler_conversion"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("gauss_rifle"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("libertas"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("niami"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("spitfire"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("gattaler"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("thunderhead"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("scratches"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("hammer_gl"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("cr4k_mining_laser"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("dozier_rl"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("treated_brass_blueprint"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("krauser"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("soul_drummer"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("uppercut"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("micina"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("valora"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("prush_gun"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("drill"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("drill_conversion"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("lockewood"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("zilk_45"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("rg_jigsaw"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("nailer"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("inertial"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("mas_55"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("mas_peddler"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("minksy"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("inquisitor"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("plasgun"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("truant"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("cyclone"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("shard_culler"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("diamond_steel_blueprint"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("hyperbaria"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("floundergat"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("marlin"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("bomb_lance"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("ocean_blueprint"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("sequoia"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("hullbreaker"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("spirulida"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("super_shotgun"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("blasphemy"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("freyr"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("pyroclastic_flow"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("vulcanic_repeater"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("piglin_blueprint"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("mangalitsa"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("trotters"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("hog_round"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("whispers"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("echoes_2"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("sculk_resonator"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("deep_dark_blueprint"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("forlorn_hope"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("sculk_cell"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("end_blueprint"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("lone_wonder"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("raygun"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("dark_matter"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("shellurker"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("carapice"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("weevil"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("shulkshot"), Constants.ENDISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("scorched_blueprint"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("scorched_ingot"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("earths_corpse"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("rat_king_and_queen"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("locust"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("newborn_cyst"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("astella"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("flayed_god"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("nervepinch"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("prima_materia"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("terra_incognita"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("sterilizer"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("advanced_exo_suit_core"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("the_pact"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("music_disc_mass_destruction"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("music_disc_mass_destruction_extended"), Constants.SCORCHED.getValue());
        ITEM_RARITY_MAP.put(Reference.id("felix_memorial"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("kiln_gun"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("ultra_knight_hawk"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("whizzbanger"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("big_bore"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("osborne_slug"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("ribs_glory"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("rusty_medal"), Constants.RUSTY.getValue());
        ITEM_RARITY_MAP.put(Reference.id("labor_trophy"), Constants.BIZARRE.getValue());
        ITEM_RARITY_MAP.put(Reference.id("snapped_cogwheel"), Constants.WRECKER.getValue());
        ITEM_RARITY_MAP.put(Reference.id("ceremonial_cod"), Constants.DIAMOND_STEEL.getValue());
        ITEM_RARITY_MAP.put(Reference.id("cog_heart"), Constants.TREATED_BRASS.getValue());
        ITEM_RARITY_MAP.put(Reference.id("gold_idol"), Constants.PIGLISH.getValue());
        ITEM_RARITY_MAP.put(Reference.id("leviathan_tooth"), Constants.OCEANIC.getValue());
        ITEM_RARITY_MAP.put(Reference.id("sculk_tome"), Constants.DEEP_DARK.getValue());
        ITEM_RARITY_MAP.put(Reference.id("shulker_core"), Constants.ENDISH.getValue());
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        ITEM_RARITY_MAP.forEach((id, rarity) -> BuiltInRegistries.ITEM.getOptional(id)
                .ifPresent(item -> event.modify(item, builder -> builder.set(DataComponents.RARITY, rarity))));
    }

    public static Rarity GetRarityFromResourceLocation(ResourceLocation location, Rarity oldRarity) {
        return ITEM_RARITY_MAP.getOrDefault(location, oldRarity);
    }

    public static Rarity GetRarityFromItem(Item item, Rarity old) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (BuiltInRegistries.ITEM.containsKey(key)) {
            return GetRarityFromResourceLocation(key, old);
        }
        return old;
    }
}
