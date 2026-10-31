package top.kristeal.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Reference;
import top.kristeal.init.ModEnchantments;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.init.ModTags;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.ExoSuitItem;

/**
 * Movement speed while holding heavy weapons or reloading, and the Lightweight / Swift Sneak bonuses.
 * Port notes: the modifier UUIDs are {@code scguns:} ids; {@code MULTIPLY_BASE} is {@code ADD_MULTIPLIED_BASE};
 * {@code TickEvent.PlayerTickEvent} END is {@code PlayerTickEvent.Post}; enchantment levels come from {@link ModEnchantments#getLevel}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class WeaponMovementEventHandler {
    private static final ResourceLocation HEAVY_WEAPON_MODIFIER_ID = Reference.id("heavy_weapon_speed");
    private static final ResourceLocation RELOAD_SPEED_MODIFIER_ID = Reference.id("reload_speed_penalty");
    private static final ResourceLocation LIGHTWEIGHT_SPEED_MODIFIER_ID = Reference.id("lightweight_speed_bonus");

    private static final double LIGHTWEIGHT_REDUCTION_PER_LEVEL = 0.2D;
    private static final double LIGHTWEIGHT_SPEED_BONUS_PER_LEVEL = 0.05D; // 5% speed increase per level
    private static final double RELOAD_SPEED_PENALTY = 0.75D; // 75% normal speed while reloading
    private static final double LIGHTWEIGHT_RELOAD_BONUS_PER_LEVEL = 0.08D; // 8% speed increase per level
    private static final double SWIFT_SNEAK_RELOAD_BONUS_PER_LEVEL = 0.05D; // 5% speed increase per level

    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20;

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            EquipmentSlot slot = event.getSlot();
            if (slot == EquipmentSlot.MAINHAND ||
                    slot == EquipmentSlot.OFFHAND ||
                    slot == EquipmentSlot.LEGS) {
                updateSpeedAttribute(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateSpeedAttribute(event.getEntity());
        }
    }

    private static void updateSpeedAttribute(Player player) {
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();
        ItemStack legsItem = player.getItemBySlot(EquipmentSlot.LEGS);

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        // Remove existing modifiers
        movementSpeed.removeModifier(HEAVY_WEAPON_MODIFIER_ID);
        movementSpeed.removeModifier(RELOAD_SPEED_MODIFIER_ID);
        movementSpeed.removeModifier(LIGHTWEIGHT_SPEED_MODIFIER_ID);

        boolean hasExoSuitLegs = legsItem.getItem() instanceof ExoSuitItem;
        boolean isReloading = ModSyncedDataKeys.RELOADING.getValue(player);

        boolean holdingGun = (mainHandItem.getItem() instanceof GunItem) || (offHandItem.getItem() instanceof GunItem);

        if (isReloading && !holdingGun) {
            ModSyncedDataKeys.RELOADING.setValue(player, false);
            isReloading = false;
        }

        float mainHandSpeedModifier = getEffectiveSpeedModifier(mainHandItem, hasExoSuitLegs);
        float offHandSpeedModifier = getEffectiveSpeedModifier(offHandItem, hasExoSuitLegs);
        float finalSpeedModifier = Math.min(mainHandSpeedModifier, offHandSpeedModifier);

        ItemStack relevantWeapon = mainHandSpeedModifier < offHandSpeedModifier ? mainHandItem : offHandItem;

        if (finalSpeedModifier < 1.0F && isHeavyWeapon(relevantWeapon)) {
            int lightweightLevel = ModEnchantments.getLevel(relevantWeapon, ModEnchantments.LIGHTWEIGHT);
            double reduction = LIGHTWEIGHT_REDUCTION_PER_LEVEL * lightweightLevel;
            finalSpeedModifier = (float) Math.min(1.0F, finalSpeedModifier + reduction);
        }

        if (finalSpeedModifier != 1.0F) {
            AttributeModifier modifier = new AttributeModifier(
                    HEAVY_WEAPON_MODIFIER_ID,
                    finalSpeedModifier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            movementSpeed.addTransientModifier(modifier);
        }

        if (holdingGun) {
            ItemStack gunWithLightweight = null;
            int maxLightweightLevel = 0;

            if (mainHandItem.getItem() instanceof GunItem) {
                int level = ModEnchantments.getLevel(mainHandItem, ModEnchantments.LIGHTWEIGHT);
                if (level > maxLightweightLevel) {
                    maxLightweightLevel = level;
                    gunWithLightweight = mainHandItem;
                }
            }
            if (offHandItem.getItem() instanceof GunItem) {
                int level = ModEnchantments.getLevel(offHandItem, ModEnchantments.LIGHTWEIGHT);
                if (level > maxLightweightLevel) {
                    maxLightweightLevel = level;
                    gunWithLightweight = offHandItem;
                }
            }

            if (gunWithLightweight != null) {
                double speedBonus = 0.0D;

                if (isHeavyWeapon(gunWithLightweight)) {
                    if (maxLightweightLevel >= 2) {
                        speedBonus = LIGHTWEIGHT_SPEED_BONUS_PER_LEVEL;
                    }
                } else {
                    speedBonus = LIGHTWEIGHT_SPEED_BONUS_PER_LEVEL * maxLightweightLevel;
                }

                if (speedBonus > 0.0D) {
                    AttributeModifier lightweightSpeedModifier = new AttributeModifier(
                            LIGHTWEIGHT_SPEED_MODIFIER_ID,
                            speedBonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    movementSpeed.addTransientModifier(lightweightSpeedModifier);
                }
            }
        }
        if (isReloading && holdingGun) {
            double reloadSpeedModifier = RELOAD_SPEED_PENALTY;
            ItemStack gunItem = mainHandItem.getItem() instanceof GunItem ? mainHandItem : offHandItem;
            int lightweightLevel = ModEnchantments.getLevel(gunItem, ModEnchantments.LIGHTWEIGHT);
            double lightweightBonus = LIGHTWEIGHT_RELOAD_BONUS_PER_LEVEL * lightweightLevel;
            int swiftSneakLevel = ModEnchantments.getLevel(legsItem, Enchantments.SWIFT_SNEAK);
            double swiftSneakBonus = SWIFT_SNEAK_RELOAD_BONUS_PER_LEVEL * swiftSneakLevel;
            reloadSpeedModifier = Math.min(1.0D, reloadSpeedModifier + lightweightBonus + swiftSneakBonus);

            AttributeModifier reloadModifier = new AttributeModifier(
                    RELOAD_SPEED_MODIFIER_ID,
                    reloadSpeedModifier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            movementSpeed.addTransientModifier(reloadModifier);
        }
    }

    private static float getEffectiveSpeedModifier(ItemStack stack, boolean hasExoSuitLegs) {
        if (stack.isEmpty()) return 1.0F;

        if (stack.getItem() instanceof GunItem gunItem) {
            float baseModifier = gunItem.getGunProperties().getGeneral().getSpeedModifier();

            if (hasExoSuitLegs && baseModifier < 1.0F && isHeavyWeapon(stack)) {
                return 1.0F;
            }

            return baseModifier;
        }

        return 1.0F;
    }

    private static boolean isHeavyWeapon(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.is(ModTags.Items.HEAVY_WEAPON);
    }
}
