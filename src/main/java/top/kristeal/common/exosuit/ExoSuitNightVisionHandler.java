package top.kristeal.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Reference;
import top.kristeal.item.exosuit.NightVisionModuleItem;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class ExoSuitNightVisionHandler {

    private static final int REFRESH_INTERVAL = 150;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        handleNightVisionEnergy(player);
    }

    private static void handleNightVisionEnergy(Player player) {
        if (!ExoSuitPowerManager.isPowerEnabled(player, "hud")) {
            return;
        }

        if (!hasNightVisionModule(player)) {
            return;
        }

        if (!ExoSuitPowerManager.canConsumeEnergy(player, "hud", REFRESH_INTERVAL)) {
            return;
        }

        if (!ExoSuitPowerManager.canUpgradeFunction(player, "hud")) {
            return;
        }

        ItemStack helmetUpgrade = findNightVisionModule(player);
        if (!helmetUpgrade.isEmpty() &&
                helmetUpgrade.getItem() instanceof NightVisionModuleItem nightVisionModule) {
            if (!nightVisionModule.canFunctionWithoutPower()) {
                ExoSuitPowerManager.consumeEnergyForUpgrade(player, "hud", helmetUpgrade);
            }
        }
    }

    private static boolean hasNightVisionModule(Player player) {
        return !findNightVisionModule(player).isEmpty();
    }

    private static ItemStack findNightVisionModule(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof top.kristeal.item.animated.ExoSuitItem exosuit &&
                    exosuit.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET) {
                for (int slot = 0; slot < 4; slot++) {
                    ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
                    if (!upgradeItem.isEmpty()) {
                        ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                        if (upgrade != null && upgrade.getType().equals("hud") &&
                                upgradeItem.getItem() instanceof NightVisionModuleItem) {
                            return upgradeItem;
                        }
                    }
                }
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void onPlayerLogout(Player player) {
    }
}
