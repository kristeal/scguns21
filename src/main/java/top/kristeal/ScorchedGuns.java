package top.kristeal;

import top.kristeal.world.VillageStructures;

import net.minecraft.world.entity.Entity;

import top.kristeal.entity.throwable.GrenadeEntity;

import top.kristeal.init.ModFluids;
import top.kristeal.util.ModCauldronInteraction;

import top.kristeal.entity.projectile.*;
import top.kristeal.attributes.SCAttributes;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kristeal.client.screen.ModMenuTypes;
import top.kristeal.common.BoundingBoxManager;
import top.kristeal.common.ProjectileManager;
import top.kristeal.common.exosuit.ExoSuitUpgradeManager;
import top.kristeal.init.ModArmorMaterials;
import top.kristeal.init.*;
import top.kristeal.network.PacketHandler;

import static top.kristeal.Reference.MOD_ID;

@Mod(MOD_ID)
public class ScorchedGuns {
    public static final String MODID = "scguns";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean playerReviveLoaded;
    public static boolean soulFiredLoaded;

    //Some compatibility mods were dropped as i dont have time to port them all, or didnt have a 1.21.1 port.


    public ScorchedGuns(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.serverSpec);

        ModItems.registerItems();
        ModItems.REGISTER.register(modEventBus);
        ModBlocks.REGISTER.register(modEventBus);
        ModParticleTypes.REGISTER.register(modEventBus);
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModEffects.REGISTER.register(modEventBus);
        ModEntities.REGISTER.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.REGISTER.register(modEventBus);
        modEventBus.addListener(PacketHandler::register);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(top.kristeal.research.ResearchCommand::onRegisterCommands); // /scguns research
        NeoForge.EVENT_BUS.addListener(top.kristeal.entity.raid.RaidCommand::onRegisterCommands); // /scguns raid
        NeoForge.EVENT_BUS.addListener(top.kristeal.progression.ProgressionCommand::onRegisterCommands); // /scguns progression
        // Gun tiers (player data) and the progression-mode advancement trigger
        top.kristeal.progression.PlayerGunProgression.register(modEventBus);
        top.kristeal.progression.ProgressionModeTrigger.register(modEventBus);
        top.kristeal.progression.ProgressionTriggers.register(modEventBus); // gun_tier, raid_won
        top.kristeal.progression.BlueprintLootCondition.register(modEventBus); // Research-mode blueprint loot
        ModRecipes.register(modEventBus);
        // Item components, synced player state, gun attributes
        ModDataComponents.REGISTER.register(modEventBus);
        top.kristeal.research.ResearchItemPredicate.register(modEventBus); // Advancement predicate
        ModSyncedDataKeys.REGISTER.register(modEventBus);
        SCAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup); // Projectile factories, improved hitboxes
        // Village gunsmith houses, and the two compat flags the port uses
        NeoForge.EVENT_BUS.addListener(VillageStructures::addNewVillageBuilding);
        playerReviveLoaded = ModList.get().isLoaded("playerrevive");
        soulFiredLoaded = ModList.get().isLoaded("soul_fire_d");
        ModCreativeModeTabs.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus); // Vicious acid
        ModFluids.FLUIDS.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
    }

    public static void setSoulFireOnEntity(Entity entity, int seconds) {
        if (soulFiredLoaded) {
            try {
                it.crystalnest.prometheus.api.FireManager.setOnFire(entity, seconds,
                        it.crystalnest.prometheus.api.FireManager.SOUL_FIRE_TYPE);
                return;
            } catch (Exception e) {
                // fall through to normal fire, as the reference
            }
        }
        entity.igniteForSeconds(seconds);
    }

    // The projectile part of the reference's onCommonSetup: one factory per ammo item.
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModCauldronInteraction.register(); // Vicious acid

            //Regular shells
            ProjectileManager.getInstance().registerFactory(ModItems.POWDER_AND_BALL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.GRAPESHOT.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.COMPACT_COPPER_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.STANDARD_COPPER_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.COMPACT_ADVANCED_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.ADVANCED_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new AdvancedRoundProjectileEntity(ModEntities.ADVANCED_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHOTGUN_SHELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            // The 10 special rounds
            ProjectileManager.getInstance().registerFactory(ModItems.HOG_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new HogRoundProjectileEntity(ModEntities.HOG_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.RAMROD_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new RamrodProjectileEntity(ModEntities.RAMROD_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHATTER_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ShatterRoundProjectileEntity(ModEntities.SHATTER_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.KRAHG_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new KrahgRoundProjectileEntity(ModEntities.KRAHG_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.BEOWULF_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BeowulfProjectileEntity(ModEntities.BEOWULF_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.GIBBS_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new GibbsRoundProjectileEntity(ModEntities.GIBBS_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.BEARPACK_SHELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BearPackShellProjectileEntity(ModEntities.BEARPACK_SHELL_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.OSBORNE_SLUG.get(), (worldIn, entity, weapon, item, modifiedGun) -> new OsborneSlugProjectileEntity(ModEntities.OSBORNE_SLUG_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SYRINGE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new SyringeProjectileEntity(ModEntities.SYRINGE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.FROG_DART.get(), (worldIn, entity, weapon, item, modifiedGun) -> new FrogDartProjectileEntity(ModEntities.FROG_DART_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            // GRENADE
            ProjectileManager.getInstance().registerFactory(ModItems.GRENADE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new GrenadeEntity(ModEntities.GRENADE.get(), worldIn, entity, weapon, item, modifiedGun));
            // The SPECIAL-SPECIAL ammo
            ProjectileManager.getInstance().registerFactory(ModItems.BLAZE_FUEL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new FireRoundEntity(ModEntities.FIRE_ROUND_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SCULK_CELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new SculkCellEntity(ModEntities.SCULK_CELL.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHOCK_CELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new LightningProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHULKSHOT.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ShulkshotProjectileEntity(ModEntities.SHULKSHOT.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.ENERGY_CELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new PlasmaProjectileEntity(ModEntities.PLASMA_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(Items.BLAZE_ROD, (worldIn, entity, weapon, item, modifiedGun) -> new BlazeRodProjectileEntity(ModEntities.BLAZE_ROD_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.STANDARD_BULLET.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BasicBulletProjectileEntity(ModEntities.BASIC_BULLET_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.ADVANCED_BULLET.get(), (worldIn, entity, weapon, item, modifiedGun) -> new HardenedBulletProjectileEntity(ModEntities.HARDENED_BULLET_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.NEEDLE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new NeedleProjectileEntity(ModEntities.NEEDLE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.FLECHETTE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new FlechetteProjectileEntity(ModEntities.FLECHETTE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.BUCKSHOT.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BuckshotProjectileEntity(ModEntities.BUCKSHOT_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.NITRO_BUCKSHOT.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BuckshotProjectileEntity(ModEntities.BUCKSHOT_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHOTBALL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ShotballProjectileEntity(ModEntities.SHOTBALL_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.ROCKET.get(), (worldIn, entity, weapon, item, modifiedGun) -> new RocketEntity(ModEntities.ROCKET.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.MICROJET.get(), (worldIn, entity, weapon, item, modifiedGun) -> new MicroJetEntity(ModEntities.MICROJET.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.HE_GRENADE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new HeGrenadeRoundEntity(ModEntities.HE_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.FIRE_GRENADE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new FireGrenadeRoundEntity(ModEntities.FIRE_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.GAS_GRENADE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new GasGrenadeRoundEntity(ModEntities.GAS_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.BOUNCY_GRENADE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BouncyGrenadeRoundEntity(ModEntities.BOUNCY_GRENADE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            if (Config.COMMON.gameplay.improvedHitboxes.get()) {
                NeoForge.EVENT_BUS.register(new BoundingBoxManager());
            }
        });
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ExoSuitUpgradeManager());
    }
    public static boolean isDebugging() {
        return false;
    }
}
