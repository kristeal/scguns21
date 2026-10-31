package top.kristeal.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import top.kristeal.Reference;
import top.kristeal.item.animated.ExoSuitItem;

/**
 * Handles fall damage reduction for ExoSuit mobility modules
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ExoSuitFallDamageHandler {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        float totalFallDamageReduction = calculateTotalFallDamageReduction(player);

        if (totalFallDamageReduction > 0) {
            float originalDamage = event.getDamageMultiplier();
            float reducedDamage = originalDamage * (1.0f - totalFallDamageReduction);
            reducedDamage = Math.max(0, reducedDamage);

            event.setDamageMultiplier(reducedDamage);
        }
    }

    private static float calculateTotalFallDamageReduction(Player player) {
        float totalReduction = 0.0f;

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem) {
                totalReduction += getFallDamageReductionFromPiece(armorStack, player);
            }
        }
        return Math.min(totalReduction, 1.0f);
    }

    /**
     * Gets fall damage reduction from a single armor piece
     */
    private static float getFallDamageReductionFromPiece(ItemStack armorStack, Player player) {
        float totalReduction = 0.0f;

        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null) {
                    ExoSuitUpgrade.Effects effects = upgrade.getEffects();
                    if (canUpgradeFunction(player, upgrade, upgradeItem)) {
                        totalReduction += effects.getFallDamageReduction();
                    }
                }
            }
        }

        return totalReduction;
    }

    private static boolean canUpgradeFunction(Player player, ExoSuitUpgrade upgrade, ItemStack upgradeItem) {
        String upgradeType = upgrade.getType();

        if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.EnergyUpgradeItem energyUpgrade) {
            boolean powerEnabled = ExoSuitPowerManager.isPowerEnabled(player, upgradeType);

            if (!powerEnabled) {
                return false;
            }

            if (!energyUpgrade.canFunctionWithoutPower()) {
                return ExoSuitPowerManager.canUpgradeFunction(player, upgradeType);
            }

            return true;
        }
        return true;
    }
}
