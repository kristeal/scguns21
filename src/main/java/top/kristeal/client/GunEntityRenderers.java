package top.kristeal.client;

import top.kristeal.client.render.entity.GrenadeRenderer;

import top.kristeal.client.render.entity.GrenadeRoundRenderer;
import top.kristeal.client.render.entity.MicroJetRenderer;
import top.kristeal.client.render.entity.RocketRenderer;
import top.kristeal.client.render.entity.ShotballRenderer;
import top.kristeal.client.render.entity.ShulkshotRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.client.render.entity.*;
import top.kristeal.init.ModEntities;

/**
 * Author: MrCrayfish
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class GunEntityRenderers
{
    @SubscribeEvent
    public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event)
    {

        event.registerEntityRenderer(ModEntities.PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.RAID_FLARE.get(), RaidFlareRenderer::new);

        event.registerEntityRenderer(ModEntities.BEARPACK_SHELL_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.PLASMA_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.RAMROD_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.HOG_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BEOWULF_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BLAZE_ROD_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BASIC_BULLET_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.NEEDLE_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.FLECHETTE_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.HARDENED_BULLET_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BUCKSHOT_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.OSBORNE_SLUG_PROJECTILE.get(), ProjectileRenderer::new);

        event.registerEntityRenderer(ModEntities.GRENADE.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.HE_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
        event.registerEntityRenderer(ModEntities.BOUNCY_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
        event.registerEntityRenderer(ModEntities.GAS_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.MICROJET.get(), MicroJetRenderer::new);
        event.registerEntityRenderer(ModEntities.SCULK_CELL.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.FROG_DART_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SHATTER_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SYRINGE_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SHULKSHOT.get(), ShulkshotRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAHG_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.ADVANCED_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GIBBS_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOTBALL_PROJECTILE.get(), ShotballRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_GRENADE.get(), ThrowableGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_STUN_GRENADE.get(), ThrowableGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_MOLOTOV_COCKTAIL.get(), ThrowableItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_HELLFIRE_BOMB.get(), ThrowableItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_BEACON_GRENADE.get(), ThrowableItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_GAS_GRENADE.get(), ThrowableItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_CHOKE_BOMB.get(), ThrowableGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_SWARM_BOMB.get(), ThrowableItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_SHOTBALL.get(), ThrowableShotballRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWABLE_NAIL_BOMB.get(), ThrowableItemRenderer::new);

    }
}
