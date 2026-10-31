package top.kristeal.entity.throwable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import top.kristeal.Config;
import top.kristeal.entity.projectile.ProjectileEntity;
import top.kristeal.init.ModBlocks;
import top.kristeal.interfaces.IExplosionDamageable;
import top.kristeal.world.ProjectileExplosion;

import java.util.List;

/**
 * Explosion helpers used by the throwables. In the 1.20.1 reference these are static methods on the gun
 * {@code ProjectileEntity} (reached through {@code GrenadeEntity.createExplosion(...)}). They live here until the
 * gun projectile system is ported; {@code ProjectileEntity} should then delegate to these instead of duplicating them.
 * <p>
 * The {@code instanceof ProjectileEntity} branches are kept as in the reference: they are always false for thrown items, so
 * thrown explosions use the explosion's own damage source, exactly as in the reference.
 */
public class GrenadeExplosionHelper
{
    /**
     * Creates a projectile explosion for the specified entity.
     *
     * @param entity    The entity to explode
     * @param radius    The amount of radius the entity should deal
     * @param forceNone If true, forces the explosion mode to be NONE instead of config value
     */
    public static void createExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;

        boolean allowBlockRemoval = Config.COMMON.gameplay.griefing.enableBlockRemovalOnExplosions.get() && !forceNone;
        if (allowBlockRemoval && entity instanceof ProjectileEntity projectile) {
            LivingEntity shooter = projectile.getShooter();
            if (shooter != null && !(shooter instanceof Player)) {
                allowBlockRemoval = Config.COMMON.gameplay.griefing.enableMobExplosionBlockRemoval.get();
            }
        }

        Explosion.BlockInteraction mode = allowBlockRemoval ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP;
        Explosion explosion = new ProjectileExplosion(world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius, false, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);

        explosion.getToBlow().forEach(pos ->
        {
            if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                ((IExplosionDamageable) world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
            }
        });

        if (!explosion.interactsWithBlocks()) {
            explosion.clearToBlow();
        }

        for (ServerPlayer player : ((ServerLevel) world).players()) {
            if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4096) {
                player.connection.send(new ClientboundExplodePacket(entity.getX(), entity.getY(), entity.getZ(), radius, explosion.getToBlow(), explosion.getHitPlayers().get(player),
                        explosion.getBlockInteraction(), explosion.getSmallExplosionParticles(), explosion.getLargeExplosionParticles(), explosion.getExplosionSound()));
            }
        }
    }

    public static void createFireExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ?
                entity.damageSources().explosion(entity, projectile.getShooter()) : null;
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;

        Explosion explosion = new ProjectileExplosion(world, entity, source, null,
                entity.getX(), entity.getY(), entity.getZ(), radius * 0.5f, true, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);

        BlockPos centerPos = entity.blockPosition();
        AABB effectArea = new AABB(centerPos).inflate(radius);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, effectArea);

        for (LivingEntity livingEntity : nearbyEntities) {
            double distance = livingEntity.distanceTo(entity);
            if (distance <= radius) {
                livingEntity.igniteForSeconds(8);
                float damage = (float) (4.0F * (1.0 - distance / radius));
                livingEntity.hurt(world.damageSources().inFire(), damage);
            }
        }
    }

    public static void createSoulFireExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ?
                entity.damageSources().explosion(entity, projectile.getShooter()) : null;
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;

        Explosion explosion = new ProjectileExplosion(world, entity, source, null,
                entity.getX(), entity.getY(), entity.getZ(), radius * 0.5f, false, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);
        BlockPos centerPos = entity.blockPosition();
        AABB effectArea = new AABB(centerPos).inflate(radius);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, effectArea);

        for (LivingEntity livingEntity : nearbyEntities) {
            double distance = livingEntity.distanceTo(entity);
            if (distance <= radius) {
                livingEntity.igniteForSeconds(8);

                float damage = (float) (4.0F * (1.0 - distance / radius));
                livingEntity.hurt(world.damageSources().inFire(), damage);
            }
        }

        int radiusInt = (int) Math.ceil(radius);
        int radiusSquared = radiusInt * radiusInt;

        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int z = -radiusInt; z <= radiusInt; z++) {
                BlockPos columnPos = centerPos.offset(x, 0, z);

                if (centerPos.distSqr(columnPos) <= radiusSquared) {
                    for (int y = -radiusInt; y <= radiusInt; y++) {
                        BlockPos pos = centerPos.offset(x, y, z);
                        BlockState stateAtPos = world.getBlockState(pos);
                        BlockState stateBelow = world.getBlockState(pos.below());

                        if (stateAtPos.isAir() &&
                                (stateBelow.isFaceSturdy(world, pos.below(), net.minecraft.core.Direction.UP) ||
                                        !stateBelow.isAir())) {
                            world.setBlock(pos, ModBlocks.FAKE_SOUL_FIRE.get().defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }
}
