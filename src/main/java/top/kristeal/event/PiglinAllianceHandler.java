package top.kristeal.event;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.entity.monster.ZombifiedHornlinEntity;

// Port note: Forge LivingHurtEvent -> NeoForge LivingIncomingDamageEvent (also fires before armour)
@EventBusSubscriber(modid = Reference.MOD_ID)
public class PiglinAllianceHandler {

    private static final double ALLIANCE_RANGE = 26.0;
    private static final int ALERT_RANGE_Y = 10;

    @SubscribeEvent
    public static void onZombifiedPiglinHurt(LivingIncomingDamageEvent event) {

        if (event.getAmount() <= 0) return;
        if (!(event.getEntity() instanceof ZombifiedPiglin piglin)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (piglin.level().isClientSide) return;
        AABB quickCheck = AABB.unitCubeFromLowerCorner(piglin.position()).inflate(ALLIANCE_RANGE, ALERT_RANGE_Y, ALLIANCE_RANGE);
        if (piglin.level().getEntitiesOfClass(ZombifiedHornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS).isEmpty()) {
            return;
        }

        piglin.level().getEntitiesOfClass(ZombifiedHornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS)
                .stream()
                .filter(hornlin -> hornlin.getTarget() == null)
                .filter(hornlin -> !hornlin.isAlliedTo(player))
                .forEach(hornlin -> {
                    hornlin.setLastHurtByPlayer(player);
                    hornlin.startPersistentAngerTimer();
                    hornlin.setPersistentAngerTarget(player.getUUID());
                    hornlin.setTarget(player);

                    try {
                        java.lang.reflect.Field playFirstAngerSoundInField =
                                ZombifiedHornlinEntity.class.getDeclaredField("playFirstAngerSoundIn");
                        playFirstAngerSoundInField.setAccessible(true);
                        playFirstAngerSoundInField.setInt(hornlin, 0);

                        java.lang.reflect.Field ticksUntilNextAlertField =
                                ZombifiedHornlinEntity.class.getDeclaredField("ticksUntilNextAlert");
                        ticksUntilNextAlertField.setAccessible(true);
                        ticksUntilNextAlertField.setInt(hornlin, 0);
                    } catch (Exception e) {
                        // Silently continue - not critical
                    }
                });
    }
}