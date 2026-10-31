package top.kristeal.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.kristeal.init.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WeirdFleshItem extends Item {

    // 1.21: effects are keyed by Holder<MobEffect> (MobEffects.* are holders; ModEffects are DeferredHolders)
    private static final Map<Holder<MobEffect>, EffectData> WEIRD_FLESH_EFFECTS = new HashMap<>();

    static {
        WEIRD_FLESH_EFFECTS.put(MobEffects.POISON, new EffectData(20, 100, 200, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.CONFUSION, new EffectData(18, 100, 180, 0, 0));
        WEIRD_FLESH_EFFECTS.put(MobEffects.WEAKNESS, new EffectData(15, 80, 160, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.MOVEMENT_SLOWDOWN, new EffectData(15, 60, 140, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.HUNGER, new EffectData(25, 100, 300, 0, 2));
        WEIRD_FLESH_EFFECTS.put(MobEffects.BLINDNESS, new EffectData(12, 60, 120, 0, 0));
        WEIRD_FLESH_EFFECTS.put(MobEffects.WITHER, new EffectData(10, 40, 100, 0, 1));
        WEIRD_FLESH_EFFECTS.put(ModEffects.SULFUR_POISONING, new EffectData(12, 60, 140, 0, 1));
        WEIRD_FLESH_EFFECTS.put(ModEffects.BLINDED, new EffectData(10, 40, 100, 0, 0));
        WEIRD_FLESH_EFFECTS.put(ModEffects.DEAFENED, new EffectData(10, 40, 100, 0, 0));
        WEIRD_FLESH_EFFECTS.put(ModEffects.LACERATED, new EffectData(8, 80, 160, 0, 1));


        WEIRD_FLESH_EFFECTS.put(MobEffects.LEVITATION, new EffectData(8, 40, 80, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.DIG_SLOWDOWN, new EffectData(10, 60, 120, 0, 1));

        WEIRD_FLESH_EFFECTS.put(MobEffects.REGENERATION, new EffectData(5, 60, 120, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.DAMAGE_RESISTANCE, new EffectData(4, 60, 140, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.MOVEMENT_SPEED, new EffectData(6, 100, 200, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.ABSORPTION, new EffectData(3, 80, 160, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.FIRE_RESISTANCE, new EffectData(4, 100, 200, 0, 0));
        WEIRD_FLESH_EFFECTS.put(MobEffects.WATER_BREATHING, new EffectData(4, 100, 200, 0, 0));
        WEIRD_FLESH_EFFECTS.put(MobEffects.NIGHT_VISION, new EffectData(5, 100, 300, 0, 0));

        WEIRD_FLESH_EFFECTS.put(MobEffects.HEAL, new EffectData(2, 1, 1, 0, 1));
        WEIRD_FLESH_EFFECTS.put(MobEffects.HARM, new EffectData(3, 1, 1, 0, 1));
    }

    public WeirdFleshItem() {
        super(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(2)
                        .saturationModifier(0.1F)
                        // 1.21: FoodProperties.Builder#meat() was removed (meat is now the minecraft:meat item tag)
                        .alwaysEdible()
                        .build())
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide()) {
            applyRandomEffect(entity);
        }

        return result;
    }

    private void applyRandomEffect(LivingEntity entity) {
        Random random = new Random();

        int totalWeight = 0;
        for (EffectData data : WEIRD_FLESH_EFFECTS.values()) {
            totalWeight += data.weight;
        }

        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<Holder<MobEffect>, EffectData> entry : WEIRD_FLESH_EFFECTS.entrySet()) {
            currentWeight += entry.getValue().weight;
            if (roll < currentWeight) {
                Holder<MobEffect> effect = entry.getKey();
                EffectData data = entry.getValue();

                int duration = data.getDuration(random);
                int amplifier = data.getAmplifier(random);

                entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
                break;
            }
        }
    }

        private record EffectData(int weight, int minDuration, int maxDuration, int minAmplifier, int maxAmplifier) {

        int getDuration(Random random) {
                if (minDuration == maxDuration) return minDuration;
                return minDuration + random.nextInt(maxDuration - minDuration + 1);
            }

            int getAmplifier(Random random) {
                if (minAmplifier == maxAmplifier) return minAmplifier;
                return minAmplifier + random.nextInt(maxAmplifier - minAmplifier + 1);
            }
        }
}