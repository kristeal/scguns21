package top.kristeal.entity.raid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The raids that are running, so they survive a restart. One instance lives in the overworld's data storage and holds the
 * raids of every dimension.
 * <p>
 * Also holds tonight's scheduled nightly raid and the day of the last nightly raid, per dimension.
 * <p>
 * Port notes: the reference saved the last-raid day but never read it; it now enforces {@code minDaysBetweenRaids}.
 * A restored raid always has to confirm its boss again (the boss is not loaded when the level loads), so that flag is not
 * saved.
 */
public class RaidSaveData extends SavedData
{
    private static final String DATA_NAME = "scguns_raid_save_data";
    private static final Factory<RaidSaveData> FACTORY = new Factory<>(RaidSaveData::new, RaidSaveData::load, null);

    private final Map<UUID, ActiveRaidData> activeRaidData = new HashMap<>();
    private final Map<ResourceLocation, ScheduledRaidData> scheduledRaids = new HashMap<>();
    private final Map<ResourceLocation, Long> lastRaidDays = new HashMap<>();

    /** A nightly raid rolled at dusk, spawning around its target at the raid time of the same day. */
    public record ScheduledRaidData(ResourceLocation dimension, UUID targetPlayerUUID, String raidId, long scheduledDay)
    {
        public CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.toString());
            tag.putUUID("TargetPlayer", targetPlayerUUID);
            tag.putString("RaidId", raidId);
            tag.putLong("Day", scheduledDay);
            return tag;
        }

        @Nullable
        public static ScheduledRaidData load(CompoundTag tag)
        {
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null || !tag.hasUUID("TargetPlayer"))
            {
                return null;
            }
            return new ScheduledRaidData(dimension, tag.getUUID("TargetPlayer"), tag.getString("RaidId"), tag.getLong("Day"));
        }
    }

    public record ActiveRaidData(
            UUID raidId,
            String configRaidId,
            ResourceLocation dimension,
            Vec3 spawnCenter,
            long startTime,
            @Nullable UUID bossUUID,
            @Nullable UUID mountUUID,
            @Nullable UUID targetPlayerUUID,
            Set<UUID> henchmenUUIDs,
            int spawnTimer,
            int totalSpawned
    )
    {
        public CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("RaidId", raidId);
            tag.putString("ConfigRaidId", configRaidId);
            tag.putString("Dimension", dimension.toString());
            tag.putDouble("SpawnX", spawnCenter.x);
            tag.putDouble("SpawnY", spawnCenter.y);
            tag.putDouble("SpawnZ", spawnCenter.z);
            tag.putLong("StartTime", startTime);
            if (bossUUID != null)
            {
                tag.putUUID("BossUUID", bossUUID);
            }
            if (mountUUID != null)
            {
                tag.putUUID("MountUUID", mountUUID);
            }
            if (targetPlayerUUID != null)
            {
                tag.putUUID("TargetPlayerUUID", targetPlayerUUID);
            }
            ListTag henchmen = new ListTag();
            for (UUID uuid : henchmenUUIDs)
            {
                henchmen.add(NbtUtils.createUUID(uuid));
            }
            tag.put("Henchmen", henchmen);
            tag.putInt("SpawnTimer", spawnTimer);
            tag.putInt("TotalSpawned", totalSpawned);
            return tag;
        }

        @Nullable
        public static ActiveRaidData load(CompoundTag tag)
        {
            if (!tag.hasUUID("RaidId") || !tag.contains("ConfigRaidId"))
            {
                return null;
            }

            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null)
            {
                return null;
            }

            Set<UUID> henchmen = new HashSet<>();
            for (Tag element : tag.getList("Henchmen", Tag.TAG_INT_ARRAY))
            {
                henchmen.add(NbtUtils.loadUUID(element));
            }

            return new ActiveRaidData(
                    tag.getUUID("RaidId"),
                    tag.getString("ConfigRaidId"),
                    dimension,
                    new Vec3(tag.getDouble("SpawnX"), tag.getDouble("SpawnY"), tag.getDouble("SpawnZ")),
                    tag.getLong("StartTime"),
                    tag.hasUUID("BossUUID") ? tag.getUUID("BossUUID") : null,
                    tag.hasUUID("MountUUID") ? tag.getUUID("MountUUID") : null,
                    tag.hasUUID("TargetPlayerUUID") ? tag.getUUID("TargetPlayerUUID") : null,
                    henchmen,
                    tag.getInt("SpawnTimer"),
                    tag.getInt("TotalSpawned"));
        }
    }

    public static RaidSaveData get(ServerLevel level)
    {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        ListTag raids = new ListTag();
        for (ActiveRaidData data : activeRaidData.values())
        {
            raids.add(data.save());
        }
        tag.put("ActiveRaids", raids);

        ListTag scheduled = new ListTag();
        for (ScheduledRaidData data : scheduledRaids.values())
        {
            scheduled.add(data.save());
        }
        tag.put("ScheduledRaids", scheduled);

        CompoundTag lastDays = new CompoundTag();
        lastRaidDays.forEach((dimension, day) -> lastDays.putLong(dimension.toString(), day));
        tag.put("LastRaidDays", lastDays);
        return tag;
    }

    public static RaidSaveData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        RaidSaveData data = new RaidSaveData();
        for (Tag element : tag.getList("ActiveRaids", Tag.TAG_COMPOUND))
        {
            ActiveRaidData raid = ActiveRaidData.load((CompoundTag) element);
            if (raid != null)
            {
                data.activeRaidData.put(raid.raidId(), raid);
            }
        }
        for (Tag element : tag.getList("ScheduledRaids", Tag.TAG_COMPOUND))
        {
            ScheduledRaidData scheduled = ScheduledRaidData.load((CompoundTag) element);
            if (scheduled != null)
            {
                data.scheduledRaids.put(scheduled.dimension(), scheduled);
            }
        }
        CompoundTag lastDays = tag.getCompound("LastRaidDays");
        for (String key : lastDays.getAllKeys())
        {
            ResourceLocation dimension = ResourceLocation.tryParse(key);
            if (dimension != null)
            {
                data.lastRaidDays.put(dimension, lastDays.getLong(key));
            }
        }
        return data;
    }

    public void saveActiveRaid(ActiveRaid raid)
    {
        activeRaidData.put(raid.getRaidId(), new ActiveRaidData(
                raid.getRaidId(),
                raid.getConfig().raidId(),
                raid.getDimension(),
                raid.getSpawnCenter(),
                raid.getStartTime(),
                raid.getBossUUID(),
                raid.getMountUUID(),
                raid.getTargetPlayerUUID(),
                raid.getHenchmenUUIDs(),
                raid.getSpawnTimer(),
                raid.getTotalHenchmenSpawned()));
        setDirty();
    }

    public void removeActiveRaid(UUID raidId)
    {
        if (activeRaidData.remove(raidId) != null)
        {
            setDirty();
        }
    }

    public Collection<ActiveRaidData> getActiveRaidData()
    {
        return new ArrayList<>(activeRaidData.values());
    }

    @Nullable
    public ScheduledRaidData getScheduledRaid(ResourceLocation dimension)
    {
        return scheduledRaids.get(dimension);
    }

    public void scheduleRaid(ResourceLocation dimension, UUID targetPlayer, String raidId, long day)
    {
        scheduledRaids.put(dimension, new ScheduledRaidData(dimension, targetPlayer, raidId, day));
        setDirty();
    }

    public void removeScheduledRaid(ResourceLocation dimension)
    {
        if (scheduledRaids.remove(dimension) != null)
        {
            setDirty();
        }
    }

    /** The day of the last nightly raid in this dimension, or a very old day when there was none. */
    public long getLastRaidDay(ResourceLocation dimension)
    {
        return lastRaidDays.getOrDefault(dimension, Long.MIN_VALUE / 2);
    }

    public void setLastRaidDay(ResourceLocation dimension, long day)
    {
        lastRaidDays.put(dimension, day);
        setDirty();
    }
}
