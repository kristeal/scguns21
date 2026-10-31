package top.kristeal.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kristeal.Reference;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Gunner data files, read from the main {@code scguns} data:
 * <ul>
 *     <li>{@code entity/gunner_mobs.json}: "thematic" gunners, mobs that always carry their own weapons when they roll
 *     their spawn chance (pillagers, vindicators, piglins, Hornlins);</li>
 *     <li>{@code entity/tier_weapons.json}: the guns a progression gunner of each tier may get;</li>
 *     <li>{@code entity/elite_tiers.json}: the guns and armour of an elite gunner of each tier.</li>
 * </ul>
 * Tier keys are matched to the gun tier ids without case ({@code "IRON"} is {@code iron}).
 * <p>
 * Port notes: the reference had three classes for this (GunnerMobConfig, TieredWeaponConfig, EliteTierConfig). Its
 * {@code weapon_drop_chance} was read but never applied; it is the weapon's drop chance here. Unknown ids are logged as
 * errors and skipped.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunnerMobConfig
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<EntityType<?>, ThematicGunner> THEMATIC = new HashMap<>();
    private static final Map<String, List<Item>> TIER_WEAPONS = new HashMap<>();
    private static final Map<String, Elite> ELITES = new HashMap<>();

    public record ArmorPiece(Item item, String slot, float chance) {}

    public record ThematicGunner(float spawnChance, List<Item> weapons, List<ArmorPiece> armor, int aiDifficulty, float weaponDropChance)
    {
        @Nullable
        public Item getRandomWeapon(RandomSource random)
        {
            return weapons.isEmpty() ? null : weapons.get(random.nextInt(weapons.size()));
        }
    }

    public record Elite(List<Item> weapons, List<ArmorPiece> armor)
    {
        @Nullable
        public Item getRandomWeapon(RandomSource random)
        {
            return weapons.isEmpty() ? null : weapons.get(random.nextInt(weapons.size()));
        }
    }

    @Nullable
    public static ThematicGunner getThematic(EntityType<?> type)
    {
        return THEMATIC.get(type);
    }

    public static List<Item> getTierWeapons(String tierId)
    {
        return TIER_WEAPONS.getOrDefault(tierId, List.of());
    }

    @Nullable
    public static Elite getElite(String tierId)
    {
        return ELITES.get(tierId);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event)
    {
        event.addListener(new SimplePreparableReloadListener<Void>()
        {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler)
            {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler)
            {
                load(resourceManager);
            }
        });
    }

    private static void load(ResourceManager resourceManager)
    {
        THEMATIC.clear();
        TIER_WEAPONS.clear();
        ELITES.clear();

        JsonObject gunners = read(resourceManager, "gunner_mobs");
        if (gunners != null && gunners.has("mobs"))
        {
            for (Map.Entry<String, JsonElement> entry : gunners.getAsJsonObject("mobs").entrySet())
            {
                Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entry.getKey()));
                if (type.isEmpty())
                {
                    LOGGER.error("gunner_mobs.json: unknown entity '{}'", entry.getKey());
                    continue;
                }
                JsonObject data = entry.getValue().getAsJsonObject();
                THEMATIC.put(type.get(), new ThematicGunner(
                        data.has("spawn_chance") ? data.get("spawn_chance").getAsFloat() : 0.3f,
                        items(data, "weapons", "gunner_mobs.json"),
                        armor(data, "gunner_mobs.json"),
                        data.has("ai_difficulty") ? data.get("ai_difficulty").getAsInt() : 2,
                        data.has("weapon_drop_chance") ? data.get("weapon_drop_chance").getAsFloat() : 0.085f));
            }
        }

        JsonObject tiers = read(resourceManager, "tier_weapons");
        if (tiers != null)
        {
            for (Map.Entry<String, JsonElement> entry : tiers.entrySet())
            {
                List<Item> weapons = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray())
                {
                    item(element.getAsString(), "tier_weapons.json").ifPresent(weapons::add);
                }
                TIER_WEAPONS.put(entry.getKey().toLowerCase(Locale.ROOT), List.copyOf(weapons));
            }
        }

        JsonObject elites = read(resourceManager, "elite_tiers");
        if (elites != null)
        {
            for (Map.Entry<String, JsonElement> entry : elites.entrySet())
            {
                JsonObject data = entry.getValue().getAsJsonObject();
                ELITES.put(entry.getKey().toLowerCase(Locale.ROOT), new Elite(items(data, "weapons", "elite_tiers.json"), armor(data, "elite_tiers.json")));
            }
        }

        LOGGER.info("Loaded gunner configs: {} thematic mobs, {} weapon tiers, {} elite tiers", THEMATIC.size(), TIER_WEAPONS.size(), ELITES.size());
    }

    @Nullable
    private static JsonObject read(ResourceManager resourceManager, String name)
    {
        ResourceLocation location = Reference.id("entity/" + name + ".json");
        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty())
        {
            LOGGER.error("Gunner config {} not found", location);
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, JsonObject.class);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to load gunner config {}", location, e);
            return null;
        }
    }

    private static List<Item> items(JsonObject json, String key, String file)
    {
        List<Item> items = new ArrayList<>();
        if (json.has(key))
        {
            for (JsonElement element : json.getAsJsonArray(key))
            {
                item(element.getAsString(), file).ifPresent(items::add);
            }
        }
        return List.copyOf(items);
    }

    private static List<ArmorPiece> armor(JsonObject json, String file)
    {
        List<ArmorPiece> armor = new ArrayList<>();
        if (json.has("armor"))
        {
            for (JsonElement element : json.getAsJsonArray("armor"))
            {
                JsonObject piece = element.getAsJsonObject();
                float chance = piece.has("chance") ? piece.get("chance").getAsFloat() : 1.0f;
                item(piece.get("item").getAsString(), file).ifPresent(item -> armor.add(new ArmorPiece(item, piece.get("slot").getAsString(), chance)));
            }
        }
        return List.copyOf(armor);
    }

    private static Optional<Item> item(String id, String file)
    {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
        if (item.isEmpty())
        {
            LOGGER.error("{}: unknown item '{}'", file, id);
        }
        return item;
    }
}
