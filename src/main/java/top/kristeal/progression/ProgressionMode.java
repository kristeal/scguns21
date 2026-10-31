package top.kristeal.progression;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.network.message.S2CMessageProgressionMode;

import javax.annotation.Nullable;

/**
 * The world's progression: {@link #RAID} (the reference loop: gun tiers unlock nightly raids, raid trophies craft the
 * blueprints, the Gun Bench needs no research) or {@link #RESEARCH} (blueprints from structure loot, researched at the
 * Research Station). Gun tiers are tracked in both, so the mode can change at any time.
 * <p>
 * The mode is a server config value. Everything that depends on it checks it at runtime (no datapack conditions), so a
 * switch needs no reload. A client on a remote server gets the value from {@link S2CMessageProgressionMode}; a client on
 * the integrated server reads the shared config directly.
 */
public enum ProgressionMode
{
    RAID, RESEARCH;

    /** Set on a remote client by the server's packet; null on servers and integrated clients. */
    @Nullable private static ProgressionMode clientOverride;
    @Nullable private static ProgressionMode lastBroadcast;

    public static ProgressionMode get()
    {
        if (clientOverride != null)
        {
            return clientOverride;
        }
        return Config.isServerConfigLoaded() ? Config.SERVER.progression.mode.get() : RAID;
    }

    public static boolean isRaid()
    {
        return get() == RAID;
    }

    public static boolean isResearch()
    {
        return get() == RESEARCH;
    }

    /** Client side: the mode the server sent (null when leaving the server). */
    public static void setClientOverride(@Nullable ProgressionMode mode)
    {
        clientOverride = mode;
    }

    /** Writes the mode to the server config and tells every player. */
    public static void set(MinecraftServer server, ProgressionMode mode)
    {
        Config.SERVER.progression.mode.set(mode);
        Config.SERVER.progression.mode.save();
        broadcast(server);
    }

    /** Sends the current mode to every player and fires the mode advancement trigger. */
    public static void broadcast(MinecraftServer server)
    {
        lastBroadcast = get();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            send(player);
        }
    }

    private static void send(ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, new S2CMessageProgressionMode(get()));
        ProgressionModeTrigger.INSTANCE.get().trigger(player);
    }

    @EventBusSubscriber(modid = Reference.MOD_ID)
    public static class Events
    {
        /** On login and after every datapack reload. */
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event)
        {
            event.getRelevantPlayers().forEach(ProgressionMode::send);
        }
    }

    @EventBusSubscriber(modid = Reference.MOD_ID) // mod bus: picked from the event type
    public static class ConfigEvents
    {
        /** A hand edit of the server config file: tell the players if the mode changed. */
        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event)
        {
            if (event.getConfig().getType() != ModConfig.Type.SERVER || !event.getConfig().getModId().equals(Reference.MOD_ID))
            {
                return;
            }
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && lastBroadcast != get())
            {
                server.execute(() -> broadcast(server));
            }
        }
    }
}
