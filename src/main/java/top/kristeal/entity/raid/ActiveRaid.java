package top.kristeal.entity.raid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import top.kristeal.Config;
import top.kristeal.config.RaidConfig;

import javax.annotation.Nullable;
import java.util.*;

/**
 * One running raid in one dimension: a boss (maybe mounted), the henchmen it keeps summoning, and the boss bar.
 * <p>
 * Port notes: a boss that cannot be found after it was confirmed (its chunk is unloaded) no longer ends the raid as won;
 * the raid waits for it and the timeout is the only way out. The chunk that is force-loaded while a restored raid looks for
 * its boss is released again.
 */
public class ActiveRaid
{
    private static final int BOSS_REVALIDATION_INTERVAL = 100;
    private static final int TARGET_UPDATE_INTERVAL = 40;
    private static final int BOSS_VALIDATION_TICKS = 600;

    private final UUID raidId;
    private final RaidConfig.RaidData config;
    private final ServerLevel level;
    private final Vec3 spawnCenter;
    private final long startTime;

    @Nullable private UUID bossUUID;
    @Nullable private UUID mountUUID;
    @Nullable private UUID targetPlayerUUID;
    private final Set<UUID> henchmenUUIDs = new HashSet<>();
    private int spawnTimer;
    private int totalHenchmenSpawned;
    private boolean isActive = true;
    private boolean bossConfirmed;

    private final ServerBossEvent bossBar;
    @Nullable private ChunkPos forcedChunk;
    private int ticksSinceLoad;
    private int ticksSinceLastValidation;
    private int ticksSinceTargetUpdate;
    private int ticksSinceStart;

    public ActiveRaid(RaidConfig.RaidData config, ServerLevel level, Vec3 spawnCenter, long startTime)
    {
        this(UUID.randomUUID(), config, level, spawnCenter, startTime);
    }

    private ActiveRaid(UUID raidId, RaidConfig.RaidData config, ServerLevel level, Vec3 spawnCenter, long startTime)
    {
        this.raidId = raidId;
        this.config = config;
        this.level = level;
        this.spawnCenter = spawnCenter;
        this.startTime = startTime;
        this.spawnTimer = config.henchmen().spawnIntervalTicks();
        this.bossBar = createBossBar();
    }

    public static ActiveRaid restore(RaidSaveData.ActiveRaidData data, RaidConfig.RaidData config, ServerLevel level)
    {
        ActiveRaid raid = new ActiveRaid(data.raidId(), config, level, data.spawnCenter(), data.startTime());
        raid.bossUUID = data.bossUUID();
        raid.mountUUID = data.mountUUID();
        raid.targetPlayerUUID = data.targetPlayerUUID();
        raid.henchmenUUIDs.addAll(data.henchmenUUIDs());
        raid.spawnTimer = data.spawnTimer();
        raid.totalHenchmenSpawned = data.totalSpawned();
        raid.ticksSinceStart = (int) Math.min(Math.max(level.getGameTime() - data.startTime(), 0), Integer.MAX_VALUE);
        return raid;
    }

    private ServerBossEvent createBossBar()
    {
        String bossName = config.boss().customName();
        Component title = bossName == null ? Component.literal("Raid Boss: " + config.raidId()) : RaidManager.parseText(bossName);

        ServerBossEvent bar = new ServerBossEvent(title, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setProgress(1.0f);
        bar.setVisible(true);
        return bar;
    }

    public void tick()
    {
        if (!isActive)
        {
            return;
        }

        ticksSinceLoad++;
        ticksSinceLastValidation++;
        ticksSinceTargetUpdate++;
        ticksSinceStart++;

        int timeoutMinutes = Config.COMMON.raids.raidTimeoutMinutes.get();
        if (timeoutMinutes > 0 && ticksSinceStart >= timeoutMinutes * 60 * 20)
        {
            announceToNearbyPlayers(Component.translatable("raid.scguns.timeout").withStyle(ChatFormatting.RED),
                    Math.max(256.0, config.spawnConditions().searchRadius()));
            endRaid(false);
            return;
        }

        if (!validateBoss())
        {
            return;
        }

        if (ticksSinceLastValidation >= BOSS_REVALIDATION_INTERVAL)
        {
            revalidateRaidState();
            ticksSinceLastValidation = 0;
        }

        if (ticksSinceTargetUpdate >= TARGET_UPDATE_INTERVAL)
        {
            updateMobTargets();
            ticksSinceTargetUpdate = 0;
        }

        LivingEntity boss = getBoss();
        if (boss == null)
        {
            return;
        }
        if (!boss.isAlive())
        {
            endRaid(true);
            return;
        }

        if (mountUUID != null)
        {
            Entity mount = getMount();
            if (mount != null && !mount.isAlive())
            {
                mountUUID = null;
            }
        }

        updateBossBar();

        if (level.getGameTime() % 20 == 0)
        {
            updateBossBarPlayers();
        }

        if (spawnTimer > 0)
        {
            spawnTimer--;
        }
    }

    private void updateMobTargets()
    {
        ServerPlayer targetPlayer = getTargetPlayer(level);
        if (targetPlayer == null || targetPlayer.isRemoved() || targetPlayer.isSpectator() || targetPlayer.isCreative() || !targetPlayer.isAlive())
        {
            targetPlayer = findNewTargetPlayer();
            if (targetPlayer != null)
            {
                targetPlayerUUID = targetPlayer.getUUID();
            }
        }

        if (targetPlayer == null)
        {
            return;
        }

        if (getBoss() instanceof PathfinderMob boss && boss.getTarget() == null)
        {
            aggravate(boss, targetPlayer);
        }

        for (UUID henchmanUUID : henchmenUUIDs)
        {
            if (level.getEntity(henchmanUUID) instanceof PathfinderMob henchman && henchman.getTarget() == null)
            {
                aggravate(henchman, targetPlayer);
            }
        }
    }

    /** Points a raid mob at the player. Piglins run on brain memories, so they need their anger set as well. */
    public static void aggravate(Mob mob, ServerPlayer target)
    {
        mob.setTarget(target);

        if (mob instanceof AbstractPiglin piglin)
        {
            var brain = piglin.getBrain();
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            brain.setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
            brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
            brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
            brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, target);
            piglin.setLastHurtByMob(target);
        }
    }

    @Nullable
    private ServerPlayer findNewTargetPlayer()
    {
        ServerPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ServerPlayer player : level.players())
        {
            if (player.isSpectator() || player.isCreative() || !player.isAlive())
            {
                continue;
            }
            double dist = player.position().distanceTo(spawnCenter);
            if (dist <= config.spawnConditions().searchRadius() && dist < closestDist)
            {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    private boolean validateBoss()
    {
        if (bossUUID == null)
        {
            endRaid(false);
            return false;
        }

        if (bossConfirmed)
        {
            return true;
        }

        LivingEntity boss = getBoss();
        if (boss != null && boss.isAlive())
        {
            bossConfirmed = true;
            ticksSinceLoad = 0;
            releaseForcedChunk();
            return true;
        }

        if (ticksSinceLoad >= BOSS_VALIDATION_TICKS)
        {
            endRaid(false);
            return false;
        }

        if (forcedChunk == null && ticksSinceLoad % 20 == 0)
        {
            ChunkPos chunkPos = new ChunkPos(BlockPos.containing(spawnCenter));
            if (level.setChunkForced(chunkPos.x, chunkPos.z, true))
            {
                forcedChunk = chunkPos;
            }
        }

        return false;
    }

    private void releaseForcedChunk()
    {
        if (forcedChunk != null)
        {
            level.setChunkForced(forcedChunk.x, forcedChunk.z, false);
            forcedChunk = null;
        }
    }

    private void revalidateRaidState()
    {
        henchmenUUIDs.removeIf(uuid -> level.getEntity(uuid) instanceof LivingEntity living && !living.isAlive());
    }

    public void updateBossBarPlayers()
    {
        LivingEntity boss = getBoss();
        if (boss == null || !boss.isAlive())
        {
            return;
        }

        List<ServerPlayer> nearbyPlayers = level.getPlayers(player ->
                player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.position().distanceTo(boss.position()) <= 128);

        for (ServerPlayer player : new ArrayList<>(bossBar.getPlayers()))
        {
            if (!nearbyPlayers.contains(player))
            {
                bossBar.removePlayer(player);
            }
        }

        for (ServerPlayer player : nearbyPlayers)
        {
            bossBar.addPlayer(player); // no-op for a player that already sees the bar
        }
    }

    private void updateBossBar()
    {
        LivingEntity boss = getBoss();
        if (boss != null && boss.isAlive())
        {
            bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, boss.getHealth() / boss.getMaxHealth())));
        }
    }

    public void onBossDefeated()
    {
        for (ServerPlayer player : level.players())
        {
            if (player.position().distanceTo(spawnCenter) <= Math.max(64, config.spawnConditions().searchRadius()))
            {
                top.kristeal.progression.ProgressionTriggers.RAID_WON.get().trigger(player, config.raidId());
            }
        }
        endRaid(true);
    }

    public void endRaid(boolean bossDefeated)
    {
        if (!isActive)
        {
            return;
        }

        isActive = false;
        releaseForcedChunk();

        announceToNearbyPlayers(Component.translatable(bossDefeated ? "raid.scguns.defeated" : "raid.scguns.failed"), 64);

        bossBar.setVisible(false);
        bossBar.removeAllPlayers();

        cleanup(bossUUID, "RaidBoss", bossDefeated);
        for (UUID uuid : new HashSet<>(henchmenUUIDs))
        {
            cleanup(uuid, "RaidHenchman", bossDefeated);
        }
        henchmenUUIDs.clear();
        cleanup(mountUUID, "RaidMount", bossDefeated);
    }

    /** Frees a raid mob from the raid. After a lost raid the mob is removed; after a won one it stays. */
    private void cleanup(@Nullable UUID uuid, String roleTag, boolean bossDefeated)
    {
        if (uuid != null && level.getEntity(uuid) instanceof Mob mob)
        {
            mob.removeTag(roleTag);
            mob.removeTag(RaidManager.memberTag(raidId));

            if (!bossDefeated && mob.isAlive())
            {
                mob.discard();
            }
        }
    }

    /** Ends the raid at once without announcing a result, removing every raid mob that is loaded. */
    public void abort()
    {
        if (!isActive)
        {
            return;
        }

        discard(bossUUID);
        discard(mountUUID);
        for (UUID uuid : henchmenUUIDs)
        {
            discard(uuid);
        }
        henchmenUUIDs.clear();

        isActive = false;
        releaseForcedChunk();
        bossBar.setVisible(false);
        bossBar.removeAllPlayers();
    }

    private void discard(@Nullable UUID uuid)
    {
        if (uuid != null)
        {
            Entity entity = level.getEntity(uuid);
            if (entity != null && entity.isAlive())
            {
                entity.discard();
            }
        }
    }

    public void announceToNearbyPlayers(Component message, double radius)
    {
        for (ServerPlayer player : level.players())
        {
            if (player.position().distanceTo(spawnCenter) <= radius)
            {
                player.sendSystemMessage(message);
            }
        }
    }

    public UUID getRaidId() { return raidId; }
    public RaidConfig.RaidData getConfig() { return config; }
    public ResourceLocation getDimension() { return level.dimension().location(); }
    public Vec3 getSpawnCenter() { return spawnCenter; }
    public boolean isActive() { return isActive; }
    public long getStartTime() { return startTime; }
    public int getSpawnTimer() { return spawnTimer; }
    public int getTotalHenchmenSpawned() { return totalHenchmenSpawned; }
    public ServerBossEvent getBossBar() { return bossBar; }

    public void setBossUUID(UUID bossUUID)
    {
        this.bossUUID = bossUUID;
        this.bossConfirmed = true;
    }

    @Nullable public UUID getBossUUID() { return bossUUID; }
    public void setMountUUID(@Nullable UUID mountUUID) { this.mountUUID = mountUUID; }
    @Nullable public UUID getMountUUID() { return mountUUID; }
    public void setTargetPlayer(UUID playerUUID) { this.targetPlayerUUID = playerUUID; }
    @Nullable public UUID getTargetPlayerUUID() { return targetPlayerUUID; }

    @Nullable
    public ServerPlayer getTargetPlayer(ServerLevel level)
    {
        return targetPlayerUUID == null ? null : level.getServer().getPlayerList().getPlayer(targetPlayerUUID);
    }

    @Nullable
    public LivingEntity getBoss()
    {
        return bossUUID != null && level.getEntity(bossUUID) instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public Entity getMount()
    {
        return mountUUID == null ? null : level.getEntity(mountUUID);
    }

    public boolean isMember(UUID uuid)
    {
        return henchmenUUIDs.contains(uuid);
    }

    public void addHenchman(UUID uuid)
    {
        henchmenUUIDs.add(uuid);
        totalHenchmenSpawned++;
    }

    public void removeHenchman(UUID uuid)
    {
        henchmenUUIDs.remove(uuid);
    }

    public int getAliveHenchmenCount()
    {
        revalidateRaidState();
        return henchmenUUIDs.size();
    }

    public Set<UUID> getHenchmenUUIDs()
    {
        return new HashSet<>(henchmenUUIDs);
    }

    public boolean canSpawnMoreHenchmen()
    {
        int maxAlive = config.henchmen().maxAlive();
        double multiplier = top.kristeal.Config.COMMON.raids.henchmenCountMultiplier.get();
        int cap = multiplier == 1.0 ? maxAlive : Math.max(1, (int) Math.round(maxAlive * multiplier));
        return getAliveHenchmenCount() < cap;
    }

    public boolean shouldSpawnHenchmen()
    {
        return spawnTimer <= 0 && isActive && bossConfirmed && canSpawnMoreHenchmen();
    }

    public void resetSpawnTimer()
    {
        spawnTimer = config.henchmen().spawnIntervalTicks();
    }

    public long getRaidDuration()
    {
        return level.getGameTime() - startTime;
    }
}
