package top.kristeal.event;

import net.minecraft.world.entity.ambient.Bat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import top.kristeal.Reference;
import top.kristeal.entity.ai.BatGlowberryGoal;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class BatAIEventHandler {

    @SubscribeEvent
    public static void onBatSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Bat bat) {
            if (!event.getLevel().isClientSide()) {
                bat.goalSelector.addGoal(2, new BatGlowberryGoal(bat, 0.6, 8));
            }
        }
    }
}
