package top.kristeal.entity.raid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.config.RaidConfig;
import top.kristeal.entity.ai.GunAttackGoal;
import top.kristeal.event.GunnerMobHandler;
import top.kristeal.item.GunItem;
import top.kristeal.progression.PlayerGunProgression;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Runs the raids of one dimension: one raid at a time. A raid starts from a raid flare, a command, or the nightly
 * scheduler: at dusk ({@code warningTime}) a random player with a raid level is picked, warned, and a raid of their
 * level is scheduled; it spawns around them at {@code raidSpawnTime}. Nightly raids follow the progression mode
 * ({@link RaidConfig#nightlyRaidsActive()}).
 * <p>
 * Port notes: the reference picked the nightly target among all players and gave up when that one had no raid level;
 * here it picks among the players that have one. {@code minDaysBetweenRaids} and {@code valid_dimensions} were read but
 * unused in the reference; both apply here. State is per dimension (the reference kept the raid map in a static field shared
 * by every dimension) and a saved raid is restored in whichever dimension it ran. Raid mobs that are still tagged with a
 * raid that no longer exists are removed when they load. Mobs that burn in daylight (zombies, skeletons) get a hidden
 * fire resistance effect, since a flare can start a raid at any time of day. Mount armor goes in the body slot, where
 * 1.21 keeps horse armor.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidManager
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation RAID_HEALTH_MODIFIER_ID = Reference.id("raid_health");
    private static final ResourceLocation RAID_BOSS_HEALTH_MODIFIER_ID = Reference.id("raid_boss_health");
    private static final String MEMBER_TAG_PREFIX = "RaidMember_";

    private static final Map<ResourceLocation, RaidManager> INSTANCES = new HashMap<>();

    public enum StartResult
    {
        STARTED, ALREADY_ACTIVE, NO_LOCATION, DISABLED, FAILED
    }

    @Nullable private ActiveRaid current;
    private boolean needsRestore = true;
    private long lastRollDay = Long.MIN_VALUE;

    public static RaidManager get(ServerLevel level)
    {
        return INSTANCES.computeIfAbsent(level.dimension().location(), k -> new RaidManager());
    }

    public static String memberTag(UUID raidId)
    {
        return MEMBER_TAG_PREFIX + raidId;
    }

    /** Reads a name or message from raid JSON: {@code translation:<key>} is a translatable, anything else is literal text. */
    public static Component parseText(String text)
    {
        return text.startsWith("translation:") ? Component.translatable(text.substring("translation:".length())) : Component.literal(text);
    }

    public boolean hasActiveRaid()
    {
        return current != null && current.isActive();
    }

    public static boolean hasActiveRaidInDimension(ServerLevel level)
    {
        return get(level).hasActiveRaid();
    }

    @Nullable
    public ActiveRaid getCurrentActiveRaid()
    {
        return hasActiveRaid() ? current : null;
    }

    /** Ends the running raid and removes its mobs without a result message. Returns whether there was one. */
    public static boolean stopRaid(ServerLevel level)
    {
        RaidManager manager = get(level);
        ActiveRaid raid = manager.getCurrentActiveRaid();
        if (raid == null)
        {
            return false;
        }

        raid.abort();
        manager.current = null;
        RaidSaveData.get(level).removeActiveRaid(raid.getRaidId());
        return true;
    }

    /** The white flag: the boss and its henchmen leave and the raid counts as neither won nor lost. */
    public static void surrenderRaid(ServerLevel level)
    {
        ActiveRaid raid = get(level).getCurrentActiveRaid();
        if (raid == null)
        {
            return;
        }

        raid.announceToNearbyPlayers(Component.translatable("raid.scguns.surrendered").withStyle(ChatFormatting.YELLOW), 64);
        stopRaid(level);
    }

    private void restoreRaidsFromSave(ServerLevel level)
    {
        RaidSaveData saveData = RaidSaveData.get(level);
        ResourceLocation dimension = level.dimension().location();

        for (RaidSaveData.ActiveRaidData data : saveData.getActiveRaidData())
        {
            if (!data.dimension().equals(dimension))
            {
                continue;
            }

            RaidConfig.RaidData config = RaidConfig.getRaidById(data.configRaidId());
            if (config == null || current != null)
            {
                LOGGER.error("Dropping saved raid {}: {}", data.raidId(), config == null ? "no raid '" + data.configRaidId() + "' is configured" : "the dimension already has a raid");
                saveData.removeActiveRaid(data.raidId());
                continue;
            }

            current = ActiveRaid.restore(data, config, level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            get(level).tick(level);
        }
    }

    public void tick(ServerLevel level)
    {
        if (needsRestore)
        {
            restoreRaidsFromSave(level);
            needsRestore = false;
        }

        checkForNightlyRaid(level);

        ActiveRaid raid = current;
        if (raid == null)
        {
            return;
        }

        if (!raid.isActive())
        {
            finish(level, raid);
            return;
        }

        raid.tick();

        if (raid.isActive() && raid.shouldSpawnHenchmen())
        {
            spawnHenchmen(raid, level);
            raid.resetSpawnTimer();
        }

        if (!raid.isActive())
        {
            finish(level, raid);
        }
        else if (level.getGameTime() % 100 == 0)
        {
            RaidSaveData.get(level).saveActiveRaid(raid);
        }
    }

    private void checkForNightlyRaid(ServerLevel level)
    {
        if (!RaidConfig.nightlyRaidsActive() || level.dimensionType().hasFixedTime())
        {
            return;
        }

        Config.Raids settings = Config.COMMON.raids;
        ResourceLocation dimension = level.dimension().location();
        long dayTime = level.getDayTime() % 24000;
        long day = level.getDayTime() / 24000;
        int warningTime = settings.warningTime.get();
        int spawnTime = settings.raidSpawnTime.get();
        RaidSaveData saveData = RaidSaveData.get(level);

        RaidSaveData.ScheduledRaidData scheduled = saveData.getScheduledRaid(dimension);
        if (scheduled != null && scheduled.scheduledDay() < day)
        {
            saveData.removeScheduledRaid(dimension); // missed: slept through, or the target was offline
            scheduled = null;
        }

        if (dayTime >= warningTime && dayTime < warningTime + 20 && lastRollDay != day)
        {
            lastRollDay = day;
            if (scheduled == null && !hasActiveRaid()
                    && day - saveData.getLastRaidDay(dimension) > settings.minDaysBetweenRaids.get()
                    && level.random.nextFloat() < settings.nightlyRaidChance.get())
            {
                // A spawn time earlier in the day than the warning means the next morning
                scheduleNightlyRaid(level, dimension, spawnTime > warningTime ? day : day + 1, saveData);
            }
            return;
        }

        if (scheduled == null || scheduled.scheduledDay() != day || dayTime < spawnTime || dayTime >= spawnTime + 20 || hasActiveRaid())
        {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(scheduled.targetPlayerUUID());
        RaidConfig.RaidData config = RaidConfig.getRaidById(scheduled.raidId());
        if (player == null || player.isSpectator() || player.level() != level || config == null)
        {
            saveData.removeScheduledRaid(dimension);
            return;
        }

        Vec3 spawnPos = findRaidSpawnLocation(level, player.position());
        if (spawnPos == null)
        {
            return; // try again next tick while the spawn window lasts
        }

        if (startRaid(config, level, spawnPos) == StartResult.STARTED)
        {
            saveData.setLastRaidDay(dimension, day);
        }
        saveData.removeScheduledRaid(dimension);
    }

    private void scheduleNightlyRaid(ServerLevel level, ResourceLocation dimension, long day, RaidSaveData saveData)
    {
        boolean targetCreative = Config.COMMON.raids.targetCreativePlayers.get();
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players())
        {
            if (!player.isSpectator() && (targetCreative || !player.isCreative())
                    && PlayerGunProgression.getRaidLevel(player) > 0)
            {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty())
        {
            return;
        }

        ServerPlayer target = candidates.get(level.random.nextInt(candidates.size()));
        RaidConfig.RaidData raid = selectRaidForLevel(PlayerGunProgression.getRaidLevel(target), dimension, level.random);
        if (raid == null)
        {
            return;
        }

        saveData.scheduleRaid(dimension, target.getUUID(), raid.raidId(), day);
        target.sendSystemMessage(Component.translatable("raid.scguns.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /** Most of the time a raid of the player's highest level, otherwise any unlocked one (reference: 60%). */
    @Nullable
    public static RaidConfig.RaidData selectRaidForLevel(int raidLevel, ResourceLocation dimension, RandomSource random)
    {
        List<RaidConfig.RaidData> available = RaidConfig.getRaidsForLevel(raidLevel).stream()
                .filter(raid -> raid.spawnConditions().allows(dimension)).toList();
        if (available.isEmpty())
        {
            return null;
        }

        int highest = available.stream().mapToInt(RaidConfig.RaidData::raidLevel).max().orElse(raidLevel);
        List<RaidConfig.RaidData> top = available.stream().filter(raid -> raid.raidLevel() == highest).toList();
        if (random.nextFloat() < Config.COMMON.raids.highestLevelBias.get())
        {
            return top.get(random.nextInt(top.size()));
        }
        return available.get(random.nextInt(available.size()));
    }

    /** Drops tonight's scheduled raid in this dimension. Returns whether there was one. */
    public static boolean cancelScheduledRaid(ServerLevel level)
    {
        RaidSaveData saveData = RaidSaveData.get(level);
        boolean had = saveData.getScheduledRaid(level.dimension().location()) != null;
        saveData.removeScheduledRaid(level.dimension().location());
        return had;
    }

    private void finish(ServerLevel level, ActiveRaid raid)
    {
        RaidSaveData.get(level).removeActiveRaid(raid.getRaidId());
        if (current == raid)
        {
            current = null;
        }
    }

    public StartResult startRaidFromPlayer(RaidConfig.RaidData config, ServerLevel level, ServerPlayer player)
    {
        if (hasActiveRaid())
        {
            return StartResult.ALREADY_ACTIVE;
        }

        Vec3 spawnPos = findRaidSpawnLocation(level, player.position());
        if (spawnPos == null)
        {
            return StartResult.NO_LOCATION;
        }

        return startRaid(config, level, spawnPos);
    }

    public StartResult startRaid(RaidConfig.RaidData config, ServerLevel level, Vec3 spawnPos)
    {
        if (!Config.COMMON.raids.raidsEnabled.get())
        {
            return StartResult.DISABLED;
        }
        if (hasActiveRaid())
        {
            return StartResult.ALREADY_ACTIVE;
        }

        ServerPlayer targetPlayer = findNearestPlayer(level, spawnPos);
        ActiveRaid raid = new ActiveRaid(config, level, spawnPos, level.getGameTime());
        if (targetPlayer != null)
        {
            raid.setTargetPlayer(targetPlayer.getUUID());
        }

        current = raid; // before the spawns: a raid mob that joins without a current raid is removed as an orphan
        Mob boss = spawnBoss(raid, level, spawnPos, targetPlayer);
        if (boss == null)
        {
            current = null;
            return StartResult.FAILED;
        }

        raid.setBossUUID(boss.getUUID());
        if (boss.getVehicle() != null)
        {
            raid.setMountUUID(boss.getVehicle().getUUID());
        }

        raid.announceToNearbyPlayers(parseText(config.spawnConditions().announcementMessage()), config.spawnConditions().searchRadius());
        spawnHenchmen(raid, level);
        raid.resetSpawnTimer();

        RaidSaveData.get(level).saveActiveRaid(raid);
        return StartResult.STARTED;
    }

    @Nullable
    private ServerPlayer findNearestPlayer(ServerLevel level, Vec3 pos)
    {
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ServerPlayer player : level.players())
        {
            if (player.isSpectator() || player.isCreative())
            {
                continue;
            }

            double dist = player.position().distanceTo(pos);
            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = player;
            }
        }

        return nearest;
    }

    @Nullable
    private Vec3 findRaidSpawnLocation(ServerLevel level, Vec3 center)
    {
        RandomSource random = level.getRandom();
        int playerY = (int) center.y;
        boolean isUnderground = playerY < 50;

        for (int attempt = 0; attempt < 15; attempt++)
        {
            double angle = random.nextDouble() * Math.PI * 2;
            int min = Config.COMMON.raids.spawnDistanceMin.get();
            int max = Math.max(min, Config.COMMON.raids.spawnDistanceMax.get());
            double distance = min + random.nextDouble() * (max - min);

            BlockPos pos = BlockPos.containing(center.x + Math.cos(angle) * distance, playerY, center.z + Math.sin(angle) * distance);

            BlockPos groundPos;
            if (isUnderground)
            {
                groundPos = findNearestValidCaveSpawn(level, pos, playerY);
                if (groundPos == null)
                {
                    continue;
                }
            }
            else
            {
                groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            }

            if (level.getBlockState(groundPos.below()).isSolid()
                    && level.getBlockState(groundPos).isAir()
                    && level.getBlockState(groundPos.above()).isAir()
                    && level.getBlockState(groundPos.above(2)).isAir())
            {
                return new Vec3(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            }
        }

        return null;
    }

    @Nullable
    private BlockPos findNearestValidCaveSpawn(ServerLevel level, BlockPos center, int playerY)
    {
        for (int yOffset = -5; yOffset <= 5; yOffset++)
        {
            BlockPos checkPos = new BlockPos(center.getX(), playerY + yOffset, center.getZ());

            if (level.getBlockState(checkPos.below()).isSolid()
                    && level.getBlockState(checkPos).isAir()
                    && level.getBlockState(checkPos.above()).isAir()
                    && level.getBlockState(checkPos.above(2)).isAir())
            {
                return checkPos;
            }
        }
        return null;
    }

    @Nullable
    private Mob spawnBoss(ActiveRaid raid, ServerLevel level, Vec3 spawnPos, @Nullable ServerPlayer targetPlayer)
    {
        RaidConfig.BossData bossData = raid.getConfig().boss();

        if (!(bossData.entityType().create(level) instanceof Mob boss))
        {
            return null;
        }

        boss.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Tags first: the gunner handler skips mobs that are raid members when it sees them pick up a gun
        boss.addTag("RaidBoss");
        boss.addTag(memberTag(raid.getRaidId()));
        boss.addTag("MobGunner");
        boss.addTag("AI_" + bossData.aiType().name());
        boss.setPersistenceRequired();

        if (bossData.customName() != null)
        {
            boss.setCustomName(parseText(bossData.customName()));
            boss.setCustomNameVisible(true);
        }

        applyHealthConfig(boss, bossData.healthConfig());
        applyBossHealthMultiplier(boss);
        applyEffects(boss, bossData.effects());
        protectFromSun(boss);

        if (bossData.weapon() != null)
        {
            ItemStack weaponStack = new ItemStack(bossData.weapon().item());
            for (RaidConfig.EnchantEntry enchantEntry : bossData.weapon().enchantments())
            {
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantEntry.id());
                Optional<Holder.Reference<Enchantment>> enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
                if (enchantment.isPresent())
                {
                    weaponStack.enchant(enchantment.get(), enchantEntry.level());
                }
                else
                {
                    LOGGER.error("Raid {}: unknown enchantment {}", raid.getConfig().raidId(), enchantEntry.id());
                }
            }
            boss.setItemSlot(EquipmentSlot.MAINHAND, weaponStack);
            boss.setDropChance(EquipmentSlot.MAINHAND, bossData.weapon().dropChance());
        }

        for (RaidConfig.ArmorEntry armorEntry : bossData.armor())
        {
            EquipmentSlot slot = slotOf(armorEntry.slot(), false);
            if (slot != null)
            {
                boss.setItemSlot(slot, new ItemStack(armorEntry.item()));
                boss.setDropChance(slot, armorEntry.chance());
            }
        }

        if (boss instanceof PathfinderMob pathfinderBoss)
        {
            equipGunGoal(pathfinderBoss, bossData.aiType(), bossData.aiDifficulty());
        }

        if (boss instanceof AbstractPiglin piglin)
        {
            piglin.setImmuneToZombification(true);
            if (piglin instanceof Piglin normalPiglin)
            {
                normalPiglin.setAggressive(true);
            }
        }

        if (targetPlayer != null)
        {
            ActiveRaid.aggravate(boss, targetPlayer);
        }

        level.addFreshEntity(boss);

        RaidConfig.MountData mountData = bossData.mount();
        if (mountData != null)
        {
            Mob mount = spawnMount(raid, mountData, level, spawnPos);
            if (mount != null)
            {
                boss.startRiding(mount, true);
            }
        }

        return boss;
    }

    @Nullable
    private Mob spawnMount(ActiveRaid raid, RaidConfig.MountData mountData, ServerLevel level, Vec3 spawnPos)
    {
        if (!(mountData.entityType().create(level) instanceof Mob mount))
        {
            return null;
        }

        mount.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        applyHealthConfig(mount, mountData.healthConfig());
        applyEffects(mount, mountData.effects());
        protectFromSun(mount);

        for (RaidConfig.ArmorEntry armorEntry : mountData.armor())
        {
            EquipmentSlot slot = slotOf(armorEntry.slot(), true);
            if (slot != null)
            {
                mount.setItemSlot(slot, new ItemStack(armorEntry.item()));
                mount.setDropChance(slot, armorEntry.chance());
            }
        }

        mount.addTag("RaidMount");
        mount.addTag(memberTag(raid.getRaidId()));
        mount.addTag("MobGunner");
        mount.setPersistenceRequired();

        if (!mountData.mountDropsLoot())
        {
            mount.addTag("NoLootDrop");
        }

        level.addFreshEntity(mount);
        return mount;
    }

    private void spawnHenchmen(ActiveRaid raid, ServerLevel level)
    {
        RaidConfig.HenchmenData henchmenData = raid.getConfig().henchmen();
        LivingEntity boss = raid.getBoss();
        if (boss == null || !boss.isAlive())
        {
            return;
        }

        for (int i = 0; i < henchmenData.spawnAttemptsPerWave(); i++)
        {
            if (!raid.canSpawnMoreHenchmen())
            {
                break;
            }

            RaidConfig.HenchmanType type = henchmenData.selectRandomType(level.getRandom());
            if (type == null)
            {
                continue;
            }

            Vec3 spawnPos = findHenchmanSpawnPos(level, boss.position(), henchmenData.spawnRadius());
            if (spawnPos == null)
            {
                continue;
            }

            Mob henchman = spawnHenchman(raid, type, level, spawnPos);
            if (henchman != null)
            {
                raid.addHenchman(henchman.getUUID());
            }
        }
    }

    @Nullable
    private Vec3 findHenchmanSpawnPos(ServerLevel level, Vec3 center, int radius)
    {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < 10; attempt++)
        {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;

            BlockPos pos = BlockPos.containing(center.x + Math.cos(angle) * distance, center.y, center.z + Math.sin(angle) * distance);
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);

            if (level.getBlockState(groundPos.below()).isSolid()
                    && level.getBlockState(groundPos).isAir()
                    && level.getBlockState(groundPos.above()).isAir())
            {
                return new Vec3(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            }
        }

        return null;
    }

    @Nullable
    private Mob spawnHenchman(ActiveRaid raid, RaidConfig.HenchmanType type, ServerLevel level, Vec3 spawnPos)
    {
        if (!(type.entityType().create(level) instanceof Mob henchman))
        {
            return null;
        }

        henchman.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        henchman.addTag("RaidHenchman");
        henchman.addTag(memberTag(raid.getRaidId()));
        henchman.addTag("AI_" + type.aiType().name());
        henchman.addTag("MobGunner");
        henchman.setPersistenceRequired();

        applyHealthConfig(henchman, type.healthConfig());
        applyEffects(henchman, type.effects());
        protectFromSun(henchman);

        if (!type.weapons().isEmpty())
        {
            Item weapon = type.weapons().get(level.random.nextInt(type.weapons().size()));
            henchman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
            henchman.setDropChance(EquipmentSlot.MAINHAND, 0.05f);
        }

        for (RaidConfig.ArmorEntry armorEntry : type.armor())
        {
            if (level.random.nextFloat() > armorEntry.chance())
            {
                continue;
            }

            EquipmentSlot slot = slotOf(armorEntry.slot(), false);
            if (slot != null)
            {
                henchman.setItemSlot(slot, new ItemStack(armorEntry.item()));
                henchman.setDropChance(slot, 0.05f);
            }
        }

        if (henchman instanceof PathfinderMob pathfinderMob)
        {
            equipGunGoal(pathfinderMob, type.aiType(), type.aiDifficulty());
        }

        if (henchman instanceof AbstractPiglin piglin)
        {
            piglin.setImmuneToZombification(true);
            if (piglin instanceof Piglin normalPiglin)
            {
                normalPiglin.setAggressive(true);
            }
        }

        ServerPlayer targetPlayer = raid.getTargetPlayer(level);
        if (targetPlayer != null)
        {
            ActiveRaid.aggravate(henchman, targetPlayer);
        }

        level.addFreshEntity(henchman);
        return henchman;
    }

    private static void equipGunGoal(PathfinderMob mob, top.kristeal.entity.ai.AIType aiType, int aiDifficulty)
    {
        ItemStack heldItem = mob.getMainHandItem();
        if (heldItem.getItem() instanceof GunItem)
        {
            mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, heldItem, 1.2F, aiType, aiDifficulty));
        }
        GunnerMobHandler.extendFollowRange(mob);
    }

    @Nullable
    private static EquipmentSlot slotOf(String name, boolean mount)
    {
        return switch (name)
        {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> mount ? EquipmentSlot.BODY : EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "body" -> EquipmentSlot.BODY;
            default -> null;
        };
    }

    private static void applyHealthConfig(Mob mob, RaidConfig.HealthConfig healthConfig)
    {
        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null)
        {
            return;
        }

        if (healthConfig.useMultiplier())
        {
            healthAttr.addPermanentModifier(new AttributeModifier(RAID_HEALTH_MODIFIER_ID, healthConfig.healthMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        else if (healthConfig.fixedHealth() != null)
        {
            healthAttr.addPermanentModifier(new AttributeModifier(RAID_HEALTH_MODIFIER_ID, healthConfig.fixedHealth() - healthAttr.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
        }

        mob.setHealth(mob.getMaxHealth());
    }

    private static void applyBossHealthMultiplier(Mob boss)
    {
        double multiplier = Config.COMMON.raids.bossHealthMultiplier.get();
        AttributeInstance healthAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null || multiplier == 1.0)
        {
            return;
        }
        healthAttr.addPermanentModifier(new AttributeModifier(RAID_BOSS_HEALTH_MODIFIER_ID, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        boss.setHealth(boss.getMaxHealth());
    }

    private static void applyEffects(Mob mob, List<RaidConfig.EffectEntry> effects)
    {
        for (RaidConfig.EffectEntry effectEntry : effects)
        {
            mob.addEffect(new MobEffectInstance(effectEntry.effect(), effectEntry.duration(), effectEntry.amplifier(), effectEntry.ambient(), effectEntry.visible()));
        }
    }

    private static void protectFromSun(Mob mob)
    {
        if (mob instanceof Zombie || mob instanceof AbstractSkeleton)
        {
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, true, false));
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event)
    {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob))
        {
            return;
        }

        ActiveRaid raid = get(level).getCurrentActiveRaid();
        if (raid == null)
        {
            return;
        }

        if (mob.getUUID().equals(raid.getBossUUID()))
        {
            ResourceLocation lootTable = raid.getConfig().boss().specialLootTable();
            if (lootTable != null)
            {
                dropSpecialLoot(mob, lootTable, level);
            }
            raid.onBossDefeated();
        }
        else if (raid.isMember(mob.getUUID()))
        {
            raid.removeHenchman(mob.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        if (event.getEntity().getTags().contains("NoLootDrop"))
        {
            event.setCanceled(true);
        }
    }

    /** Raid mobs that load after their raid is gone (an unloaded henchman of a finished raid) are removed. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob))
        {
            return;
        }

        RaidManager manager = get(level);
        if (manager.needsRestore)
        {
            return;
        }

        for (String tag : mob.getTags())
        {
            if (tag.startsWith(MEMBER_TAG_PREFIX) && (manager.current == null || !tag.equals(memberTag(manager.current.getRaidId()))))
            {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void dropSpecialLoot(Mob boss, ResourceLocation lootTableId, ServerLevel level)
    {
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableId));

        DamageSource source = boss.getLastDamageSource() != null ? boss.getLastDamageSource() : level.damageSources().generic();
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, boss)
                .withParameter(LootContextParams.ORIGIN, boss.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source);

        if (boss.getKillCredit() instanceof Player player)
        {
            builder.withParameter(LootContextParams.ATTACKING_ENTITY, player).withLuck(player.getLuck());
        }

        lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY))
                .forEach(stack -> level.addFreshEntity(new ItemEntity(level, boss.getX(), boss.getY(), boss.getZ(), stack)));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        for (ServerLevel level : event.getServer().getAllLevels())
        {
            ActiveRaid raid = get(level).getCurrentActiveRaid();
            if (raid != null)
            {
                RaidSaveData.get(level).saveActiveRaid(raid);
            }
        }

        INSTANCES.clear();
    }
}
