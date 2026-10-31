package top.kristeal.progression;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kristeal.Reference;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Loads the gun tiers from {@code data/<ns>/gun_tiers/*.json} (server side). The tier id is the file name for the
 * {@code scguns} namespace and the full id otherwise. Unknown blueprint ids are logged as errors and skipped.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunTierManager extends SimpleJsonResourceReloadListener
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<String, GunTier> TIERS = new LinkedHashMap<>();
    private static List<GunTier> ordered = List.of();

    public GunTierManager()
    {
        super(GSON, "gun_tiers");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event)
    {
        event.addListener(new GunTierManager());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        TIERS.clear();
        files.forEach((location, element) -> {
            String id = location.getNamespace().equals(Reference.MOD_ID) ? location.getPath() : location.toString();
            try
            {
                TIERS.put(id, parse(id, element.getAsJsonObject(), location));
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to load gun tier {}", location, e);
            }
        });
        ordered = TIERS.values().stream()
                .sorted(Comparator.comparingInt(GunTier::order).thenComparingInt(GunTier::level))
                .toList();
        for (GunTier tier : ordered)
        {
            for (String mobTier : tier.mobTiers())
            {
                if (!TIERS.containsKey(mobTier))
                {
                    LOGGER.error("Gun tier {} lists unknown mob tier '{}'", tier.id(), mobTier);
                }
            }
        }
        LOGGER.info("Loaded {} gun tiers", TIERS.size());
    }

    private static GunTier parse(String id, JsonObject json, ResourceLocation location)
    {
        int level = json.get("level").getAsInt();
        int order = json.has("order") ? json.get("order").getAsInt() : level;
        TagKey<Item> tag = json.has("tag") ? TagKey.create(Registries.ITEM, ResourceLocation.parse(json.get("tag").getAsString())) : null;
        int raidLevel = json.has("raid_level") ? json.get("raid_level").getAsInt() : 0;

        List<String> mobTiers = new ArrayList<>();
        if (json.has("mob_tiers"))
        {
            json.getAsJsonArray("mob_tiers").forEach(e -> mobTiers.add(e.getAsString()));
        }

        List<Item> blueprints = new ArrayList<>();
        if (json.has("blueprints"))
        {
            JsonArray array = json.getAsJsonArray("blueprints");
            for (JsonElement e : array)
            {
                ResourceLocation itemId = ResourceLocation.parse(e.getAsString());
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
                if (item.isPresent())
                {
                    blueprints.add(item.get());
                }
                else
                {
                    LOGGER.error("Gun tier {} ({}): unknown blueprint '{}'", id, location, itemId);
                }
            }
        }
        return new GunTier(id, level, order, tag, raidLevel, List.copyOf(mobTiers), List.copyOf(blueprints));
    }

    /** The tier with this id, or {@link GunTier#NONE}. */
    public static GunTier getTier(@Nullable String id)
    {
        if (id == null)
        {
            return GunTier.NONE;
        }
        return TIERS.getOrDefault(id, GunTier.NONE);
    }

    /** Every loaded tier, in check order. */
    public static List<GunTier> getAllTiers()
    {
        return ordered;
    }

    /** The first tier (in check order) whose tag holds this item. */
    @Nullable
    public static GunTier getTierForItem(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return null;
        }
        for (GunTier tier : ordered)
        {
            if (tier.matches(stack))
            {
                return tier;
            }
        }
        return null;
    }

    /** The highest-level tier this blueprint unlocks. */
    @Nullable
    public static GunTier getTierForBlueprint(ItemStack stack)
    {
        GunTier best = null;
        for (GunTier tier : ordered)
        {
            if (tier.isUnlockedBy(stack) && (best == null || tier.level() > best.level()))
            {
                best = tier;
            }
        }
        return best;
    }

    /** The highest raid level any tier gives. */
    public static int getMaxRaidLevel()
    {
        return ordered.stream().mapToInt(GunTier::raidLevel).max().orElse(0);
    }
}
