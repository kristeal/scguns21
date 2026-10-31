package top.kristeal.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.kristeal.Reference;
import top.kristeal.init.ModItems;

/**
 * Resistance II (amplifier 1) while the Shellurker is held in either hand.
 * Port note: {@code LivingEvent.LivingTickEvent} is {@code EntityTickEvent.Pre}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ArmorBoostEventHandler {
    private static final int RESISTANCE_LEVEL = 1;
    private static final int EFFECT_DURATION = 80;

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            applyResistanceBoost(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            applyResistanceBoost(player);
        }
    }

    private static void applyResistanceBoost(Player player) {
        boolean holdingSpecialItem = isSpecialItem(player.getMainHandItem()) || isSpecialItem(player.getOffhandItem());
        if (holdingSpecialItem) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, RESISTANCE_LEVEL, false, false));
        }
    }

    private static boolean isSpecialItem(ItemStack itemStack) {
        return itemStack.getItem() == ModItems.SHELLURKER.get();
    }
}
