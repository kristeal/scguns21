package top.kristeal.entity.projectile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import top.kristeal.client.network.RaidFlareBurstHandler;
import top.kristeal.Config;
import top.kristeal.config.RaidConfig;
import top.kristeal.config.RaidFlareConfig;
import top.kristeal.entity.raid.RaidManager;
import top.kristeal.init.ModEntities;
import top.kristeal.network.message.S2CMessageRaidFlareBurst;

import java.util.ArrayList;
import java.util.List;

/**
 * The projectile of the flare pistol. It bursts when it hits something, or when its flare's burst delay runs out, and the
 * burst starts the raid the flare belongs to around the player who fired it.
 */
public class RaidFlareEntity extends ThrowableProjectile
{
    private static final EntityDataAccessor<String> RAID_ID = SynchedEntityData.defineId(RaidFlareEntity.class, EntityDataSerializers.STRING);

    private int ticksExisted = 0;
    private boolean hasBurst = false;

    public RaidFlareEntity(EntityType<? extends RaidFlareEntity> type, Level level)
    {
        super(type, level);
    }

    public RaidFlareEntity(Level level, LivingEntity shooter, String raidId)
    {
        super(ModEntities.RAID_FLARE.get(), shooter, level);
        this.entityData.set(RAID_ID, raidId);
    }

    public String getRaidId()
    {
        return this.entityData.get(RAID_ID);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(RAID_ID, "");
    }

    @Override
    protected double getDefaultGravity()
    {
        return 0.04;
    }

    @Override
    protected void onHit(HitResult result)
    {
        if (!this.level().isClientSide && !hasBurst)
        {
            RaidFlareConfig.FlareData flareData = RaidFlareConfig.getFlareData(getRaidId());
            if (flareData != null)
            {
                performBurst(flareData);
                hasBurst = true;
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        ticksExisted++;

        String raidId = getRaidId();
        if (raidId.isEmpty())
        {
            if (!this.level().isClientSide)
            {
                this.discard();
            }
            return;
        }

        RaidFlareConfig.FlareData flareData = RaidFlareConfig.getFlareData(raidId);
        if (flareData == null)
        {
            if (ticksExisted > 200 && !this.level().isClientSide)
            {
                this.discard();
            }
            return;
        }

        if (this.level().isClientSide && ticksExisted % 2 == 0)
        {
            spawnTrailParticles(flareData);
        }

        if (!this.level().isClientSide && ticksExisted >= flareData.burstDelay() && !hasBurst)
        {
            performBurst(flareData);
            hasBurst = true;
        }

        if (hasBurst && ticksExisted >= flareData.burstDelay() + 40 && !this.level().isClientSide)
        {
            this.discard();
        }

        if (hasBurst || this.onGround())
        {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void spawnTrailParticles(RaidFlareConfig.FlareData flareData)
    {
        for (RaidFlareConfig.ParticleEffect effect : flareData.trailParticles())
        {
            var particle = RaidFlareBurstHandler.particleWithColor(effect.particleType(), effect.color());
            if (particle == null)
            {
                continue;
            }

            for (int i = 0; i < effect.count(); i++)
            {
                double offsetX = (random.nextDouble() - 0.5) * effect.spread();
                double offsetY = (random.nextDouble() - 0.5) * effect.spread();
                double offsetZ = (random.nextDouble() - 0.5) * effect.spread();

                level().addParticle(particle, getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                        offsetX * effect.speed(), offsetY * effect.speed(), offsetZ * effect.speed());
            }
        }
    }

    private void performBurst(RaidFlareConfig.FlareData flareData)
    {
        if (!(level() instanceof ServerLevel serverLevel))
        {
            return;
        }

        ResourceLocation soundId = ResourceLocation.tryParse(flareData.burstSound());
        if (soundId != null)
        {
            BuiltInRegistries.SOUND_EVENT.getOptional(soundId).ifPresent((SoundEvent sound) ->
                    level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.AMBIENT, flareData.burstSoundVolume(), flareData.burstSoundPitch()));
        }

        sendBurstParticlePacket(serverLevel, flareData);

        if (getOwner() instanceof ServerPlayer player)
        {
            if (!Config.COMMON.raids.flareRaids.get())
            {
                player.displayClientMessage(Component.translatable("raid.scguns.flare_raids_disabled"), true);
                return;
            }
            RaidConfig.RaidData config = RaidConfig.getRaidById(flareData.raidId());
            if (config == null)
            {
                player.displayClientMessage(Component.translatable("item.scguns.flare_pistol.no_raid_config"), true);
                return;
            }

            switch (RaidManager.get(serverLevel).startRaidFromPlayer(config, serverLevel, player))
            {
                case ALREADY_ACTIVE -> player.displayClientMessage(Component.translatable("item.scguns.flare_pistol.raid_active"), true);
                case NO_LOCATION -> player.displayClientMessage(Component.translatable("raid.scguns.no_spawn_location"), true);
                case DISABLED -> player.displayClientMessage(Component.translatable("raid.scguns.disabled"), true);
                case FAILED -> player.displayClientMessage(Component.translatable("raid.scguns.cannot_start"), true);
                case STARTED -> {}
            }
        }
    }

    private void sendBurstParticlePacket(ServerLevel level, RaidFlareConfig.FlareData flareData)
    {
        List<S2CMessageRaidFlareBurst.ParticleData> particles = new ArrayList<>();
        for (RaidFlareConfig.ParticleEffect effect : flareData.burstParticles())
        {
            particles.add(new S2CMessageRaidFlareBurst.ParticleData(effect.particleType(), effect.count(), effect.spread(), effect.speed(), effect.color()));
        }

        String patternType = "default";
        double scale = 3.0;
        int repetitions = 1;
        if (flareData.pattern() != null)
        {
            patternType = flareData.pattern().patternType();
            scale = flareData.pattern().scale();
            repetitions = flareData.pattern().repetitions();
        }

        PacketDistributor.sendToPlayersTrackingEntity(this, new S2CMessageRaidFlareBurst(getX(), getY(), getZ(), patternType, scale, repetitions, particles));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("RaidId", getRaidId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RaidId"))
        {
            this.entityData.set(RAID_ID, tag.getString("RaidId"));
        }
    }
}
