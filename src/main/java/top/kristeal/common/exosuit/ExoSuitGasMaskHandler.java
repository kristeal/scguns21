package top.kristeal.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Reference;
import top.kristeal.block.SulfurVentBlock;
import top.kristeal.common.SulfurGasCloud;
import top.kristeal.item.exosuit.GasMaskModuleItem;

/**
 * Handles gas mask functionality for ExoSuit helmets
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ExoSuitGasMaskHandler {

    private static final int CHECK_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        handleGasMask(player);
    }

    private static void handleGasMask(Player player) {
        if (hasGasMaskModule(player)) {
            return;
        }

        if (!isPlayerInGasArea(player)) {
            return;
        }

        ItemStack gasMaskUpgrade = findGasMaskModule(player);
        if (!gasMaskUpgrade.isEmpty() && gasMaskUpgrade.getItem() instanceof GasMaskModuleItem gasMaskModule) {
            if (!gasMaskModule.canFunctionWithoutPower()) {
                ExoSuitPowerManager.consumeEnergyForUpgrade(player, "breathing", gasMaskUpgrade);
            }
        }
    }

    private static boolean hasGasMaskModule(Player player) {
        return findGasMaskModule(player).isEmpty();
    }

    private static ItemStack findGasMaskModule(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof top.kristeal.item.animated.ExoSuitItem exosuit &&
                    exosuit.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET) {

                for (int slot = 0; slot < 4; slot++) {
                    ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
                    if (!upgradeItem.isEmpty()) {
                        ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                        if (upgrade != null && upgrade.getType().equals("breathing") &&
                                upgradeItem.getItem() instanceof GasMaskModuleItem) {
                            return upgradeItem;
                        }
                    }
                }
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isPlayerInGasArea(Player player) {
        Vec3 playerPos = player.position();
        return SulfurGasCloud.isInGasEffectArea(player.level(), playerPos, SulfurVentBlock.EFFECT_RADIUS);
    }

    public static boolean hasProtection(Player player) {
        if (hasGasMaskModule(player)) {
            return false;
        }

        ItemStack gasMaskUpgrade = findGasMaskModule(player);
        if (!gasMaskUpgrade.isEmpty() && gasMaskUpgrade.getItem() instanceof GasMaskModuleItem gasMaskModule) {
            if (gasMaskModule.canFunctionWithoutPower()) {
                return true;
            }

            return ExoSuitPowerManager.canUpgradeFunction(player, "breathing");
        }

        return false;
    }
}
