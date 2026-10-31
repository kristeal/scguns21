package top.kristeal.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
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
 * Loads {@code data/<ns>/flares/*.json}: how a raid flare looks in flight and when it bursts, keyed by the raid it starts.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidFlareConfig
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, FlareData> FLARES = new HashMap<>();

    public record ParticleEffect(String particleType, int count, double spread, double speed, int color) {}

    public record FlarePattern(String patternType, int repetitions, double scale) {}

    public record FlareData(
            String raidId,
            int burstDelay,
            List<ParticleEffect> trailParticles,
            List<ParticleEffect> burstParticles,
            @Nullable FlarePattern pattern,
            String burstSound,
            float burstSoundVolume,
            float burstSoundPitch
    ) {}

    public static void loadFlareConfigs(ResourceManager resourceManager)
    {
        FLARES.clear();

        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources("flares", loc -> loc.getPath().endsWith(".json")).entrySet())
        {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))
            {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                FlareData flare = parseFlareData(json, location);
                FLARES.put(flare.raidId(), flare);
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to load flare config {}", location, e);
            }
        }

        LOGGER.info("Loaded {} flare configurations", FLARES.size());
    }

    private static FlareData parseFlareData(JsonObject json, ResourceLocation location)
    {
        String path = location.getPath();
        String flareId = path.substring(path.lastIndexOf('/') + 1).replace("_flare.json", "").replace(".json", "");
        String raidId = json.has("raid_id") ? json.get("raid_id").getAsString() : flareId;

        int burstDelay = json.has("burst_delay") ? json.get("burst_delay").getAsInt() : 40;

        FlarePattern pattern = null;
        if (json.has("pattern"))
        {
            JsonObject patternObj = json.getAsJsonObject("pattern");
            pattern = new FlarePattern(
                    patternObj.has("type") ? patternObj.get("type").getAsString() : "circle",
                    patternObj.has("repetitions") ? patternObj.get("repetitions").getAsInt() : 1,
                    patternObj.has("scale") ? patternObj.get("scale").getAsDouble() : 1.0);
        }

        String burstSound = json.has("burst_sound") ? json.get("burst_sound").getAsString() : "minecraft:entity.firework_rocket.large_blast";
        float burstSoundVolume = json.has("burst_sound_volume") ? json.get("burst_sound_volume").getAsFloat() : 1.0f;
        float burstSoundPitch = json.has("burst_sound_pitch") ? json.get("burst_sound_pitch").getAsFloat() : 1.0f;

        return new FlareData(raidId, burstDelay, parseParticleEffects(json, "trail_particles"), parseParticleEffects(json, "burst_particles"),
                pattern, burstSound, burstSoundVolume, burstSoundPitch);
    }

    private static List<ParticleEffect> parseParticleEffects(JsonObject json, String key)
    {
        List<ParticleEffect> effects = new ArrayList<>();
        if (!json.has(key))
        {
            return effects;
        }

        JsonArray effectsArray = json.getAsJsonArray(key);
        for (JsonElement element : effectsArray)
        {
            JsonObject effectObj = element.getAsJsonObject();
            String particleType = effectObj.get("particle").getAsString();
            int count = effectObj.has("count") ? effectObj.get("count").getAsInt() : 10;
            double spread = effectObj.has("spread") ? effectObj.get("spread").getAsDouble() : 0.3;
            double speed = effectObj.has("speed") ? effectObj.get("speed").getAsDouble() : 0.1;
            int color = effectObj.has("color") ? Integer.parseInt(effectObj.get("color").getAsString().replace("#", ""), 16) : 0xFFFFFF;
            effects.add(new ParticleEffect(particleType, count, spread, speed, color));
        }
        return effects;
    }

    @Nullable
    public static FlareData getFlareData(String raidId)
    {
        return FLARES.get(raidId);
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
                loadFlareConfigs(resourceManager);
            }
        });
    }
}
