package top.kristeal.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.entity.ai.AIType;
import top.kristeal.entity.ai.GunAttackGoal;
import top.kristeal.init.ModTags;
import top.kristeal.item.GunItem;
import top.kristeal.config.GunnerMobConfig;
import top.kristeal.progression.GunTier;
import top.kristeal.progression.PlayerGunProgression;
import top.kristeal.util.GunCurseUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.List;

/**
 * The reference's config/GunnerMobSpawner, in both progression modes:
 * <ul>
 *     <li><b>Spawning:</b> a mob in {@code #scguns:gunner} that finishes spawning may become a gunner. A "thematic"
 *     gunner ({@code entity/gunner_mobs.json}) rolls its own spawn chance and gets its own weapons and armour. Any other
 *     rolls {@code gunnerSpawnChance} (scaled by difficulty) and takes a gun from the mob tiers of the nearest player's
 *     gun tier ({@code entity/tier_weapons.json}); with {@code eliteChance} it is an elite, with an elite gun and armour
 *     ({@code entity/elite_tiers.json}). The gear is put on two ticks later, after vanilla's own spawn equipment.</li>
 *     <li><b>Goals:</b> any PathfinderMob holding a GunItem (from its spawn, equipment JSON, a command or a dispenser)
 *     gets a GunAttackGoal, and its follow range is extended to 64.</li>
 * </ul>
 * Raid members ("RaidMember_" tags) are skipped: the raid sets their goals.
 * <p>
 * Port notes: LivingEvent.LivingTickEvent became EntityTickEvent.Pre; the follow-range modifier uses a fixed id (the
 * reference made a new random UUID every launch, so permanent modifiers from earlier sessions could never be removed).
 * The reference gave a thematic gunner a gun goal even when it rolled a melee weapon (Hornlins' golden shovel and
 * sword), and gave the goal a different stack from the one in its hand; here only a held gun gets a goal. Its
 * {@code weapon_drop_chance} is applied (the reference read it and never used it).
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunnerMobHandler
{
    private static final ResourceLocation GUN_FOLLOW_RANGE_MODIFIER_ID = Reference.id("gun_follow_range");
    private static final String MOB_GUNNER = "MobGunner";
    private static final String THEMATIC_GUNNER = "ThematicGunner";
    private static final String PROGRESSION_GUNNER = "ProgressionGunner";
    private static final String GUNNER_EQUIPPED = "GunnerEquipped";

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event)
    {
        if (!enabled() || !(event.getEntity() instanceof PathfinderMob mob) || !mob.getType().is(ModTags.Entities.GUNNER))
        {
            return;
        }

        GunnerMobConfig.ThematicGunner thematic = GunnerMobConfig.getThematic(mob.getType());
        if (thematic != null)
        {
            if (mob.getRandom().nextFloat() < thematic.spawnChance())
            {
                mob.addTag(MOB_GUNNER);
                mob.addTag(THEMATIC_GUNNER);
            }
            return;
        }

        Player nearestPlayer = mob.level().getNearestPlayer(mob, 64.0);
        if (nearestPlayer == null)
        {
            return;
        }
        boolean hasWeapons = PlayerGunProgression.getTier(nearestPlayer).getAvailableMobTiers().stream()
                .anyMatch(tier -> !GunnerMobConfig.getTierWeapons(tier.id()).isEmpty());
        if (hasWeapons && mob.getRandom().nextFloat() < gunnerSpawnChance(mob.level().getDifficulty()))
        {
            mob.addTag(MOB_GUNNER);
            mob.addTag(PROGRESSION_GUNNER);
        }
    }

    private static double gunnerSpawnChance(Difficulty difficulty)
    {
        double chance = Config.COMMON.gunnerMobs.gunnerSpawnChance.get();
        if (!Config.COMMON.gunnerMobs.scaleToDifficulty.get())
        {
            return chance;
        }
        return chance * switch (difficulty)
        {
            case PEACEFUL -> 0.5;
            case EASY -> 0.75;
            case NORMAL -> 1.0;
            case HARD -> 1.5;
        };
    }

    private static boolean rollElite(PathfinderMob mob)
    {
        return Config.COMMON.gunnerMobs.eliteSpawning.get() && mob.getRandom().nextFloat() < Config.COMMON.gunnerMobs.eliteChance.get();
    }

    private static void equipThematicGun(PathfinderMob mob)
    {
        GunnerMobConfig.ThematicGunner data = GunnerMobConfig.getThematic(mob.getType());
        Item weapon = data == null ? null : data.getRandomWeapon(mob.getRandom());
        if (weapon == null)
        {
            return;
        }

        boolean elite = rollElite(mob);
        if (elite)
        {
            mob.addTag("EliteGunner");
        }
        equipArmor(mob, data.armor());
        equipWeapon(mob, weapon, elite ? 0.0F : data.weaponDropChance(), data.aiDifficulty() + (elite ? 1 : 0));
    }

    private static void equipProgressionGun(PathfinderMob mob)
    {
        Player nearestPlayer = mob.level().getNearestPlayer(mob, 64.0);
        if (nearestPlayer == null)
        {
            return;
        }

        boolean elite = rollElite(mob);
        List<GunTier> validTiers = PlayerGunProgression.getTier(nearestPlayer).getAvailableMobTiers().stream()
                .filter(tier -> elite ? GunnerMobConfig.getElite(tier.id()) != null : !GunnerMobConfig.getTierWeapons(tier.id()).isEmpty())
                .toList();
        if (validTiers.isEmpty())
        {
            return;
        }

        GunTier tier = validTiers.get(pickTierIndex(validTiers.size(), mob.getRandom()));
        Item gun;
        if (elite)
        {
            GunnerMobConfig.Elite eliteData = GunnerMobConfig.getElite(tier.id());
            gun = eliteData.getRandomWeapon(mob.getRandom());
            if (gun == null)
            {
                return;
            }
            mob.addTag("EliteGunner");
            equipArmor(mob, eliteData.armor());
        }
        else
        {
            List<Item> weapons = GunnerMobConfig.getTierWeapons(tier.id());
            gun = weapons.get(mob.getRandom().nextInt(weapons.size()));
        }
        equipWeapon(mob, gun, elite ? 0.0F : 0.085F, 2 + (elite ? 1 : 0));
    }

    /**
     * The reference's weighting over the mob tiers (most advanced first): 60% from the first half, 30% from the middle
     * third, 10% from the last third.
     */
    private static int pickTierIndex(int size, RandomSource random)
    {
        float roll = random.nextFloat();
        if (roll < 0.6f)
        {
            return random.nextInt(Math.max(1, size / 2));
        }
        if (roll < 0.9f && size > 1)
        {
            int midStart = size / 3;
            int midEnd = (size * 2) / 3;
            if (midEnd <= midStart) midEnd = midStart + 1;
            if (midEnd >= size) midEnd = size - 1;
            return midStart + random.nextInt(midEnd - midStart + 1);
        }
        return Math.max(0, size - 1 - random.nextInt(Math.max(1, size / 3)));
    }

    private static void equipArmor(PathfinderMob mob, List<GunnerMobConfig.ArmorPiece> armor)
    {
        for (GunnerMobConfig.ArmorPiece piece : armor)
        {
            if (mob.getRandom().nextFloat() >= piece.chance())
            {
                continue;
            }
            EquipmentSlot slot = switch (piece.slot())
            {
                case "head" -> EquipmentSlot.HEAD;
                case "chest" -> EquipmentSlot.CHEST;
                case "legs" -> EquipmentSlot.LEGS;
                case "feet" -> EquipmentSlot.FEET;
                default -> null;
            };
            if (slot != null)
            {
                mob.setItemSlot(slot, new ItemStack(piece.item()));
            }
        }
    }

    private static void equipWeapon(PathfinderMob mob, Item weapon, float dropChance, int aiDifficulty)
    {
        ItemStack stack = new ItemStack(weapon);
        if (weapon instanceof GunItem)
        {
            GunCurseUtil.applyCurseIfRoll(stack, mob.getRandom(), mob.level().registryAccess());
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
        mob.setDropChance(EquipmentSlot.MAINHAND, dropChance);
        if (weapon instanceof GunItem)
        {
            AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
            assignGunGoal(mob, aiType, aiDifficulty);
        }
    }

    private static boolean enabled()
    {
        return Config.COMMON.gunnerMobs.gunnerMobSpawning.get(); // the reference's GunMobValues.enabled
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event)
    {
        if (!(event.getEntity() instanceof PathfinderMob mob))
        {
            return;
        }

        ItemStack heldItem = mob.getMainHandItem();

        if (heldItem.getItem() instanceof GunItem)
        {
            reassessWeaponGoal(mob);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event)
    {
        if (!(event.getEntity() instanceof PathfinderMob mob))
        {
            return;
        }

        if (mob instanceof AbstractPiglin abstractPiglin && abstractPiglin.level().dimension() == Level.OVERWORLD)
        {
            if (abstractPiglin.tickCount % 20 == 0)
            {
                ItemStack helmet = abstractPiglin.getItemBySlot(EquipmentSlot.HEAD);
                abstractPiglin.setImmuneToZombification(helmet.is(ModTags.Items.GAS_MASK));
            }
        }

        if (mob.tickCount >= 2)
        {
            return;
        }

        if (!enabled())
        {
            return;
        }

        if (mob.getTags().contains(MOB_GUNNER) && !mob.getTags().contains(GUNNER_EQUIPPED) && !(mob.getMainHandItem().getItem() instanceof GunItem))
        {
            mob.addTag(GUNNER_EQUIPPED); // equip once: tickCount restarts when the mob is loaded again
            if (mob.getTags().contains(THEMATIC_GUNNER))
            {
                equipThematicGun(mob);
            }
            else if (mob.getTags().contains(PROGRESSION_GUNNER))
            {
                equipProgressionGun(mob);
            }
        }

        if (mob.getMainHandItem().getItem() instanceof GunItem)
        {
            reassessWeaponGoal(mob);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event)
    {
        if (!enabled())
        {
            return;
        }

        if (!(event.getEntity() instanceof PathfinderMob mob))
        {
            return;
        }

        mob.removeTag("GunAttackAssigned");

        ItemStack heldItem = mob.getMainHandItem();
        if (heldItem.getItem() instanceof GunItem)
        {
            reassessWeaponGoal(mob);
        }
        else
        {
            resetFollowRange(mob);
        }
    }

    public static boolean hasGunAttackGoal(PathfinderMob mob)
    {
        return mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof GunAttackGoal<?>);
    }

    public static void reassessWeaponGoal(PathfinderMob mob)
    {
        AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
        assignGunGoal(mob, aiType, mob.getRandom().nextInt(4) + 1);
    }

    private static void assignGunGoal(PathfinderMob mob, AIType aiType, int aiDifficulty)
    {
        if (mob.level().isClientSide || hasGunAttackGoal(mob))
        {
            return;
        }
        for (String tag : mob.getTags())
        {
            if (tag.startsWith("RaidMember_"))
            {
                return;
            }
        }

        ItemStack heldItem = mob.getMainHandItem();
        mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, heldItem, 1.2F, aiType, aiDifficulty));
        mob.addTag("GunAttackAssigned");
        extendFollowRange(mob);
    }

    public static void extendFollowRange(PathfinderMob mob)
    {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && !followRange.hasModifier(GUN_FOLLOW_RANGE_MODIFIER_ID))
        {
            double additionalRange = 64 - followRange.getBaseValue();
            followRange.addPermanentModifier(new AttributeModifier(GUN_FOLLOW_RANGE_MODIFIER_ID, additionalRange, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void resetFollowRange(PathfinderMob mob)
    {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null)
        {
            followRange.removeModifier(GUN_FOLLOW_RANGE_MODIFIER_ID);
        }
    }
}
