package top.kristeal.event;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.kristeal.Reference;
import top.kristeal.effect.SulfurPoisoningEffect;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class SulfurPoisoningEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (SulfurPoisoningEffect.hasFireVulnerability(entity)) {
            if (event.getSource().is(DamageTypes.IN_FIRE) ||
                    event.getSource().is(DamageTypes.ON_FIRE) ||
                    event.getSource().is(DamageTypes.LAVA) ||
                    event.getSource().is(DamageTypes.HOT_FLOOR)) {

                float multiplier = SulfurPoisoningEffect.getFireDamageMultiplier(entity);
                event.setAmount(event.getAmount() * multiplier);
            }
        }
    }
}
