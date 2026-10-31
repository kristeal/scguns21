package top.kristeal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import top.kristeal.Reference;
import top.kristeal.entity.monster.BlundererEntity;
import top.kristeal.init.ModEntities;

/**
 * Adds one Blunderer to every vanilla village raid from its third wave on. This is unrelated to the flare raids.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidEventHandler
{
    @SubscribeEvent
    public static void onRaidSpawn(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Raider raider) || !(event.getLevel() instanceof ServerLevel serverLevel))
        {
            return;
        }

        Raid raid = serverLevel.getRaidAt(raider.blockPosition());
        if (raid == null)
        {
            return;
        }

        int wave = raid.getGroupsSpawned();
        if (wave > 1 && !isBlundererAlreadyInRaid(raid))
        {
            BlundererEntity blunderer = ModEntities.BLUNDERER.get().create(serverLevel);
            if (blunderer != null)
            {
                BlockPos pos = raider.blockPosition();
                blunderer.setPos(pos.getX(), pos.getY(), pos.getZ());
                raid.joinRaid(wave, blunderer, pos, false);
                serverLevel.addFreshEntity(blunderer);
            }
        }
    }

    private static boolean isBlundererAlreadyInRaid(Raid raid)
    {
        for (Raider raider : raid.getAllRaiders())
        {
            if (raider instanceof BlundererEntity)
            {
                return true;
            }
        }
        return false;
    }
}
