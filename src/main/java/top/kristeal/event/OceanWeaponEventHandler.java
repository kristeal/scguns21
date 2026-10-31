package top.kristeal.event;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.kristeal.Reference;
import top.kristeal.init.ModTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dolphin's Grace while holding an ocean gun underwater.
 * Port note: {@code LivingEvent.LivingTickEvent} is {@code EntityTickEvent.Pre}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class OceanWeaponEventHandler {

    private static final Map<UUID, Boolean> appliedDolphinGrace = new HashMap<>();

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            applyDolphinGrace(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            applyDolphinGrace(player);
        }
    }

    private static void applyDolphinGrace(Player player) {
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        boolean holdingOceanWeapon = isOceanWeapon(mainHandItem) || isOceanWeapon(offHandItem);
        boolean isInWater = player.isEyeInFluid(FluidTags.WATER);
        MobEffectInstance dolphinGraceEffect = player.getEffect(MobEffects.DOLPHINS_GRACE);

        UUID playerId = player.getUUID();
        boolean wasAppliedByUs = appliedDolphinGrace.getOrDefault(playerId, false);

        if (holdingOceanWeapon && isInWater) {
            if (dolphinGraceEffect == null || dolphinGraceEffect.getDuration() <= 10) {
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
                appliedDolphinGrace.put(playerId, true);
            } else if (dolphinGraceEffect.getDuration() > 60) {
                appliedDolphinGrace.put(playerId, false);
            }
        } else {
            if (wasAppliedByUs && dolphinGraceEffect != null && dolphinGraceEffect.getDuration() <= 60) {
                player.removeEffect(MobEffects.DOLPHINS_GRACE);
                appliedDolphinGrace.remove(playerId);
            } else if (!holdingOceanWeapon || !isInWater) {
                appliedDolphinGrace.remove(playerId);
            }
        }
    }

    private static boolean isOceanWeapon(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.is(ModTags.Items.OCEAN_GUN);
    }
}
