package top.kristeal.common.exosuit;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.kristeal.Reference;
import top.kristeal.item.animated.ExoSuitItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExoSuitEffectsHandler {

    private static final ResourceLocation HELMET_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_helmet_armor");
    private static final ResourceLocation HELMET_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_helmet_toughness");
    private static final ResourceLocation HELMET_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_helmet_knockback");
    private static final ResourceLocation HELMET_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_helmet_speed");

    private static final ResourceLocation CHEST_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_chest_armor");
    private static final ResourceLocation CHEST_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_chest_toughness");
    private static final ResourceLocation CHEST_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_chest_knockback");
    private static final ResourceLocation CHEST_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_chest_speed");

    private static final ResourceLocation LEGS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_legs_armor");
    private static final ResourceLocation LEGS_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_legs_toughness");
    private static final ResourceLocation LEGS_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_legs_knockback");
    private static final ResourceLocation LEGS_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_legs_speed");

    private static final ResourceLocation BOOTS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_boots_armor");
    private static final ResourceLocation BOOTS_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_boots_toughness");
    private static final ResourceLocation BOOTS_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_boots_knockback");
    private static final ResourceLocation BOOTS_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_boots_speed");

    private static final ResourceLocation BODY_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_body_armor");
    private static final ResourceLocation BODY_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_body_toughness");
    private static final ResourceLocation BODY_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_body_knockback");
    private static final ResourceLocation BODY_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "exosuit_body_speed");

    private static final String EXOSUIT_TAG = "ExoSuitEffect";
    private static final Map<UUID, Map<String, Boolean>> activeExoSuitEffects = new HashMap<>();

    public static void applyExoSuitEffects(Player player) {
        removeExoSuitEffects(player);

        Map<String, Boolean> playerEffects = activeExoSuitEffects.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        playerEffects.clear();

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem exosuit) {
                applyArmorPieceEffects(player, armorStack, exosuit, playerEffects);
            }
        }
    }

    public static ExoSuitUpgrade.Effects getTotalEffects(Player player) {
        ExoSuitUpgrade.Effects totalEffects = new ExoSuitUpgrade.Effects();

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem) {
                ExoSuitUpgrade.Effects pieceEffects = calculateTotalEffects(player, armorStack);

                totalEffects.armorBonus += pieceEffects.getArmorBonus();
                totalEffects.armorToughness += pieceEffects.getArmorToughness();
                totalEffects.knockbackResistance += pieceEffects.getKnockbackResistance();
                totalEffects.speedModifier += pieceEffects.getSpeedModifier();
                totalEffects.jumpBoost += pieceEffects.getJumpBoost();
                totalEffects.fallDamageReduction += pieceEffects.getFallDamageReduction();
                totalEffects.nightVision = totalEffects.nightVision || pieceEffects.hasNightVision();
                totalEffects.flight = totalEffects.flight || pieceEffects.hasFlight();

                if (pieceEffects.getFlightSpeed() > totalEffects.flightSpeed) {
                    totalEffects.flightSpeed = pieceEffects.getFlightSpeed();
                }

                totalEffects.recoilAngleReduction += pieceEffects.getRecoilAngleReduction();
                totalEffects.recoilKickReduction += pieceEffects.getRecoilKickReduction();
                totalEffects.spreadReduction += pieceEffects.getSpreadReduction();
            }
        }

        return totalEffects;
    }

    public static void removeExoSuitEffects(Player player) {
        removeAttributeModifier(player, Attributes.ARMOR, HELMET_ARMOR_ID);
        removeAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, HELMET_TOUGHNESS_ID);
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, HELMET_KNOCKBACK_ID);
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, HELMET_SPEED_ID);

        removeAttributeModifier(player, Attributes.ARMOR, CHEST_ARMOR_ID);
        removeAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, CHEST_TOUGHNESS_ID);
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, CHEST_KNOCKBACK_ID);
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, CHEST_SPEED_ID);

        removeAttributeModifier(player, Attributes.ARMOR, LEGS_ARMOR_ID);
        removeAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, LEGS_TOUGHNESS_ID);
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, LEGS_KNOCKBACK_ID);
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, LEGS_SPEED_ID);

        removeAttributeModifier(player, Attributes.ARMOR, BOOTS_ARMOR_ID);
        removeAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, BOOTS_TOUGHNESS_ID);
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, BOOTS_KNOCKBACK_ID);
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, BOOTS_SPEED_ID);

        removeAttributeModifier(player, Attributes.ARMOR, BODY_ARMOR_ID);
        removeAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS_ID);
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK_ID);
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED_ID);

        Map<String, Boolean> playerEffects = activeExoSuitEffects.get(player.getUUID());
        if (playerEffects == null) {
            return;
        }

        if (Boolean.TRUE.equals(playerEffects.get("nightVision"))) {
            removeExoSuitEffect(player, MobEffects.NIGHT_VISION);
        }

        if (Boolean.TRUE.equals(playerEffects.get("jumpBoost"))) {
            removeExoSuitEffect(player, MobEffects.JUMP);
        }

        if (Boolean.TRUE.equals(playerEffects.get("waterBreathing"))) {
            removeExoSuitEffect(player, MobEffects.WATER_BREATHING);
        }

        playerEffects.clear();
    }

    private static void removeExoSuitEffect(Player player, Holder<MobEffect> effect) {
        if (!player.hasEffect(effect)) {
            return;
        }

        MobEffectInstance currentEffect = player.getEffect(effect);
        if (currentEffect == null) {
            return;
        }

        int maxDuration = effect.equals(MobEffects.NIGHT_VISION) ? 400 : 200;

        if (currentEffect.getDuration() <= maxDuration && !currentEffect.isAmbient() && !currentEffect.isVisible() && currentEffect.getAmplifier() <= 1) {
            player.removeEffect(effect);
        }
    }

    private static void applyArmorPieceEffects(Player player, ItemStack armorStack, ExoSuitItem exosuit, Map<String, Boolean> playerEffects) {
        ExoSuitUpgrade.Effects totalEffects = calculateTotalEffects(player, armorStack);

        ResourceLocation[] ids = getIdsForArmorType(exosuit.getType());
        ResourceLocation armorId = ids[0];
        ResourceLocation toughnessId = ids[1];
        ResourceLocation knockbackId = ids[2];
        ResourceLocation speedId = ids[3];

        if (totalEffects.getArmorBonus() > 0) {
            addAttributeModifier(player, Attributes.ARMOR, armorId,
                    totalEffects.getArmorBonus(),
                    AttributeModifier.Operation.ADD_VALUE);
        }

        if (totalEffects.getArmorToughness() > 0) {
            addAttributeModifier(player, Attributes.ARMOR_TOUGHNESS, toughnessId,
                    totalEffects.getArmorToughness(),
                    AttributeModifier.Operation.ADD_VALUE);
        }

        if (totalEffects.getKnockbackResistance() > 0) {
            addAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, knockbackId,
                    totalEffects.getKnockbackResistance(),
                    AttributeModifier.Operation.ADD_VALUE);
        }

        if (totalEffects.getSpeedModifier() != 0) {
            addAttributeModifier(player, Attributes.MOVEMENT_SPEED, speedId,
                    totalEffects.getSpeedModifier(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        if (totalEffects.hasNightVision() && shouldApplyNightVision(player, armorStack)) {
            applyExoSuitEffect(player, MobEffects.NIGHT_VISION, 400, 0);
            playerEffects.put("nightVision", true);
        }

        if (totalEffects.getJumpBoost() > 0 && shouldApplyJumpBoost(player, armorStack)) {
            int amplifier = Math.max(0, (int) (totalEffects.getJumpBoost() * 5) - 1);
            applyExoSuitEffect(player, MobEffects.JUMP, 200, amplifier);
            playerEffects.put("jumpBoost", true);
        }

        if (shouldApplyWaterBreathing(player, armorStack)) {
            applyExoSuitEffect(player, MobEffects.WATER_BREATHING, 200, 0);
            playerEffects.put("waterBreathing", true);
        }
    }

    private static void applyExoSuitEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance currentEffect = player.getEffect(effect);

        if (currentEffect == null || currentEffect.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
        }
    }

    private static boolean shouldApplyNightVision(Player player, ItemStack armorStack) {
        if (!(armorStack.getItem() instanceof ExoSuitItem exosuit) ||
                exosuit.getType() != net.minecraft.world.item.ArmorItem.Type.HELMET) {
            return false;
        }

        if (!ExoSuitPowerManager.isPowerEnabled(player, "hud")) {
            return false;
        }

        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("hud") &&
                        upgradeItem.getItem() instanceof top.kristeal.item.exosuit.NightVisionModuleItem) {

                    return ExoSuitPowerManager.canUpgradeFunction(player, "hud");
                }
            }
        }
        return false;
    }

    private static boolean shouldApplyJumpBoost(Player player, ItemStack armorStack) {
        if (!(armorStack.getItem() instanceof ExoSuitItem exosuit) ||
                exosuit.getType() != net.minecraft.world.item.ArmorItem.Type.BOOTS) {
            return false;
        }

        if (!ExoSuitPowerManager.isPowerEnabled(player, "mobility")) {
            return false;
        }

        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("mobility")) {
                    return ExoSuitPowerManager.canUpgradeFunction(player, "mobility");
                }
            }
        }
        return false;
    }

    private static boolean shouldApplyWaterBreathing(Player player, ItemStack armorStack) {
        if (!(armorStack.getItem() instanceof ExoSuitItem exosuit) ||
                exosuit.getType() != net.minecraft.world.item.ArmorItem.Type.HELMET) {
            return false;
        }

        if (!player.isInWater() && !player.isUnderWater()) {
            return false;
        }

        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("breathing") &&
                        upgradeItem.getItem() instanceof top.kristeal.item.exosuit.RebreatherModuleItem) {
                    return ExoSuitPowerManager.canUpgradeFunction(player, "breathing");
                }
            }
        }
        return false;
    }

    private static ExoSuitUpgrade.Effects calculateTotalEffects(Player player, ItemStack armorStack) {
        ExoSuitUpgrade.Effects totalEffects = new ExoSuitUpgrade.Effects();
        String slotContext = getSlotContextForArmorType(armorStack);

        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = getUpgradeForSlotContext(upgradeItem, slot, slotContext);

                if (upgrade != null) {
                    ExoSuitUpgrade.Effects effects = upgrade.getEffects();
                    if (requiresPowerAndEnabled(player, upgrade, upgradeItem)) {
                        totalEffects.armorBonus += effects.getArmorBonus();
                        totalEffects.armorToughness += effects.getArmorToughness();
                        totalEffects.knockbackResistance += effects.getKnockbackResistance();
                        totalEffects.speedModifier += effects.getSpeedModifier();
                        totalEffects.jumpBoost += effects.getJumpBoost();
                        totalEffects.fallDamageReduction += effects.getFallDamageReduction();
                        totalEffects.nightVision = totalEffects.nightVision || effects.hasNightVision();
                        totalEffects.flight = totalEffects.flight || effects.hasFlight();
                        if (effects.getFlightSpeed() > totalEffects.flightSpeed) {
                            totalEffects.flightSpeed = effects.getFlightSpeed();
                        }
                    } else if (!isEnergyUpgrade(upgradeItem)) {
                        totalEffects.armorBonus += effects.getArmorBonus();
                        totalEffects.armorToughness += effects.getArmorToughness();
                        totalEffects.knockbackResistance += effects.getKnockbackResistance();
                        totalEffects.speedModifier += effects.getSpeedModifier();
                        totalEffects.jumpBoost += effects.getJumpBoost();
                        totalEffects.fallDamageReduction += effects.getFallDamageReduction();
                        totalEffects.flight = totalEffects.flight || effects.hasFlight();
                        if (effects.getFlightSpeed() > totalEffects.flightSpeed) {
                            totalEffects.flightSpeed = effects.getFlightSpeed();
                        }
                    }
                }
            }
        }

        return totalEffects;
    }

    private static ExoSuitUpgrade getUpgradeForSlotContext(ItemStack upgradeItem, int slotIndex, String armorContext) {
        String slotType = determineSlotType(armorContext, slotIndex);

        ExoSuitUpgrade slotSpecific = ExoSuitUpgradeManager.getUpgradeForItemInSlot(upgradeItem, slotType);
        if (slotSpecific != null) {
            return slotSpecific;
        }
        return ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
    }

    private static String determineSlotType(String armorContext, int slotIndex) {
        return switch (armorContext) {
            case "helmet" -> switch (slotIndex) {
                case 0 -> "plating";
                case 1 -> "hud";
                case 2 -> "breathing";
                default -> "unknown";
            };
            case "chestplate" -> switch (slotIndex) {
                case 0 -> "plating";
                case 1 -> "pauldron";
                case 2 -> "power_core";
                case 3 -> "utility";
                default -> "unknown";
            };
            case "leggings" -> switch (slotIndex) {
                case 0 -> "plating";
                case 1 -> "knee_guard";
                case 2 -> "utility";
                default -> "unknown";
            };
            case "boots" -> switch (slotIndex) {
                case 0 -> "plating";
                case 1 -> "mobility";
                default -> "unknown";
            };
            default -> "unknown";
        };
    }

    private static String getSlotContextForArmorType(ItemStack armorStack) {
        if (armorStack.getItem() instanceof ExoSuitItem exosuit) {
            return switch (exosuit.getType()) {
                case HELMET -> "helmet";
                case CHESTPLATE -> "chestplate";
                case LEGGINGS -> "leggings";
                case BOOTS -> "boots";
                case BODY -> "body";
            };
        }
        return "unknown";
    }

    private static boolean requiresPowerAndEnabled(Player player, ExoSuitUpgrade upgrade, ItemStack upgradeItem) {
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

    private static boolean isEnergyUpgrade(ItemStack upgradeItem) {
        return upgradeItem.getItem() instanceof top.kristeal.item.exosuit.EnergyUpgradeItem;
    }

    private static ResourceLocation[] getIdsForArmorType(net.minecraft.world.item.ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> new ResourceLocation[]{HELMET_ARMOR_ID, HELMET_TOUGHNESS_ID, HELMET_KNOCKBACK_ID, HELMET_SPEED_ID};
            case CHESTPLATE -> new ResourceLocation[]{CHEST_ARMOR_ID, CHEST_TOUGHNESS_ID, CHEST_KNOCKBACK_ID, CHEST_SPEED_ID};
            case LEGGINGS -> new ResourceLocation[]{LEGS_ARMOR_ID, LEGS_TOUGHNESS_ID, LEGS_KNOCKBACK_ID, LEGS_SPEED_ID};
            case BOOTS -> new ResourceLocation[]{BOOTS_ARMOR_ID, BOOTS_TOUGHNESS_ID, BOOTS_KNOCKBACK_ID, BOOTS_SPEED_ID};
            case BODY -> new ResourceLocation[]{BODY_ARMOR_ID, BODY_TOUGHNESS_ID, BODY_KNOCKBACK_ID, BODY_SPEED_ID};
        };
    }

    private static void addAttributeModifier(Player player, Holder<Attribute> attribute,
                                             ResourceLocation id, double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            if (instance.getModifier(id) == null) {
                AttributeModifier modifier = new AttributeModifier(id, value, operation);
                instance.addPermanentModifier(modifier);
            }
        }
    }

    private static void removeAttributeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    public static void cleanupPlayerData(UUID playerId) {
        activeExoSuitEffects.remove(playerId);
    }
}
