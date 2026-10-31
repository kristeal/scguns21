package top.kristeal.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.entity.ai.AIType;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads {@code data/<ns>/raids/*.json}: one boss (optionally mounted) and its henchmen per raid.
 * <p>
 * {@code raid_level} makes a raid a nightly progression raid: a player whose gun tier gives that raid level (or higher) can
 * be its target. A raid without one (Blood Moon) only starts from its flare or the command.
 * {@code spawn_conditions.valid_dimensions} limits where a nightly raid can happen (empty: anywhere with a day cycle).
 * <p>
 * Port notes: {@code min_players_nearby} and {@code max_total} (read but never used by the reference) are gone; the
 * reference read {@code valid_dimensions} without using it, it is used here. The weapon's {@code nbt} block, which only
 * ever held one enchantment, became an {@code enchantments} list of {@code {id, level}}. An unknown item, entity or effect
 * id is logged as an error and the entry is skipped; a boss or spawn-conditions failure drops the whole raid.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidConfig
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, RaidData> RAIDS_BY_ID = new LinkedHashMap<>();

    public record HealthConfig(@Nullable Float fixedHealth, @Nullable Float healthMultiplier)
    {
        public boolean useMultiplier()
        {
            return healthMultiplier != null;
        }
    }

    public record EnchantEntry(ResourceLocation id, int level) {}
    public record WeaponEntry(Item item, float dropChance, List<EnchantEntry> enchantments) {}
    /** {@code chance} is the drop chance for a boss or mount, and the chance to be worn for a henchman. */
    public record ArmorEntry(Item item, String slot, float chance) {}
    public record EffectEntry(Holder<MobEffect> effect, int amplifier, int duration, boolean ambient, boolean visible) {}

    public record BossData(
            EntityType<?> entityType,
            @Nullable String customName,
            HealthConfig healthConfig,
            @Nullable WeaponEntry weapon,
            List<ArmorEntry> armor,
            List<EffectEntry> effects,
            int aiDifficulty,
            AIType aiType,
            @Nullable ResourceLocation specialLootTable,
            @Nullable MountData mount
    ) {}

    public record MountData(
            EntityType<?> entityType,
            HealthConfig healthConfig,
            List<ArmorEntry> armor,
            List<EffectEntry> effects,
            boolean mountDropsLoot
    ) {}

    public record HenchmanType(
            EntityType<?> entityType,
            float weight,
            HealthConfig healthConfig,
            List<Item> weapons,
            List<ArmorEntry> armor,
            List<EffectEntry> effects,
            int aiDifficulty,
            AIType aiType
    ) {}

    public record HenchmenData(
            List<HenchmanType> types,
            int maxAlive,
            int spawnIntervalTicks,
            int spawnRadius,
            int spawnAttemptsPerWave
    )
    {
        @Nullable
        public HenchmanType selectRandomType(RandomSource random)
        {
            if (types.isEmpty())
            {
                return null;
            }

            float totalWeight = 0;
            for (HenchmanType type : types)
            {
                totalWeight += type.weight;
            }

            float roll = random.nextFloat() * totalWeight;
            float currentWeight = 0;
            for (HenchmanType type : types)
            {
                currentWeight += type.weight;
                if (roll < currentWeight)
                {
                    return type;
                }
            }

            return types.get(types.size() - 1);
        }
    }

    public record SpawnConditions(int searchRadius, String announcementMessage, List<ResourceLocation> validDimensions)
    {
        public boolean allows(ResourceLocation dimension)
        {
            return validDimensions.isEmpty() || validDimensions.contains(dimension);
        }
    }

    public record RaidData(String raidId, @Nullable Integer raidLevel, BossData boss, HenchmenData henchmen, SpawnConditions spawnConditions) {}

    public static void loadRaidConfigs(ResourceManager resourceManager)
    {
        RAIDS_BY_ID.clear();

        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources("raids", loc -> loc.getPath().endsWith(".json")).entrySet())
        {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))
            {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                RaidData raid = parseRaidData(json, location);
                if (raid != null)
                {
                    RAIDS_BY_ID.put(raid.raidId(), raid);
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to load raid config {}", location, e);
            }
        }

        LOGGER.info("Loaded {} raids", RAIDS_BY_ID.size());
    }

    @Nullable
    public static RaidData getRaidById(String raidId)
    {
        return RAIDS_BY_ID.get(raidId);
    }

    public static Collection<RaidData> getAllRaids()
    {
        return RAIDS_BY_ID.values();
    }

    /** The progression raids of exactly this level. */
    public static List<RaidData> getRaidsAtLevel(int level)
    {
        return RAIDS_BY_ID.values().stream().filter(raid -> raid.raidLevel() != null && raid.raidLevel() == level).toList();
    }

    /** Every progression raid a player at this raid level can get (levels 1 up to it). */
    public static List<RaidData> getRaidsForLevel(int level)
    {
        return RAIDS_BY_ID.values().stream().filter(raid -> raid.raidLevel() != null && raid.raidLevel() >= 1 && raid.raidLevel() <= level).toList();
    }

    /** Whether nightly raids run in the current progression mode. */
    public static boolean nightlyRaidsActive()
    {
        Config.Raids raids = Config.COMMON.raids;
        if (!raids.raidsEnabled.get() || !raids.nightlyRaids.get())
        {
            return false;
        }
        return top.kristeal.progression.ProgressionMode.isRaid() || raids.nightlyRaidsInResearchMode.get();
    }

    @Nullable
    private static RaidData parseRaidData(JsonObject json, ResourceLocation location)
    {
        try
        {
            String raidId = json.get("raid_id").getAsString();
            Integer raidLevel = json.has("raid_level") ? json.get("raid_level").getAsInt() : null;
            BossData boss = parseBossData(json.getAsJsonObject("boss"), location);
            HenchmenData henchmen = parseHenchmenData(json.getAsJsonObject("henchmen"), location);
            SpawnConditions conditions = parseSpawnConditions(json.getAsJsonObject("spawn_conditions"));
            if (boss == null)
            {
                LOGGER.error("Raid {} dropped: its boss could not be parsed", location);
                return null;
            }
            return new RaidData(raidId, raidLevel, boss, henchmen, conditions);
        }
        catch (Exception e)
        {
            LOGGER.error("Error parsing raid {}", location, e);
            return null;
        }
    }

    @Nullable
    private static BossData parseBossData(JsonObject json, ResourceLocation location)
    {
        EntityType<?> entityType = entityType(json.get("entity_type").getAsString(), location);
        if (entityType == null)
        {
            return null;
        }

        String customName = json.has("custom_name") ? json.get("custom_name").getAsString() : null;

        WeaponEntry weapon = null;
        if (json.has("weapon"))
        {
            JsonObject weaponObj = json.getAsJsonObject("weapon");
            Item item = item(weaponObj.get("item").getAsString(), location);
            if (item != null)
            {
                float dropChance = weaponObj.has("drop_chance") ? weaponObj.get("drop_chance").getAsFloat() : 0.085f;
                List<EnchantEntry> enchantments = new ArrayList<>();
                if (weaponObj.has("enchantments"))
                {
                    for (JsonElement element : weaponObj.getAsJsonArray("enchantments"))
                    {
                        JsonObject enchantment = element.getAsJsonObject();
                        enchantments.add(new EnchantEntry(ResourceLocation.parse(enchantment.get("id").getAsString()), enchantment.get("level").getAsInt()));
                    }
                }
                weapon = new WeaponEntry(item, dropChance, enchantments);
            }
        }

        int aiDifficulty = json.has("ai_difficulty") ? json.get("ai_difficulty").getAsInt() : 3;
        AIType aiType = json.has("ai_type") ? AIType.valueOf(json.get("ai_type").getAsString()) : AIType.DEFAULT;
        ResourceLocation lootTable = json.has("special_loot_table") ? ResourceLocation.parse(json.get("special_loot_table").getAsString()) : null;
        MountData mount = json.has("mount") ? parseMountData(json.getAsJsonObject("mount"), location) : null;

        return new BossData(entityType, customName, parseHealthConfig(json), weapon, parseArmor(json, "drop_chance", 0.085f, location),
                parseEffects(json, location), aiDifficulty, aiType, lootTable, mount);
    }

    @Nullable
    private static MountData parseMountData(JsonObject json, ResourceLocation location)
    {
        EntityType<?> mountType = entityType(json.get("entity_type").getAsString(), location);
        if (mountType == null)
        {
            return null;
        }

        boolean dropsLoot = !json.has("drops_loot") || json.get("drops_loot").getAsBoolean();
        return new MountData(mountType, parseHealthConfig(json), parseArmor(json, "drop_chance", 0.085f, location), parseEffects(json, location), dropsLoot);
    }

    private static HenchmenData parseHenchmenData(JsonObject json, ResourceLocation location)
    {
        List<HenchmanType> types = new ArrayList<>();

        if (json.has("types"))
        {
            for (JsonElement element : json.getAsJsonArray("types"))
            {
                JsonObject typeObj = element.getAsJsonObject();

                EntityType<?> entityType = entityType(typeObj.get("entity_type").getAsString(), location);
                if (entityType == null)
                {
                    continue;
                }

                List<Item> weapons = new ArrayList<>();
                if (typeObj.has("weapons"))
                {
                    for (JsonElement weaponElement : typeObj.getAsJsonArray("weapons"))
                    {
                        Item weapon = item(weaponElement.getAsString(), location);
                        if (weapon != null)
                        {
                            weapons.add(weapon);
                        }
                    }
                }

                float weight = typeObj.has("weight") ? typeObj.get("weight").getAsFloat() : 1.0f;
                int aiDifficulty = typeObj.has("ai_difficulty") ? typeObj.get("ai_difficulty").getAsInt() : 2;
                AIType aiType = typeObj.has("ai_type") ? AIType.valueOf(typeObj.get("ai_type").getAsString()) : AIType.DEFAULT;

                types.add(new HenchmanType(entityType, weight, parseHealthConfig(typeObj), weapons, parseArmor(typeObj, "chance", 0.5f, location),
                        parseEffects(typeObj, location), aiDifficulty, aiType));
            }
        }

        int maxAlive = json.has("max_concurrent") ? json.get("max_concurrent").getAsInt() : json.has("max_alive") ? json.get("max_alive").getAsInt() : 4;
        int spawnInterval = json.has("spawn_interval_ticks") ? json.get("spawn_interval_ticks").getAsInt() : 200;
        int spawnRadius = json.has("spawn_radius") ? json.get("spawn_radius").getAsInt() : 20;
        int spawnAttempts = json.has("spawn_attempts_per_wave") ? json.get("spawn_attempts_per_wave").getAsInt() : 3;

        return new HenchmenData(types, maxAlive, spawnInterval, spawnRadius, spawnAttempts);
    }

    private static SpawnConditions parseSpawnConditions(JsonObject json)
    {
        int searchRadius = json.has("search_radius") ? json.get("search_radius").getAsInt() : 64;
        String announcement = json.has("announcement_message") ? json.get("announcement_message").getAsString() : "A raid approaches!";
        List<ResourceLocation> dimensions = new ArrayList<>();
        if (json.has("valid_dimensions"))
        {
            json.getAsJsonArray("valid_dimensions").forEach(e -> dimensions.add(ResourceLocation.parse(e.getAsString())));
        }
        return new SpawnConditions(searchRadius, announcement, List.copyOf(dimensions));
    }

    private static List<ArmorEntry> parseArmor(JsonObject json, String chanceKey, float defaultChance, ResourceLocation location)
    {
        List<ArmorEntry> armor = new ArrayList<>();
        if (!json.has("armor"))
        {
            return armor;
        }

        for (JsonElement element : json.getAsJsonArray("armor"))
        {
            JsonObject armorObj = element.getAsJsonObject();
            Item item = item(armorObj.get("item").getAsString(), location);
            if (item != null)
            {
                float chance = armorObj.has(chanceKey) ? armorObj.get(chanceKey).getAsFloat() : defaultChance;
                armor.add(new ArmorEntry(item, armorObj.get("slot").getAsString(), chance));
            }
        }
        return armor;
    }

    private static HealthConfig parseHealthConfig(JsonObject json)
    {
        Float fixedHealth = json.has("fixed_health") ? json.get("fixed_health").getAsFloat() : null;
        Float healthMultiplier = json.has("health_multiplier") ? json.get("health_multiplier").getAsFloat() : null;
        return new HealthConfig(fixedHealth, healthMultiplier);
    }

    private static List<EffectEntry> parseEffects(JsonObject json, ResourceLocation location)
    {
        List<EffectEntry> effects = new ArrayList<>();
        if (!json.has("effects"))
        {
            return effects;
        }

        JsonArray effectsArray = json.getAsJsonArray("effects");
        for (JsonElement element : effectsArray)
        {
            JsonObject effectObj = element.getAsJsonObject();
            String effectId = effectObj.get("effect").getAsString();
            Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectId));
            if (effect.isEmpty())
            {
                LOGGER.error("Raid {}: unknown effect {}", location, effectId);
                continue;
            }

            int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
            int duration = effectObj.has("duration") ? effectObj.get("duration").getAsInt() : -1;
            boolean ambient = effectObj.has("ambient") && effectObj.get("ambient").getAsBoolean();
            boolean visible = !effectObj.has("visible") || effectObj.get("visible").getAsBoolean();
            effects.add(new EffectEntry(effect.get(), amplifier, duration, ambient, visible));
        }
        return effects;
    }

    @Nullable
    private static Item item(String id, ResourceLocation location)
    {
        ResourceLocation itemId = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(itemId))
        {
            LOGGER.error("Raid {}: unknown item {}", location, id);
            return null;
        }
        return BuiltInRegistries.ITEM.get(itemId);
    }

    @Nullable
    private static EntityType<?> entityType(String id, ResourceLocation location)
    {
        ResourceLocation typeId = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId))
        {
            LOGGER.error("Raid {}: unknown entity type {}", location, id);
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(typeId);
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
                loadRaidConfigs(resourceManager);
            }
        });
    }
}
