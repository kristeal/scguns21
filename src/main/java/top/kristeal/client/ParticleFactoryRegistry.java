package top.kristeal.client;

import top.kristeal.client.particle.AcidBubbleParticle;

import top.kristeal.client.particle.FireBallParticle;
import top.kristeal.client.particle.FireGrenadeExplosionParticle;
import top.kristeal.client.particle.GreenFlameParticle;
import top.kristeal.client.particle.GrenadeExplosionParticle;
import top.kristeal.client.particle.PlasmaExplosionParticle;
import top.kristeal.client.particle.PlasmaRingParticle;
import top.kristeal.client.particle.RocketTrailParticle;
import top.kristeal.client.particle.SonicBlastParticle;
import top.kristeal.client.particle.SoulFireBallParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import top.kristeal.Reference;
import top.kristeal.client.particle.BloodParticle;
import top.kristeal.client.particle.BeowulfImpactParticle;
import top.kristeal.client.particle.RamrodImpactParticle;
import top.kristeal.client.particle.RocketExplosionParticle;
import top.kristeal.client.particle.BulletHoleParticle;
import top.kristeal.client.particle.CasingParticle;
import top.kristeal.client.particle.LaserParticle;
import top.kristeal.client.particle.SulfurDustParticle;
import top.kristeal.client.particle.SulfurSmokeParticle;
import top.kristeal.client.particle.TrailParticle;
import top.kristeal.client.particle.TurretMuzzleFlashParticle;
import top.kristeal.init.ModParticleTypes;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ParticleFactoryRegistry {

    @SubscribeEvent
    public static void onRegisterParticleFactory(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.SULFUR_DUST.get(), SulfurDustParticle.Provider::new);

        event.registerSpecial(ModParticleTypes.BULLET_HOLE.get(), (typeIn, worldIn, x, y, z, xSpeed, ySpeed, zSpeed) -> new BulletHoleParticle(worldIn, x, y, z, typeIn.getDirection(), typeIn.getPos()));
        event.registerSpriteSet(ModParticleTypes.BLOOD.get(), BloodParticle.Factory::new);
        event.registerSpriteSet(ModParticleTypes.TRAIL.get(), TrailParticle.Factory::new);
        event.registerSpriteSet(ModParticleTypes.ROCKET_TRAIL.get(), RocketTrailParticle.Factory::new);
        event.registerSpriteSet(ModParticleTypes.SONIC_BLAST.get(), SonicBlastParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.COPPER_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BRASS_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.IRON_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.DIAMOND_STEEL_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SHULK_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SHELL_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BEARPACK_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.GREEN_FLAME.get(), GreenFlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.PLASMA_RING.get(), PlasmaRingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SULFUR_SMOKE.get(), SulfurSmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAMROD_IMPACT.get(), RamrodImpactParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BEOWULF_IMPACT.get(), BeowulfImpactParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.TURRET_MUZZLE_FLASH.get(), TurretMuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.LASER.get(), LaserParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ACID_BUBBLE.get(), AcidBubbleParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.PLASMA_EXPLOSION.get(), PlasmaExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ROCKET_EXPLOSION.get(), RocketExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.GRENADE_EXPLOSION.get(), GrenadeExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.FIRE_GRENADE_EXPLOSION.get(), FireGrenadeExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.FIREBALL.get(), FireBallParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SOUL_FIREBALL.get(), SoulFireBallParticle.Provider::new);
    }
}
