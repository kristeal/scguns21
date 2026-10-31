package top.kristeal.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.kristeal.Reference;
import top.kristeal.init.ModEnchantments;
import top.kristeal.init.ModTags;

/**
 * The effects of the reference's CorrodedEnchantment class. The enchantment itself is
 * data/scguns/enchantment/corroded.json; its code effects live here:
 * <ul>
 *     <li>LivingHurtEvent bonus damage against #scguns:bot becomes {@link LivingIncomingDamageEvent}.</li>
 *     <li>Enchantment#doPostAttack (sparks on bots, otherwise a 30% chance of Poison) has no 1.21 counterpart for code
 *     enchantments, so it runs after the damage is dealt ({@link LivingDamageEvent.Post}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class CorrodedEnchantmentHandler
{
    private static final RandomSource RANDOM = RandomSource.create();

    public static boolean isBotEntity(LivingEntity entity)
    {
        return entity.getType().is(ModTags.Entities.BOT);
    }

    public static float getBotDamageBonus(int level)
    {
        return level * 2.0F;
    }

    private static int getAttackerLevel(Object sourceEntity)
    {
        if (sourceEntity instanceof LivingEntity attacker)
        {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty())
            {
                return ModEnchantments.getLevel(weapon, ModEnchantments.CORRODED);
            }
        }
        return 0;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event)
    {
        int corrodedLevel = getAttackerLevel(event.getSource().getEntity());
        if (corrodedLevel > 0 && isBotEntity(event.getEntity()))
        {
            event.setAmount(event.getAmount() + getBotDamageBonus(corrodedLevel));
        }
    }

    @SubscribeEvent
    public static void onPostDamage(LivingDamageEvent.Post event)
    {
        int corrodedLevel = getAttackerLevel(event.getSource().getEntity());
        if (corrodedLevel <= 0)
        {
            return;
        }
        LivingEntity target = event.getEntity();
        if (isBotEntity(target))
        {
            spawnCorrodedParticles(target, corrodedLevel);
        }
        else if (RANDOM.nextFloat() < 0.30F)
        {
            int poisonDuration = 60 + (corrodedLevel * 20);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
        }
    }

    private static void spawnCorrodedParticles(LivingEntity entity, int level)
    {
        if (entity.level() instanceof ServerLevel serverLevel)
        {
            for (int i = 0; i < level * 5; i++)
            {
                double offsetX = (RANDOM.nextDouble() - 0.5) * entity.getBbWidth();
                double offsetY = RANDOM.nextDouble() * entity.getBbHeight();
                double offsetZ = (RANDOM.nextDouble() - 0.5) * entity.getBbWidth();
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX() + offsetX, entity.getY() + offsetY,
                        entity.getZ() + offsetZ, 1, 0, 0, 0, 0.1);
            }
        }
    }
}
