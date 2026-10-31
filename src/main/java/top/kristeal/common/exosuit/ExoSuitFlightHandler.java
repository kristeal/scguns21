package top.kristeal.common.exosuit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Reference;
import top.kristeal.item.animated.ExoSuitItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server/common half of the ExoSuit jetpack. Client input, sounds and particles live in
 * {@link top.kristeal.client.handler.ExoSuitFlightClientHandler}; this class must not reference
 * client-only classes, or {@code @EventBusSubscriber} registration crashes dedicated servers.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ExoSuitFlightHandler {

    private static final Map<UUID, Long> lastEnergyConsumptionTime = new HashMap<>();
    private static final long ENERGY_CONSUMPTION_INTERVAL = 1000; // 1 second
    private static final Map<UUID, Boolean> serverJetpackStates = new HashMap<>();


    private static final Map<UUID, Boolean> playerThrustStates = new HashMap<>();
    private static final Map<UUID, Long> lastThrustTime = new HashMap<>();
    private static final long THRUST_TIMEOUT = 100;

    public static void setJetpackActive(Player player, boolean active) {
        serverJetpackStates.put(player.getUUID(), active);
    }

    public static void setPlayerThrusting(Player player, boolean thrusting) {
        UUID playerId = player.getUUID();
        playerThrustStates.put(playerId, thrusting);
        if (thrusting) {
            lastThrustTime.put(playerId, System.currentTimeMillis());
        }
    }

    public static boolean isPlayerThrusting(Player player) {
        UUID playerId = player.getUUID();
        Boolean thrusting = playerThrustStates.get(playerId);
        if (thrusting == null || !thrusting) {
            return false;
        }
        Long lastThrust = lastThrustTime.get(playerId);
        if (lastThrust == null) {
            return false;
        }

        return (System.currentTimeMillis() - lastThrust) < THRUST_TIMEOUT;
    }

    public static boolean isJetpackActiveOnServer(Player player) {
        return serverJetpackStates.getOrDefault(player.getUUID(), false);
    }

    // Client-side flight state, written by ExoSuitFlightClientHandler. onLivingHurt reads it, which
    // only has an effect in singleplayer (integrated server shares these statics), as in 1.20.1.
    public static boolean clientJetpackActive = false;
    public static boolean exoSuitFlightEnabled = false;
    public static boolean wasDescending = false;
    public static long landingProtectionTime = 0;
    private static final long LANDING_PROTECTION_DURATION = 500;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        handleJetpackEnergyConsumption(player);
    }

    private static void handleJetpackEnergyConsumption(Player player) {
        boolean jetpackActive = isJetpackActiveOnServer(player);

        if (!jetpackActive) {
            return;
        }

        if (!hasJetpackModule(player)) {
            setJetpackActive(player, false);
            return;
        }

        boolean utilityEnabled = ExoSuitPowerManager.isPowerEnabled(player, "utility");

        if (!utilityEnabled) {
            setJetpackActive(player, false);
            return;
        }

        boolean canFunction = ExoSuitPowerManager.canUpgradeFunction(player, "utility");
        if (!canFunction) {
            setJetpackActive(player, false);
            return;
        }

        boolean isThrusting = isPlayerThrusting(player);

        if (!isThrusting) {
            return;
        }

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastConsumption = lastEnergyConsumptionTime.get(playerId);

        if (lastConsumption == null || (currentTime - lastConsumption) >= ENERGY_CONSUMPTION_INTERVAL) {
            ItemStack jetpackUpgrade = findJetpackModuleFromPlayer(player);
            if (jetpackUpgrade.isEmpty()) {
                setJetpackActive(player, false);
                return;
            }

            if (!ExoSuitPowerManager.consumeEnergyForUpgrade(player, "utility", jetpackUpgrade)) {
                setJetpackActive(player, false);
                return;
            }

            lastEnergyConsumptionTime.put(playerId, currentTime);
        }
    }

    private static boolean hasJetpackModule(Player player) {
        return !findJetpackModuleFromPlayer(player).isEmpty();
    }

    private static ItemStack findJetpackModuleFromPlayer(Player player) {
        ItemStack chestplate = getEquippedChestplate(player);
        if (chestplate.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return findJetpackModule(chestplate);
    }

    private static ItemStack getEquippedChestplate(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem exosuit &&
                    exosuit.getType() == net.minecraft.world.item.ArmorItem.Type.CHESTPLATE) {
                return armorStack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasExoSuitJetpackSetup(Player player) {
        ItemStack chestplate = player.getInventory().getArmor(2);
        if (!(chestplate.getItem() instanceof ExoSuitItem)) {
            return false;
        }
        ItemStack jetpackUpgrade = findJetpackModule(chestplate);
        if (jetpackUpgrade.isEmpty()) {
            return false;
        }
        ExoSuitUpgrade.Effects totalEffects = ExoSuitEffectsHandler.getTotalEffects(player);

        return totalEffects.hasFlight();
    }

    public static boolean isUsingExoSuitJetpack(Player player) {
        if (player.level().isClientSide) {
            return hasExoSuitJetpackSetup(player) && clientJetpackActive;
        } else {
            return hasExoSuitJetpackSetup(player) && isJetpackActiveOnServer(player);
        }
    }

    private static ItemStack findJetpackModule(ItemStack chestplate) {
        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("utility") && upgrade.getEffects().hasFlight()) {
                    return upgradeItem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getSource().type().msgId().equals("fall")) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - landingProtectionTime < LANDING_PROTECTION_DURATION) {
                event.setCanceled(true);
                return;
            }
            if (isUsingExoSuitJetpack(player) && exoSuitFlightEnabled && wasDescending) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean canJetpackFunction(Player player) {
        if (!ExoSuitPowerManager.isPowerEnabled(player, "utility")) {
            return false;
        }

        if (!ExoSuitPowerManager.canUpgradeFunction(player, "utility")) {
            return false;
        }

        return hasJetpackModule(player);
    }

    public static void onPlayerLogout(Player player) {
        UUID playerId = player.getUUID();
        lastEnergyConsumptionTime.remove(playerId);
        serverJetpackStates.remove(playerId);
        playerThrustStates.remove(playerId);
        lastThrustTime.remove(playerId);
    }
}
