package top.kristeal.attributes;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import top.kristeal.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class SCEntityAttributes {
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SCAttributes.PROJECTILE_SPEED);
        event.add(EntityType.PLAYER, SCAttributes.RELOAD_SPEED);
        event.add(EntityType.PLAYER, SCAttributes.ADDITIONAL_BULLET_DAMAGE);
        event.add(EntityType.PLAYER, SCAttributes.BULLET_DAMAGE_MULTIPLIER);
        event.add(EntityType.PLAYER, SCAttributes.SPREAD_MULTIPLIER);
    }
}
