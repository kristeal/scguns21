package top.kristeal.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import top.kristeal.ScorchedGuns;
import top.kristeal.item.IMeta;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the {@code models/item/<id>.scmeta} files of every {@link IMeta} item.
 * <p>
 * Port note: the reference was a Framework {@code IDataLoader} producing {@code DataObject}s.
 * This is a plain client reload listener producing Gson {@link JsonObject}s from the same files; missing files are
 * ignored, as the reference's {@code ignoreMissing()} did.
 */
public final class MetaLoader extends SimplePreparableReloadListener<Map<Item, JsonObject>>
{
    private static final JsonObject EMPTY = new JsonObject();

    private static MetaLoader instance;

    public static MetaLoader getInstance()
    {
        if(instance == null)
        {
            instance = new MetaLoader();
        }
        return instance;
    }

    private final Reference2ObjectMap<Item, JsonObject> itemToData = Util.make(new Reference2ObjectOpenHashMap<>(), map -> map.defaultReturnValue(EMPTY)); // Port note: identity-keyed, as the reference's identityStrategy map

    private MetaLoader() {}

    public JsonObject getData(Item item)
    {
        return this.itemToData.get(item);
    }

    @Override
    protected Map<Item, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<Item, JsonObject> map = new HashMap<>();
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof IMeta).forEach(item ->
        {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "models/item/" + key.getPath() + ".scmeta");
            Optional<Resource> resource = manager.getResource(location);
            if(resource.isEmpty())
                return;
            try(Reader reader = resource.get().openAsReader())
            {
                map.put(item, JsonParser.parseReader(reader).getAsJsonObject());
            }
            catch(Exception e)
            {
                ScorchedGuns.LOGGER.error("Failed to load meta file {}", location, e);
            }
        });
        return map;
    }

    @Override
    protected void apply(Map<Item, JsonObject> map, ResourceManager manager, ProfilerFiller profiler)
    {
        this.itemToData.clear();
        map.forEach((item, object) ->
        {
            if(!object.isEmpty())
            {
                this.itemToData.put(item, object);
            }
        });
    }
}
