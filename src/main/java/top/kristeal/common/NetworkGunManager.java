package top.kristeal.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.Validate;
import top.kristeal.ScorchedGuns;
import top.kristeal.Reference;
import top.kristeal.annotation.Validator;
import top.kristeal.client.util.Easings;
import top.kristeal.item.GunItem;
import top.kristeal.network.message.S2CMessageUpdateGuns;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Author: MrCrayfish
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class NetworkGunManager extends SimplePreparableReloadListener<Map<GunItem, Gun>>
{
    private static final int FILE_TYPE_LENGTH_VALUE = ".json".length();
    private static final Gson GSON_INSTANCE = Util.make(() -> {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResourceLocation.class, JsonDeserializers.RESOURCE_LOCATION);
        builder.registerTypeAdapter(FireMode.class, JsonDeserializers.FIRE_MODE);
        builder.registerTypeAdapter(ReloadType.class, JsonDeserializers.RELOAD_TYPE);
        builder.registerTypeAdapter(GripType.class, JsonDeserializers.GRIP_TYPE);
        builder.registerTypeAdapter(Easings.class, JsonDeserializers.EASING);
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        return builder.create();
    });

    private static final List<GunItem> clientRegisteredGuns = new ArrayList<>();
    private static NetworkGunManager instance;

    private Map<ResourceLocation, Gun> registeredGuns = new HashMap<>();

    @Override
    protected Map<GunItem, Gun> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<GunItem, Gun> map = new HashMap<>();
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof GunItem).forEach(item ->
        {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if(id != null)
            {
                List<ResourceLocation> resources = new ArrayList<>(manager.listResources("guns", (fileName) -> fileName.getPath().endsWith(id.getPath() + ".json")).keySet());
                resources.sort((r1, r2) -> {
                    if(r1.getNamespace().equals(r2.getNamespace())) return 0;
                    return r2.getNamespace().equals(Reference.MOD_ID) ? 1 : -1;
                });
                resources.forEach(resourceLocation ->
                {
                    String path = resourceLocation.getPath().substring(0, resourceLocation.getPath().length() - FILE_TYPE_LENGTH_VALUE);
                    String[] splitPath = path.split("/");

                    // Makes sure the file name matches exactly with the id of the gun
                    if(!id.getPath().equals(splitPath[splitPath.length - 1]))
                        return;

                    // Also check if the mod id matches with the gun's registered namespace
                    if (!id.getNamespace().equals(resourceLocation.getNamespace()))
                        return;

                    manager.getResource(resourceLocation).ifPresent(resource ->
                    {
                        try(Reader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8)))
                        {
                            com.google.gson.JsonObject jsonObject = GsonHelper.parse(reader);
                            Gun gun = GSON_INSTANCE.fromJson(jsonObject, Gun.class);

                            if(gun != null && Validator.isValidObject(gun))
                            {
                                gun.loadAlternateProjectilesFromJson(jsonObject);
                                map.put((GunItem) item, gun);
                            }
                            else
                            {
                                ScorchedGuns.LOGGER.error("Couldn't load data file {} as it is missing or malformed. Using default gun data", resourceLocation);
                                map.putIfAbsent((GunItem) item, new Gun());
                            }
                        }
                        catch(InvalidObjectException e)
                        {
                            ScorchedGuns.LOGGER.error("Missing required properties for {}", resourceLocation);
                            e.printStackTrace();
                        }
                        catch(IOException e)
                        {
                            ScorchedGuns.LOGGER.error("Couldn't parse data file {}", resourceLocation);
                        }
                        catch(IllegalAccessException e)
                        {
                            e.printStackTrace();
                        }
                    });
                });
            }
        });
        return map;
    }

    @Override
    protected void apply(Map<GunItem, Gun> objects, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        ImmutableMap.Builder<ResourceLocation, Gun> builder = ImmutableMap.builder();
        objects.forEach((item, gun) -> {
            builder.put(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)), gun);
            item.setGun(new Supplier(gun));
        });
        this.registeredGuns = builder.build();
    }

    /**
     * Writes all registered guns into the provided packet buffer
     *
     * @param buffer a packet buffer get
     */
    public void writeRegisteredGuns(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(this.registeredGuns.size());
        this.registeredGuns.forEach((id, gun) -> {
            buffer.writeResourceLocation(id);
            buffer.writeNbt(gun.serializeNBT());
        });
    }

    /**
     * Reads all registered guns from the provided packet buffer
     *
     * @param buffer a packet buffer get
     * @return a map of registered guns from the server
     */
    public static ImmutableMap<ResourceLocation, Gun> readRegisteredGuns(FriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        if(size > 0)
        {
            ImmutableMap.Builder<ResourceLocation, Gun> builder = ImmutableMap.builder();
            for(int i = 0; i < size; i++)
            {
                ResourceLocation id = buffer.readResourceLocation();
                Gun gun = Gun.create(buffer.readNbt());
                builder.put(id, gun);
            }
            return builder.build();
        }
        return ImmutableMap.of();
    }

    public static boolean updateRegisteredGuns(S2CMessageUpdateGuns message)
    {
        return updateRegisteredGuns(message.getRegisteredGuns());
    }

    /**
     * Updates registered guns from data provided by the server
     *
     * @return true if all registered guns were able to update their corresponding gun item
     */
    private static boolean updateRegisteredGuns(Map<ResourceLocation, Gun> registeredGuns)
    {
        clientRegisteredGuns.clear();
        if(registeredGuns != null)
        {
            for(Map.Entry<ResourceLocation, Gun> entry : registeredGuns.entrySet())
            {
                Item item = BuiltInRegistries.ITEM.get(entry.getKey());
                if(!(item instanceof GunItem))
                {
                    return false;
                }
                ((GunItem) item).setGun(new Supplier(entry.getValue()));
                clientRegisteredGuns.add((GunItem) item);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets a map of all the registered guns objects. Note, this is an immutable map.
     *
     * @return a map of registered gun objects
     */
    public Map<ResourceLocation, Gun> getRegisteredGuns()
    {
        return this.registeredGuns;
    }

    /**
     * Gets a list of all the guns registered on the client side. Note, this is an immutable list.
     *
     * @return a map of guns registered on the client
     */
    public static List<GunItem> getClientRegisteredGuns()
    {
        return ImmutableList.copyOf(clientRegisteredGuns);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        NetworkGunManager.instance = null;
    }

    @SubscribeEvent
    public static void addReloadListenerEvent(AddReloadListenerEvent event)
    {
        NetworkGunManager networkGunManager = new NetworkGunManager();
        event.addListener(networkGunManager);
        NetworkGunManager.instance = networkGunManager;
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        // Port note: the reference sent this to everyone on /reload and relied on Framework login data (LoginData
        // below) for joining players. OnDatapackSyncEvent fires for both, and getRelevantPlayers() is the joining player or
        // every player on /reload.
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, new S2CMessageUpdateGuns()));
    }

    /**
     * Gets the network gun manager. This will be null if the client isn't running an integrated
     * server or the client is connected to a dedicated server.
     *
     * @return the network gun manager
     */
    @Nullable
    public static NetworkGunManager get()
    {
        return instance;
    }

    /**
     * A simple wrapper for a gun object to pass to GunItem. This is to indicate to developers that
     * Gun instances shouldn't be changed on GunItems as they are controlled by NetworkGunManager.
     * Changes to gun properties should be made through the JSON file.
     */
    public static class Supplier
    {
        private final Gun gun;

        private Supplier(Gun gun)
        {
            this.gun = gun;
        }

        public Gun getGun()
        {
            return this.gun;
        }
    }

    // Port note: the reference's Framework LoginData (writeRegisteredGuns/readRegisteredGuns at login) is replaced by
    // S2CMessageUpdateGuns sent from onDatapackSync.
}
