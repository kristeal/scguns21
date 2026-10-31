package top.kristeal.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Reference;
import top.kristeal.client.handler.MeleeAttackHandler;

/**
 * Drives the bayonet charge on the server: one check per player tick, and cleanup when the player leaves or respawns.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class BanzaiEventHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MeleeAttackHandler.tickBanzai(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MeleeAttackHandler.clearBanzai(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MeleeAttackHandler.clearBanzai(player);
        }
    }
}
