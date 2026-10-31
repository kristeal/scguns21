package top.kristeal.entity.throwable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.kristeal.Config;
import top.kristeal.init.ModEntities;
import top.kristeal.init.ModItems;
import top.kristeal.init.ModParticleTypes;

/**
 * Author: MrCrayfish
 */
public class ThrowableGrenadeEntity extends ThrowableItemEntity
{
    public float rotation;
    public float prevRotation;

    public ThrowableGrenadeEntity(EntityType<? extends ThrowableItemEntity> entityType, Level worldIn)
    {
        super(entityType, worldIn);
    }

    public ThrowableGrenadeEntity(EntityType<? extends ThrowableItemEntity> entityType, Level world, LivingEntity entity)
    {
        super(entityType, world, entity);
        this.setShouldBounce(true);
        this.setGravityVelocity(0.05F);
        this.setItem(new ItemStack(ModItems.GRENADE.get()));
        this.setMaxLife(2 * 2);
    }

    public ThrowableGrenadeEntity(Level world, LivingEntity entity, int timeLeft)
    {
        super(ModEntities.THROWABLE_GRENADE.get(), world, entity);
        this.setShouldBounce(true);
        this.setGravityVelocity(0.05F);
        this.setItem(new ItemStack(ModItems.GRENADE.get()));
        this.setMaxLife(timeLeft);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
    }

    @Override
    public void tick()
    {
        super.tick();
        this.prevRotation = this.rotation;
        double speed = this.getDeltaMovement().length();
        particleTick();
    }

    public void particleTick()
    {
        if (this.level().isClientSide)
        {
            this.level().addParticle(ParticleTypes.SMOKE, true, this.getX(), this.getY() + 0.25, this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public void onDeath()
    {
        float radius = Config.COMMON.grenades.explosionRadius.get().floatValue();
        GrenadeExplosionHelper.createExplosion(this, radius, true);
        spawnExplosionParticle(radius);
    }

    private void spawnExplosionParticle(float radius)
    {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        BlockPos pos = this.blockPosition();
        double y = serverLevel.getBlockState(pos).isAir() ? this.getY() + 0.2 : pos.getY() + 1.0;

        // Count 0 makes the client use the offsets (times speed) as the particle velocity, which the particle reads as its size multiplier.
        // Forced per player so the flash shows beyond the normal 32-block particle range, like the HE round's.
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this.getX(), y, this.getZ()) < 128 * 128) {
                serverLevel.sendParticles(player, ModParticleTypes.GRENADE_EXPLOSION.get(), true, this.getX(), y, this.getZ(), 0, radius / 3.5, 0.0, 0.0, 1.0);
            }
        }
    }

}
