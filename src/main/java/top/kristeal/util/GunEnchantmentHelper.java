package top.kristeal.util;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.kristeal.cache.HotBarrelCache;
import top.kristeal.common.*;
import top.kristeal.init.ModEffects;
import top.kristeal.init.ModEnchantments;
import top.kristeal.item.GunItem;
import top.kristeal.particles.TrailData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Author: MrCrayfish
 */
public class GunEnchantmentHelper
{
    public static float getChargeDamage(ItemStack weapon, float damage, float chargeProgress) {
        if (!(weapon.getItem() instanceof GunItem gunItem)) {
            return damage;
        }

        Gun modifiedGun = gunItem.getModifiedGun(weapon);
        if (modifiedGun.getGeneral().getFireTimer() <= 0) {
            return damage;
        }

        float minDamagePercent = 0.85f;
        float fullChargeThreshold = 0.95f;

        float effectiveCharge = (chargeProgress >= fullChargeThreshold) ? 1.0f : chargeProgress;

        float normalizedCharge = effectiveCharge < fullChargeThreshold ?
                effectiveCharge / fullChargeThreshold : 1.0f;

        float damageReductionFactor = minDamagePercent +
                (float)(Math.sqrt(normalizedCharge) * (1.0f - minDamagePercent));

        return damage * damageReductionFactor;
    }

    public static int getRealReloadSpeed(ItemStack weapon)
    {
        Gun modifiedGun = ((GunItem) weapon.getItem()).getModifiedGun(weapon);
        if (modifiedGun.getReloads().getReloadType() == ReloadType.MAG_FED)
            return getMagReloadSpeed(weapon);
        else
            return getReloadInterval(weapon);
    }


    public static int getReloadInterval(ItemStack weapon) {
        Gun modifiedGun = ((GunItem) weapon.getItem()).getModifiedGun(weapon);
        ReloadType reloadType = modifiedGun.getReloads().getReloadType();
        int level = ModEnchantments.getLevel(weapon, ModEnchantments.QUICK_HANDS);
        double decreaseFactor = 1 - (0.25 * level);

        if (reloadType == ReloadType.MANUAL) {
            int bulletReloadTime = modifiedGun.getReloads().getReloadTimer();
            double interval = bulletReloadTime * decreaseFactor;
            interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
            return Math.max((int) Math.round(interval), 1);
        }
        else if (reloadType == ReloadType.SINGLE_ITEM) {
            int bulletReloadTime = modifiedGun.getReloads().getReloadTimer();
            double interval = bulletReloadTime * decreaseFactor;
            interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
            return Math.max((int) Math.round(interval), 1);
        }
        else {
            int baseInterval = 10;
            double interval = baseInterval * decreaseFactor;
            interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
            return Math.max((int) Math.round(interval), 1);
        }
    }

    public static int getMagReloadSpeed(ItemStack weapon) {
        Gun modifiedGun = ((GunItem) weapon.getItem()).getModifiedGun(weapon);

        int baseSpeed = modifiedGun.getReloads().getReloadTimer();

        int level = ModEnchantments.getLevel(weapon, ModEnchantments.QUICK_HANDS);
        double decreaseFactor = 1 - (0.25 * level);
        double speed = baseSpeed * decreaseFactor;
        speed = GunModifierHelper.getModifiedReloadSpeed(weapon, speed);
        return Math.max((int) Math.round(speed), 4);
    }

    public static double getAimDownSightSpeed(ItemStack weapon)
    {
        int level = ModEnchantments.getLevel(weapon, ModEnchantments.LIGHTWEIGHT);
        return level > 0 ? 1.2 : 1.0;
    }

    public static double getProjectileSpeedModifier(ItemStack weapon) {
        int acceleratorLevel = ModEnchantments.getLevel(weapon, ModEnchantments.ACCELERATOR);
        int heavyShotLevel = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        double speedModifier = 1.0;
        if (acceleratorLevel > 0) {
            speedModifier += 0.25 * acceleratorLevel;
        }
        if (heavyShotLevel > 0) {
            speedModifier -= 0.10 * heavyShotLevel;
        }
        return Mth.clamp(speedModifier, 0.1, 5.0);
    }

    public static int getRate(ItemStack weapon, Gun modifiedGun) {
        int baseRate = modifiedGun.getGeneral().getRate();
        int triggerFingerLevel = ModEnchantments.getLevel(weapon, ModEnchantments.TRIGGER_FINGER);
        int heavyShotLevel = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        int puncturingLevel = ModEnchantments.getLevel(weapon, ModEnchantments.PUNCTURING);
        float rateModifier = getRateModifier(triggerFingerLevel, heavyShotLevel, puncturingLevel );
        int modifiedRate = Math.round(baseRate * rateModifier);
        modifiedRate = GunModifierHelper.getModifiedRate(weapon, modifiedRate);

        return Math.max(modifiedRate, 1);
    }


    public static float getRecoilModifier(ItemStack weapon) {
        int heavyShotLevel = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        int puncturingLevel = ModEnchantments.getLevel(weapon, ModEnchantments.PUNCTURING);

        float modifier = 1.0f;
        modifier += 0.25f * heavyShotLevel;
        modifier += 0.10f * puncturingLevel;
        return modifier;
    }

    public static float getRecoilModifier(Player player, ItemStack weapon) {
        float baseModifier = getRecoilModifier(weapon);
        if (player != null) {
            baseModifier = getHotBarrelRecoil(player, weapon, baseModifier);
        }

        return baseModifier;
    }

    public static float getKickModifier(ItemStack weapon) {
        int heavyShotLevel = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        return 1.0f + (0.05f * heavyShotLevel);
    }

    public static float getKickModifier(Player player, ItemStack weapon) {
        float baseModifier = getKickModifier(weapon);

        if (player != null) {
            int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
            float kickIncreaseFactor = 1.0f + (hotBarrelLevel / 100.0f) * 0.5f;
            baseModifier *= kickIncreaseFactor;
        }

        return baseModifier;
    }

    private static float getRateModifier(int triggerFingerLevel, int heavyShotLevel, int puncturingLevel) {
        float heavyShotModifier = 1.0f + (0.15f * heavyShotLevel);
        float puncturingModifier = 1.0f + (0.06f * puncturingLevel);
        float triggerFingerModifier = 1.0f - (0.12f * triggerFingerLevel);
        float combinedModifier = heavyShotModifier * puncturingModifier * triggerFingerModifier;
        return Mth.clamp(combinedModifier, 0.5f, 2.0f);
    }
    public static float getHeavyShotDamage(ItemStack weapon, float damage) {
        int level = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        if (level > 0) {
            damage += damage * (0.125F * level);
        }
        return damage;
    }
    public static float getHeavyShotKnockback(ItemStack weapon, float baseKnockback) {
        int level = ModEnchantments.getLevel(weapon, ModEnchantments.HEAVY_SHOT);
        if (level > 0) {
            return baseKnockback + (0.2F * level);
        }
        return baseKnockback;
    }

    public static float getAcceleratorDamage(ItemStack weapon, float damage) {
        int acceleratorLevel = ModEnchantments.getLevel(weapon, ModEnchantments.ACCELERATOR);
        if (acceleratorLevel > 0) {
            damage += damage * (0.06F * acceleratorLevel);
        }
        return damage;
    }

    public static float getHotBarrelDamage(Player player, ItemStack weapon, float baseDamage) {
        int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
        float damageBoost = (hotBarrelLevel / 100.0f) * 0.6f;
        return baseDamage + (baseDamage * damageBoost);
    }


    public static float getHotBarrelRecoil(Player player, ItemStack weapon, float baseRecoil) {
        int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
        float recoilIncreaseFactor = 1.0f + (hotBarrelLevel / 100.0f) * 0.75f;
        return baseRecoil * recoilIncreaseFactor;
    }

    public static float getHotBarrelSpread(Player player, ItemStack weapon, float baseSpread) {
        int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
        float spreadIncrease = (hotBarrelLevel / 100.0f) * 1.5f;
        return baseSpread + (baseSpread * spreadIncrease);
    }

    public static boolean shouldSetOnFire(Player player, ItemStack weapon) {
        int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
        return hotBarrelLevel >= 60;
    }

    public static ParticleOptions getParticle(ItemStack weapon) {
        if (ModEnchantments.has(weapon, ModEnchantments.PUNCTURING)) {
            return ParticleTypes.ENCHANTED_HIT;
        } else if (ModEnchantments.has(weapon, ModEnchantments.HEAVY_SHOT)) {
            return ParticleTypes.MYCELIUM;
        } else if (ModEnchantments.has(weapon, ModEnchantments.ELEMENTAL_POP)) {
            return ParticleTypes.CRIMSON_SPORE;
        }
        return new TrailData(weapon.isEnchanted());
    }

    public static float getPuncturingChance(ItemStack weapon)
    {
        int level = ModEnchantments.getLevel(weapon, ModEnchantments.PUNCTURING);
        return level * 0.05F;
    }

    public static float getPuncturingArmorBypass(ItemStack weapon) {
        int puncturingLevel = ModEnchantments.getLevel(weapon, ModEnchantments.PUNCTURING);
        if (puncturingLevel > 0) {
            return 5.0f * puncturingLevel;
        }
        return 0.0f;
    }
    public static float getPuncturingDamageReduction(ItemStack weapon, LivingEntity target, float damage) {

        return damage;
    }

    public static float getWaterProofDamage(ItemStack weapon, Player player, float damage) {
        int waterProofLevel = ModEnchantments.getLevel(weapon, ModEnchantments.WATER_PROOF);
        if (waterProofLevel > 0 && player != null && player.isUnderWater()) {
            return damage * 1.15f;
        }
        return damage;
    }
    private static final Map<Holder<MobEffect>, Integer> ELEMENTAL_EFFECTS = new HashMap<>(); // Port note: 1.21 effects are holders

    static {
        ELEMENTAL_EFFECTS.put(MobEffects.MOVEMENT_SPEED, 5);
        ELEMENTAL_EFFECTS.put(MobEffects.POISON, 6);
        ELEMENTAL_EFFECTS.put(MobEffects.WITHER, 3);
        ELEMENTAL_EFFECTS.put(MobEffects.HEAL, 6);
        ELEMENTAL_EFFECTS.put(MobEffects.HARM, 6);
        ELEMENTAL_EFFECTS.put(MobEffects.REGENERATION, 5);
        ELEMENTAL_EFFECTS.put(MobEffects.FIRE_RESISTANCE, 5);
        ELEMENTAL_EFFECTS.put(MobEffects.LEVITATION, 3);
        ELEMENTAL_EFFECTS.put(MobEffects.MOVEMENT_SLOWDOWN, 7);
        ELEMENTAL_EFFECTS.put(MobEffects.WEAKNESS, 3);
        ELEMENTAL_EFFECTS.put(MobEffects.ABSORPTION, 3);
        ELEMENTAL_EFFECTS.put(MobEffects.DAMAGE_RESISTANCE, 6);
        ELEMENTAL_EFFECTS.put(MobEffects.INVISIBILITY, 3);
        ELEMENTAL_EFFECTS.put(MobEffects.BLINDNESS, 2);
        ELEMENTAL_EFFECTS.put(MobEffects.HUNGER, 4);
        ELEMENTAL_EFFECTS.put(MobEffects.DIG_SLOWDOWN, 4);
        ELEMENTAL_EFFECTS.put(MobEffects.CONFUSION, 2);
        ELEMENTAL_EFFECTS.put(MobEffects.WATER_BREATHING, 4);
        ELEMENTAL_EFFECTS.put(MobEffects.NIGHT_VISION, 3);
        ELEMENTAL_EFFECTS.put(ModEffects.SULFUR_POISONING, 5);
        ELEMENTAL_EFFECTS.put(ModEffects.BLINDED, 2);
        ELEMENTAL_EFFECTS.put(ModEffects.DEAFENED, 2);
        ELEMENTAL_EFFECTS.put(ModEffects.LACERATED, 4);
    }

    public static void applyElementalPopEffect(ItemStack weapon, LivingEntity target) {
        int enchantmentLevel = ModEnchantments.getLevel(weapon, ModEnchantments.ELEMENTAL_POP);

        if (enchantmentLevel > 0) {
            Random random = new Random();
            for (Map.Entry<Holder<MobEffect>, Integer> entry : ELEMENTAL_EFFECTS.entrySet()) {
                Holder<MobEffect> effect = entry.getKey();
                int baseChance = entry.getValue();
                int finalChance = baseChance + (enchantmentLevel * 3);

                if (random.nextInt(100) < finalChance) {
                    int duration = getRandomEffectDuration(effect, enchantmentLevel, random);
                    int amplifier = getRandomEffectAmplifier(enchantmentLevel, random);

                    target.addEffect(new MobEffectInstance(effect, duration, amplifier));
                    triggerVisualSplashEffect(target, effect);
                    break;
                }
            }
        }
    }

    private static int getRandomEffectDuration(Holder<MobEffect> effect, int enchantmentLevel, Random random) {
        int baseDuration = 60;
        int maxDuration = 200;
        if (effect.value().isInstantenous()) {
            return 1;
        }
        int duration = baseDuration + random.nextInt(maxDuration - baseDuration) + (enchantmentLevel * 20);
        return Math.min(duration, maxDuration);
    }

    private static int getRandomEffectAmplifier(int enchantmentLevel, Random random) {
        int baseAmplifier = 0;
        int maxAmplifier = 2;
        int amplifier = baseAmplifier + random.nextInt(enchantmentLevel + 1);
        return Math.min(amplifier, maxAmplifier);
    }

    private static void triggerVisualSplashEffect(LivingEntity target, Holder<MobEffect> effect) {
        Level level = target.level();
        Vec3 position = target.position();
        int color = effect.value().getColor();
        // Port note: 1.21 ENTITY_EFFECT carries its colour in a ColorParticleOption instead of the velocity arguments
        ColorParticleOption particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.opaque(color));
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
            level.addParticle(particle, position.x + offsetX, position.y + offsetY, position.z + offsetZ, 0.0, 0.0, 0.0);
        }
    }

    public static int getQuickHands(ItemStack stack) {
        return ModEnchantments.getLevel(stack, ModEnchantments.QUICK_HANDS);
    }

    public static int getLightweight(ItemStack stack) {
        return ModEnchantments.getLevel(stack, ModEnchantments.LIGHTWEIGHT);
    }
}