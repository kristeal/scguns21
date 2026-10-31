package top.kristeal.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.item.CogMaceItem;
@EventBusSubscriber(modid = Reference.MOD_ID)
public class CogMaceEventHandler {

    // Port note: Forge LivingHurtEvent -> NeoForge LivingIncomingDamageEvent (fires before blocking and armor, like the 1.20.1 event)
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.getMainHandItem().getItem() instanceof CogMaceItem) {
                LivingEntity target = event.getEntity();

                double movementSpeed = attacker.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double speedBonus = Math.max(0, movementSpeed - 0.1) * 30.0;

                if (attacker.isSprinting()) {
                    speedBonus *= 2.0;
                }
                double fallBonus = attacker.fallDistance * 1.0;

                float totalBonus = (float)(speedBonus + fallBonus);

                if (totalBonus > 0.1F) {
                    if (target.isBlocking()) {
                        totalBonus *= 0.5F;
                    }
                    event.setAmount(event.getAmount() + totalBonus);
                }
            }
        }
    }
}