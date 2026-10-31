package top.kristeal.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import top.kristeal.Reference;
import top.kristeal.progression.ProgressionMode;

/** Forgets the remote server's progression mode when leaving it. */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ProgressionClientEvents
{
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ProgressionMode.setClientOverride(null);
    }
}
