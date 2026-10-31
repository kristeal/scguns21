package top.kristeal.client;

import top.kristeal.fluid.ViciousAcidFluidType;
import top.kristeal.init.ModFluids;

import top.kristeal.init.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import top.kristeal.network.message.C2SMessageBanzai;
import top.kristeal.network.message.C2SMessageMeleeAttack;
import top.kristeal.client.handler.ClientMeleeAttackHandler;
import top.kristeal.client.handler.MeleeAttackHandler;
import top.kristeal.client.screen.AttachmentScreen;
import top.kristeal.client.handler.BeamHandler;
import top.kristeal.network.message.C2SMessageSwapAmmo;
import top.kristeal.client.handler.ShootingHandler;
import top.kristeal.client.handler.ReloadHandler;
import top.kristeal.client.handler.RecoilHandler;
import top.kristeal.client.handler.BulletTrailRenderingHandler;
import top.kristeal.client.handler.AimingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.animatable.GeoItem;
import top.kristeal.client.handler.CrosshairHandler;
import top.kristeal.client.handler.GunRenderingHandler;
import top.kristeal.client.handler.PlayerModelHandler;
import top.kristeal.client.handler.InspectHandler;
import top.kristeal.client.handler.EntityMuzzleFlashHandler;
import top.kristeal.client.render.gun.ModelOverrides;
import top.kristeal.client.render.gun.model.*;
import top.kristeal.client.util.PropertyHelper;
import top.kristeal.item.GunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;
import top.kristeal.Reference;
import top.kristeal.client.handler.SoundHandler;
import top.kristeal.client.handler.TurretBulletTrailRenderingHandler;
import top.kristeal.client.render.block.*;
import top.kristeal.client.render.curios.AirCanisterRenderer;
import top.kristeal.client.render.curios.AmmoBoxRenderer;
import top.kristeal.client.render.entity.TurretProjectileRenderer;
import top.kristeal.client.render.entity.NitroKegRenderer;
import top.kristeal.client.render.entity.PowderKegRenderer;
import top.kristeal.client.screen.CryoniterScreen;
import top.kristeal.client.screen.ExoSuitScreen;
import top.kristeal.client.screen.SupplyScampScreen;
import top.kristeal.client.screen.GunBenchScreen;
import top.kristeal.client.screen.ResearchStationScreen;
import top.kristeal.client.screen.LightningBatteryScreen;
import top.kristeal.client.screen.MaceratorScreen;
import top.kristeal.client.screen.MechanicalPressScreen;
import top.kristeal.client.screen.PoweredMaceratorScreen;
import top.kristeal.client.screen.PolarGeneratorScreen;
import top.kristeal.client.screen.PoweredMechanicalPressScreen;
import top.kristeal.client.screen.ThermolithScreen;
import top.kristeal.client.screen.VentCollectorScreen;
import top.kristeal.client.screen.ModMenuTypes;
import top.kristeal.client.screen.AmmoBoxScreen;
import top.kristeal.client.screen.AmmoModuleScreen;
import top.kristeal.client.screen.AutoTurretScreen;
import top.kristeal.client.screen.BasicTurretScreen;
import top.kristeal.client.screen.ShellCatcherModuleScreen;
import top.kristeal.client.screen.ShotgunTurretScreen;
import top.kristeal.client.screen.SniperTurretScreen;
import top.kristeal.entity.client.*;
import top.kristeal.init.ModBlockEntities;
import top.kristeal.init.ModEntities;
import top.kristeal.init.ModItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.kristeal.item.animated.ExoSuitItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.C2SMessageAttachments;
import top.kristeal.network.message.C2SMessageToggleExoSuitPower;
import top.kristeal.network.message.C2SMessageUtilityAction;

/**
 * Author: MrCrayfish
 * Ported to NeoForge 1.21.1
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientHandler {
    private static long lastAmmoSwapTime = 0;
    private static final long SWAP_COOLDOWN_MS = 1000;

    /*
     * Note: In 1.20.1 Forge, ScorchedGuns called ClientHandler.registerClientHandlers(bus) via DistExecutor.
     * In 1.21.1 NeoForge, DistExecutor was removed, and this class uses @EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT).
     * NeoForge automatically routes IModBusEvent methods (e.g. RegisterMenuScreensEvent, RegisterKeyMappingsEvent)
     * to the Mod Event Bus and standard events (e.g. InputEvent.Key) to NeoForge.EVENT_BUS.
     */

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.EXOSUIT_MENU.get(), ExoSuitScreen::new);
        event.register(ModMenuTypes.ATTACHMENTS.get(), AttachmentScreen::new);
        event.register(ModMenuTypes.MACERATOR_MENU.get(), MaceratorScreen::new);
        event.register(ModMenuTypes.POWERED_MACERATOR_MENU.get(), PoweredMaceratorScreen::new);
        event.register(ModMenuTypes.MECHANICAL_PRESS_MENU.get(), MechanicalPressScreen::new);
        event.register(ModMenuTypes.POWERED_MECHANICAL_PRESS_MENU.get(), PoweredMechanicalPressScreen::new);
        event.register(ModMenuTypes.LIGHTING_BATTERY_MENU.get(), LightningBatteryScreen::new);
        event.register(ModMenuTypes.VENT_COLLECTOR_MENU.get(), VentCollectorScreen::new);
        event.register(ModMenuTypes.POLAR_GENERATOR_MENU.get(), PolarGeneratorScreen::new);
        event.register(ModMenuTypes.CRYONITER_MENU.get(), CryoniterScreen::new);
        event.register(ModMenuTypes.THERMOLITH_MENU.get(), ThermolithScreen::new);
        event.register(ModMenuTypes.GUN_BENCH.get(), GunBenchScreen::new);
        event.register(ModMenuTypes.RESEARCH_STATION.get(), ResearchStationScreen::new); // research progression
        event.register(ModMenuTypes.SUPPLY_SCAMP_MENU.get(), SupplyScampScreen::new);
        // Turrets and turret modules
        event.register(ModMenuTypes.SHELL_CATCHER_MODULE.get(), ShellCatcherModuleScreen::new);
        event.register(ModMenuTypes.AMMO_MODULE.get(), AmmoModuleScreen::new);
        event.register(ModMenuTypes.BASIC_TURRET_MENU.get(), BasicTurretScreen::new);
        event.register(ModMenuTypes.AUTO_TURRET_MENU.get(), AutoTurretScreen::new);
        event.register(ModMenuTypes.SNIPER_TURRET_MENU.get(), SniperTurretScreen::new);
        event.register(ModMenuTypes.SHOTGUN_TURRET_MENU.get(), ShotgunTurretScreen::new);
        // Ammo box
        event.register(ModMenuTypes.AMMO_BOX.get(), AmmoBoxScreen::new);

    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRIMED_POWDER_KEG.get(), PowderKegRenderer::new);
        event.registerEntityRenderer(ModEntities.PRIMED_NITRO_KEG.get(), NitroKegRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GUN_SHELF_BLOCK_ENTITY.get(), GunShelfRenderer::new);
        // Mobs
        event.registerEntityRenderer(ModEntities.SUPPLY_SCAMP.get(), SupplyScampRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAUMA_UNIT.get(), TraumaUnitRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAUMA_HOOK.get(), TraumaHookRenderer::new);
        event.registerEntityRenderer(ModEntities.DISSIDENT.get(), DissidentRenderer::new);
        event.registerEntityRenderer(ModEntities.PRAETOR.get(), PraetorRenderer::new);
        // Raid-only mobs, their projectiles and the Viventrum
        event.registerEntityRenderer(ModEntities.VIVENTRUM.get(), top.kristeal.entity.client.ViventrumRenderer::new);
        event.registerEntityRenderer(ModEntities.BLUNDERER.get(), top.kristeal.entity.client.BlundererRenderer::new);
        event.registerEntityRenderer(ModEntities.ADJUDICATOR.get(), top.kristeal.entity.client.AdjudicatorRenderer::new);
        event.registerEntityRenderer(ModEntities.SUBJUGATOR.get(), top.kristeal.entity.client.SubjugatorRenderer::new);
        event.registerEntityRenderer(ModEntities.SCAMP_TANK.get(), top.kristeal.entity.client.ScampTankRenderer::new);
        event.registerEntityRenderer(ModEntities.SCAMP_ROCKET.get(), top.kristeal.entity.client.ScampRocketRenderer::new);
        event.registerEntityRenderer(ModEntities.SCAMPLER.get(), top.kristeal.entity.client.ScamplerRenderer::new);
        event.registerEntityRenderer(ModEntities.MOTHER_GHAST.get(), top.kristeal.entity.client.MotherGhastRenderer::new);
        event.registerEntityRenderer(ModEntities.FINFORCER.get(), top.kristeal.entity.client.FinforcerRenderer::new);
        // The reference's anonymous renderer: it draws nothing (only the texture is named), the beacon is particles
        event.registerEntityRenderer(ModEntities.BEACON_PROJECTILE.get(), context ->
                new net.minecraft.client.renderer.entity.EntityRenderer<top.kristeal.entity.monster.BeaconProjectileEntity>(context) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(top.kristeal.entity.monster.BeaconProjectileEntity entity) {
                        return net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/item/beacon.png");
                    }
                });
        // Mobs
        event.registerEntityRenderer(ModEntities.HIVE.get(), HiveRenderer::new);
        event.registerEntityRenderer(ModEntities.SWARM.get(), SwarmRenderer::new);
        event.registerEntityRenderer(ModEntities.HORNLIN.get(), HornlinRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIFIED_HORNLIN.get(), ZombifiedHornlinRenderer::new);
        event.registerEntityRenderer(ModEntities.SULFUR_GAS_CLOUD.get(), SulfurGasCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.SULFURHEAD.get(), SulfurheadRenderer::new);
        // Mobs
        event.registerEntityRenderer(ModEntities.COG_MINION.get(), CogMinionRenderer::new);
        event.registerEntityRenderer(ModEntities.COG_KNIGHT.get(), CogKnightRenderer::new);
        event.registerEntityRenderer(ModEntities.SKY_CARRIER.get(), SkyCarrierRenderer::new);
        event.registerEntityRenderer(ModEntities.THE_MERCHANT.get(), TheMerchantRenderer::new);
        event.registerEntityRenderer(ModEntities.SIGNAL_BEACON.get(), SignalBeaconRenderer::new);
        event.registerEntityRenderer(ModEntities.ENEMY_PROJECTILE.get(), EnemyProjectileRenderer::new);
        // Turrets (the reference registered block entity renderers through BlockEntityRenderers.register)
        event.registerEntityRenderer(ModEntities.TURRET_PROJECTILE.get(), TurretProjectileRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BASIC_TURRET.get(), BasicTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENEMY_TURRET.get(), EnemyTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AUTO_TURRET.get(), AutoTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHOTGUN_TURRET.get(), ShotgunTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SNIPER_TURRET.get(), SniperTurretRenderer::new);
        // Machine renderers (their animated parts are SpecialModels)
        event.registerBlockEntityRenderer(ModBlockEntities.MACERATOR.get(), MaceratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POWERED_MACERATOR.get(), PoweredMaceratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MECHANICAL_PRESS.get(), MechanicalPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), PoweredMechanicalPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POLAR_GENERATOR.get(), PolarGeneratorRenderer::new);
    }

    /**
     * The reference registered TurretBulletTrailRenderingHandler on the Forge bus in its client setup.
     * The handler is an instance subscriber (client tick, logout/respawn clear, and the AFTER_ENTITIES render stage).
     */
    /**
     * The reference registered MetaLoader through Framework and reset the PropertyHelper cache on every
     * resource reload.
     */
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MetaLoader.getInstance());
        event.registerReloadListener((ResourceManagerReloadListener) manager -> PropertyHelper.resetCache());
    }

    /**
     * NeoForge 1.21 removed Item#initializeClient, where the reference's GunItem returned a new
     * GunItemStackRenderer. GeckoLib registers AnimatedGunRenderer for every GeoItem, so this covers the other guns.
     */
    /** The reference added CrosshairHandler::onConfigReload to the mod bus. */
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        CrosshairHandler.onConfigReload(event);
        updateRecoilConfig(event);
    }

    /** Fix: the reference's onConfigLoad. Without it the server option enableCameraRecoil was never read. */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        updateRecoilConfig(event);
    }

    private static void updateRecoilConfig(ModConfigEvent event) {
        if (event.getConfig().getType() == net.neoforged.fml.config.ModConfig.Type.SERVER
                && Reference.MOD_ID.equals(event.getConfig().getModId())) {
            RecoilHandler.get().updateConfig();
        }
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof GunItem && !(item instanceof GeoItem)).forEach(item ->
                event.registerItem(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return new GunItemStackRenderer();
                    }
                }, item));
        // The vicious acid textures, tint and fog (the reference's FluidType#initializeClient)
        event.registerFluidType(((ViciousAcidFluidType) ModFluids.VICIOUS_ACID_FLUID_TYPE.get()).createClientExtensions(),
                ModFluids.VICIOUS_ACID_FLUID_TYPE.get());
    }

    // The debug editor's screen factory
    public static net.minecraft.client.gui.screens.Screen createEditorScreen(top.kristeal.debug.IEditorMenu menu) {
        return new top.kristeal.debug.client.screen.EditorScreen(Minecraft.getInstance().screen, menu);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // The reference's render layers. Without them the Niter glass drew in the solid layer
        // (opaque, one flat colour when lit) and the three cutout blocks lost their transparent pixels.
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PLASMA_LANTERN.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.WHITE_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GREEN_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUE_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORANGE_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PURPLE_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLACK_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PINK_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BROWN_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.CYAN_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHT_BLUE_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIME_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGENTA_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GRAY_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHT_GRAY_NITER_GLASS.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHARGED_AMETHYST_RELAY.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FAKE_SOUL_FIRE.get(), RenderType.cutout());
        NeoForge.EVENT_BUS.register(TurretBulletTrailRenderingHandler.get());
        NeoForge.EVENT_BUS.register(SoundHandler.get()); // Deafen (the reference registered it in its client setup)
        // Aiming, trails, camera recoil, reloading and shooting. GunRecoilHandler stays
        // unregistered, as in the reference (only the gun renderer reads its fields).
        NeoForge.EVENT_BUS.register(AimingHandler.get());
        NeoForge.EVENT_BUS.register(BulletTrailRenderingHandler.get());
        NeoForge.EVENT_BUS.register(RecoilHandler.get());
        NeoForge.EVENT_BUS.register(ReloadHandler.get());
        NeoForge.EVENT_BUS.register(ShootingHandler.get());
        // Rendering, crosshair and third-person model handlers; the Lockewood's baked model
        NeoForge.EVENT_BUS.register(GunRenderingHandler.get());
        NeoForge.EVENT_BUS.register(BeamHandler.class); // Beam rendering (the reference registered it in its client setup)
        NeoForge.EVENT_BUS.register(CrosshairHandler.get());
        NeoForge.EVENT_BUS.register(new PlayerModelHandler());
        // Inspect key (the reference registered it in ScorchedGuns) and mob muzzle flashes
        NeoForge.EVENT_BUS.register(InspectHandler.get());
        NeoForge.EVENT_BUS.register(new EntityMuzzleFlashHandler());
        ModelOverrides.register(ModItems.LOCKEWOOD.get(), new LockewoodModel());
        // The 53 ballistic guns (reference registration order)
        ModelOverrides.register(ModItems.EARTHS_CORPSE.get(), new EarthsCorpseModel());
        ModelOverrides.register(ModItems.FENCER_CARABINE.get(), new FencerCarabineModel());
        ModelOverrides.register(ModItems.FENCER_THUMPER.get(), new FencerThumperModel());
        ModelOverrides.register(ModItems.GATTALER.get(), new GattalerModel());
        ModelOverrides.register(ModItems.DARK_MATTER.get(), new DarkMatterModel());
        ModelOverrides.register(ModItems.SUPER_SHOTGUN.get(), new SuperShotgunModel());
        ModelOverrides.register(ModItems.REPEATING_MUSKET.get(), new RepeatingMusketModel());
        ModelOverrides.register(ModItems.JACKHAMMER.get(), new JackhammerModel());
        ModelOverrides.register(ModItems.JR_WRISTBREAKER.get(), new JrWristbreakerModel());
        ModelOverrides.register(ModItems.KILLER_23.get(), new Killer23Model());
        ModelOverrides.register(ModItems.PAX.get(), new PaxModel());
        ModelOverrides.register(ModItems.FLINTLOCK_PISTOL.get(), new FlintlockPistolModel());
        ModelOverrides.register(ModItems.HANDCANNON.get(), new HandcannonPistolModel());
        ModelOverrides.register(ModItems.MUSKET.get(), new MusketModel());
        ModelOverrides.register(ModItems.BLUNDERBUSS.get(), new BlunderbussModel());
        ModelOverrides.register(ModItems.LONGARM.get(), new LongarmModel());
        ModelOverrides.register(ModItems.DOUBLET.get(), new DoubletModel());
        ModelOverrides.register(ModItems.SPIRULIDA.get(), new SpirulidaModel());
        ModelOverrides.register(ModItems.FLOUNDERGAT.get(), new FloundergatModel());
        ModelOverrides.register(ModItems.SAKETINI.get(), new SaketiniModel());
        ModelOverrides.register(ModItems.SAKETINI_IRONPORT.get(), new SaketiniIronPortModel());
        ModelOverrides.register(ModItems.CALLWELL.get(), new CallwellModel());
        ModelOverrides.register(ModItems.WINNIE.get(), new WinnieModel());
        ModelOverrides.register(ModItems.WINNIE_MILLEND.get(), new WinnieMillendModel());
        ModelOverrides.register(ModItems.SCRAPPER.get(), new ScrapperModel());
        ModelOverrides.register(ModItems.MAKESHIFT_RIFLE.get(), new MakeshiftRifleModel());
        ModelOverrides.register(ModItems.BOOMSTICK.get(), new BoomstickModel());
        ModelOverrides.register(ModItems.RUSTY_GNAT.get(), new RustyGnatModel());
        ModelOverrides.register(ModItems.LLR_DIRECTOR.get(), new LlrDirectorModel());
        ModelOverrides.register(ModItems.BIRDFEEDER.get(), new BirdfeederModel());
        ModelOverrides.register(ModItems.TURNPIKE.get(), new TurnpikeModel());
        ModelOverrides.register(ModItems.KALASKAH.get(), new KalaskahModel());
        ModelOverrides.register(ModItems.MOKOVA.get(), new MokovaModel());
        ModelOverrides.register(ModItems.MAK_MKII.get(), new MakMkIIModel());
        ModelOverrides.register(ModItems.STIGG.get(), new StiggModel());
        ModelOverrides.register(ModItems.IRON_JAVELIN.get(), new IronJavelinModel());
        ModelOverrides.register(ModItems.M3_CARABINE.get(), new M3CarabineModel());
        ModelOverrides.register(ModItems.GREASER_SMG.get(), new GreaserSmgModel());
        ModelOverrides.register(ModItems.DRILL.get(), new DrillModel());
        ModelOverrides.register(ModItems.DRILL_CONVERSION.get(), new DrillConversionModel());
        ModelOverrides.register(ModItems.DEFENDER_PISTOL.get(), new DefenderPistolModel());
        ModelOverrides.register(ModItems.COMBAT_SHOTGUN.get(), new CombatShotgunModel());
        ModelOverrides.register(ModItems.PRUSH_GUN.get(), new PrushGunModel());
        ModelOverrides.register(ModItems.COGLOADER.get(), new CogloaderModel());
        ModelOverrides.register(ModItems.GRANDLE.get(), new GrandleModel());
        ModelOverrides.register(ModItems.GRANDLE_OG.get(), new GrandleOgModel());
        ModelOverrides.register(ModItems.SOUL_DRUMMER.get(), new SoulDrummerModel());
        ModelOverrides.register(ModItems.VALORA.get(), new ValoraModel());
        ModelOverrides.register(ModItems.KRAUSER.get(), new KrauserModel());
        ModelOverrides.register(ModItems.TRENCHUR.get(), new TrenchurModel());
        ModelOverrides.register(ModItems.CALLWELL_CONVERSION.get(), new CallwellConversionModel());
        ModelOverrides.register(ModItems.CALLWELL_TERMINAL.get(), new CallwellTerminalModel());
        ModelOverrides.register(ModItems.PRIMA_MATERIA.get(), new PrimaMateriaModel());
        // The 36 ballistic guns II (reference registration order)
        ModelOverrides.register(ModItems.LOCUST.get(), new LocustModel());
        ModelOverrides.register(ModItems.NEWBORN_CYST.get(), new NewbornCystModel());
        ModelOverrides.register(ModItems.FREYR.get(), new FreyrModel());
        ModelOverrides.register(ModItems.VULCANIC_REPEATER.get(), new VulcanicRepeaterModel());
        ModelOverrides.register(ModItems.OSGOOD_50.get(), new Osgood50Model());
        ModelOverrides.register(ModItems.KILN_GUN.get(), new KilnGunModel());
        ModelOverrides.register(ModItems.THUNDERHEAD.get(), new ThunderheadModel());
        ModelOverrides.register(ModItems.ULTRA_KNIGHT_HAWK.get(), new UltraKnightHawkModel());
        ModelOverrides.register(ModItems.SEQUOIA.get(), new SequoiaModel());
        ModelOverrides.register(ModItems.HOMEMAKER.get(), new HomemakerModel());
        ModelOverrides.register(ModItems.RIBS_GLORY.get(), new RibsGloryModel());
        ModelOverrides.register(ModItems.HOWLER.get(), new HowlerModel());
        ModelOverrides.register(ModItems.HOWLER_CONVERSION.get(), new HowlerConversionModel());
        ModelOverrides.register(ModItems.BIG_BORE.get(), new BigBoreModel());
        ModelOverrides.register(ModItems.ZILK_45.get(), new Zilk45Model());
        ModelOverrides.register(ModItems.WHISTLER.get(), new WhistlerModel());
        ModelOverrides.register(ModItems.ASTELLA.get(), new AstellaModel());
        ModelOverrides.register(ModItems.BRAWLER.get(), new BrawlerModel());
        ModelOverrides.register(ModItems.WHIZZBANGER.get(), new WhizzbangerModel());
        ModelOverrides.register(ModItems.BRUISER.get(), new BruiserModel());
        ModelOverrides.register(ModItems.HYPERBARIA.get(), new HyperbariaModel());
        ModelOverrides.register(ModItems.MARLIN.get(), new MarlinModel());
        ModelOverrides.register(ModItems.IRON_SPEAR.get(), new IronSpearModel());
        ModelOverrides.register(ModItems.M3_MARKSMAN.get(), new M3MarksmanModel());
        ModelOverrides.register(ModItems.STILETTO.get(), new StilettoModel());
        ModelOverrides.register(ModItems.RG_JIGSAW.get(), new RgJigsawModel());
        ModelOverrides.register(ModItems.AUVTOMAG.get(), new AuvtomagModel());
        ModelOverrides.register(ModItems.INERTIAL.get(), new InertialModel());
        ModelOverrides.register(ModItems.UPPERCUT.get(), new UppercutModel());
        ModelOverrides.register(ModItems.MAS_55.get(), new Mas55Model());
        ModelOverrides.register(ModItems.MAS_PEDDLER.get(), new MasPeddlerModel());
        ModelOverrides.register(ModItems.M22_WALTZ.get(), new M22WaltzModel());
        ModelOverrides.register(ModItems.MICINA.get(), new MicinaModel());
        ModelOverrides.register(ModItems.MANGALITSA.get(), new MangalitsaModel());
        ModelOverrides.register(ModItems.CRUSADER.get(), new CrusaderModel());
        ModelOverrides.register(ModItems.TROTTERS.get(), new TrottersModel());
        // The remaining 51 guns (reference registration order)
        ModelOverrides.register(ModItems.FLAYED_GOD.get(), new FlayedGodModel());
        ModelOverrides.register(ModItems.NERVEPINCH.get(), new NervepinchModel());
        ModelOverrides.register(ModItems.RAT_KING_AND_QUEEN.get(), new RatKingAndQueenModel());
        ModelOverrides.register(ModItems.STERILIZER.get(), new SterilizerModel());
        ModelOverrides.register(ModItems.LONE_WONDER.get(), new LoneWonderModel());
        ModelOverrides.register(ModItems.CARAPICE.get(), new CarapiceModel());
        ModelOverrides.register(ModItems.SHELLURKER.get(), new ShellurkerModel());
        ModelOverrides.register(ModItems.ECHOES_2.get(), new Echoes2Model());
        ModelOverrides.register(ModItems.RAYGUN.get(), new RaygunModel());
        ModelOverrides.register(ModItems.SCULK_RESONATOR.get(), new SculkResonatorModel());
        ModelOverrides.register(ModItems.BLASPHEMY.get(), new BlasphemyModel());
        ModelOverrides.register(ModItems.WHISPERS.get(), new WhispersModel());
        ModelOverrides.register(ModItems.PYROCLASTIC_FLOW.get(), new PyroclasticFlowModel());
        ModelOverrides.register(ModItems.SCRATCHES.get(), new ScratchesModel());
        ModelOverrides.register(ModItems.GALE.get(), new GaleModel());
        ModelOverrides.register(ModItems.WALTZ_CONVERSION.get(), new WaltzConversionModel());
        ModelOverrides.register(ModItems.UMAX_PISTOL.get(), new UmaxPistolModel());
        ModelOverrides.register(ModItems.SPITFIRE.get(), new SpitfireModel());
        ModelOverrides.register(ModItems.SHARD_CULLER.get(), new ShardCullerModel());
        ModelOverrides.register(ModItems.CR4K_MINING_LASER.get(), new Cr4kMiningLaserModel());
        ModelOverrides.register(ModItems.GYROJET_PISTOL.get(), new GyrojetPistolModel());
        ModelOverrides.register(ModItems.DOZIER_RL.get(), new DozierRLModel());
        ModelOverrides.register(ModItems.BOMB_LANCE.get(), new BombLanceModel());
        ModelOverrides.register(ModItems.VENTURI.get(), new VenturiModel());
        ModelOverrides.register(ModItems.RED_RAYDAR.get(), new RedRaydarModel());
        ModelOverrides.register(ModItems.MK43_RIFLE.get(), new Mk43RifleModel());
        ModelOverrides.register(ModItems.PLASGUN.get(), new PlasgunModel());
        ModelOverrides.register(ModItems.LASER_MUSKET.get(), new LaserMusketModel());
        ModelOverrides.register(ModItems.PLASMABUSS.get(), new PlasmabussModel());
        ModelOverrides.register(ModItems.PULSAR.get(), new PulsarModel());
        ModelOverrides.register(ModItems.ARC_WORKER.get(), new ArcWorkerModel());
        ModelOverrides.register(ModItems.LIBERTAS.get(), new LibertasModel());
        ModelOverrides.register(ModItems.TESLOCK_RIFLE.get(), new TeslockRifleModel());
        ModelOverrides.register(ModItems.HULLBREAKER.get(), new HullbreakerModel());
        ModelOverrides.register(ModItems.NAILER.get(), new NailerModel());
        ModelOverrides.register(ModItems.TRUANT.get(), new TruantModel());
        ModelOverrides.register(ModItems.HAMMER_GL.get(), new HammerGlModel());
        ModelOverrides.register(ModItems.BLOOPER.get(), new BlooperModel());
        ModelOverrides.register(ModItems.TRIQUETRA.get(), new TriquetraModel());
        ModelOverrides.register(ModItems.BASKER.get(), new BaskerModel());
        ModelOverrides.register(ModItems.WEEVIL.get(), new WeevilModel());
        ModelOverrides.register(ModItems.TL_RUNNER.get(), new TlRunnerModel());
        ModelOverrides.register(ModItems.TERRA_INCOGNITA.get(), new TerraIncognitaModel());
        ModelOverrides.register(ModItems.GAUSS_RIFLE.get(), new GaussRifleModel());
        ModelOverrides.register(ModItems.ROCKET_RIFLE.get(), new RocketRifleModel());
        ModelOverrides.register(ModItems.RAILWORKER.get(), new RailworkerModel());
        ModelOverrides.register(ModItems.INQUISITOR.get(), new InquisitorModel());
        ModelOverrides.register(ModItems.CYCLONE.get(), new CycloneModel());
        ModelOverrides.register(ModItems.MINKSY.get(), new MinksyModel());
        ModelOverrides.register(ModItems.NIAMI.get(), new NiamiModel());
        ModelOverrides.register(ModItems.FORLORN_HOPE.get(), new ForlornHopeModel());

        // Curios renderers (the reference registered these in its client setup)
        CuriosRendererRegistry.register(ModItems.PISTOL_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.RIFLE_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.SHOTGUN_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.MAGNUM_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.ENERGY_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.ROCKET_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.SPECIAL_AMMO_BOX.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.EMPTY_CASING_POUCH.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.DISHES_POUCH.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.ROCK_POUCH.get(), AmmoBoxRenderer::new);
        CuriosRendererRegistry.register(ModItems.CREATIVE_AMMO_BOX.get(), AmmoBoxRenderer::new);

        CuriosRendererRegistry.register(ModItems.AIR_CANISTER.get(), AirCanisterRenderer::new);
        CuriosRendererRegistry.register(ModItems.REINFORCED_AIR_CANISTER.get(), AirCanisterRenderer::new);
        CuriosRendererRegistry.register(ModItems.CREATIVE_AIR_CANISTER.get(), AirCanisterRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBinds.registerKeyMappings(event);
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && event.getAction() == GLFW.GLFW_PRESS) {
            if (KeyBinds.KEY_ATTACHMENTS.isDown()) {
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageAttachments());
            }
            if (KeyBinds.KEY_SWAP_AMMO.isDown()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAmmoSwapTime >= SWAP_COOLDOWN_MS) {
                    lastAmmoSwapTime = currentTime;
                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageSwapAmmo());
                }
            }
            if (hasAnyExoSuitEquipped(mc.player)) {
                if (KeyBinds.KEY_ENABLE_EXO_HELMET.consumeClick()) {
                    PacketHandler.getPlayChannel().sendToServer(
                            new C2SMessageToggleExoSuitPower(C2SMessageToggleExoSuitPower.PowerType.HELMET_HUD)
                    );
                }

                if (KeyBinds.KEY_ENABLE_EXO_BOOTS.consumeClick()) {
                    PacketHandler.getPlayChannel().sendToServer(
                            new C2SMessageToggleExoSuitPower(C2SMessageToggleExoSuitPower.PowerType.BOOTS_MOBILITY)
                    );
                }

                if (KeyBinds.KEY_ENABLE_EXO_CHESTPLATE.consumeClick()) {
                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageUtilityAction());
                }
            }
        }
    }

    private static boolean hasAnyExoSuitEquipped(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem) {
                return true;
            }
        }
        return false;
    }

    /** Ticks of running with the melee key held before a charge starts. */
    private static final int BANZAI_HOLD_TICKS = 10;
    /** The melee key was pressed with a gun off cooldown; releasing it before a charge starts stabs. */
    private static boolean meleeHeld;
    private static int meleeHeldSlot;
    /** Ticks the melee key has been held while sprinting with a bayonet, reset when sprinting stops. */
    private static int banzaiHoldTicks;
    /** A charge start was sent for the current hold of the melee key; cleared when the key is released. */
    private static boolean banzaiRequested;
    /** Last tick's synced charge state, to notice when a charge ends. */
    private static boolean wasCharging;
    /**
     * Ticks left of the cooldown after a charge, counted here as well because the gun's synced cooldown can arrive a
     * tick after the charge ends. Stabs and charges both wait for it (the server enforces the same cooldown).
     */
    private static int banzaiCooldown;
    /** Ticks since a charge start was sent without the charge running; the request is dropped if the server refused. */
    private static int banzaiRequestAge;

    // The melee key: releasing it stabs; holding it for a second of running with a bayonet charges instead, until it
    // is released. A charge needs a fresh press of the key.
    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            meleeHeld = false;
            banzaiRequested = false;
            wasCharging = false;
            banzaiCooldown = 0;
            return;
        }
        boolean charging = MeleeAttackHandler.isBanzaiActive(mc.player);
        if (wasCharging && !charging) {
            banzaiCooldown = MeleeAttackHandler.BANZAI_COOLDOWN_TICKS;
            ClientMeleeAttackHandler.showMeleeCooldown(MeleeAttackHandler.BANZAI_COOLDOWN_TICKS);
            banzaiRequested = false;
        }
        wasCharging = charging;
        if (banzaiCooldown > 0) {
            banzaiCooldown--;
        }
        if (banzaiRequested && !charging && ++banzaiRequestAge > 10) {
            banzaiRequested = false; // the server refused the start
        }
        boolean keyDown = mc.screen == null && KeyBinds.KEY_MELEE.isDown();
        if (banzaiRequested && !keyDown) {
            banzaiRequested = false;
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageBanzai(false));
        }

        boolean pressed = false;
        while (KeyBinds.KEY_MELEE.consumeClick()) {
            pressed = true;
        }
        ItemStack heldItem = mc.player.getMainHandItem();
        int slot = mc.player.getInventory().selected;
        if (mc.screen != null || !(heldItem.getItem() instanceof GunItem gunItem) || (meleeHeld && slot != meleeHeldSlot)) {
            meleeHeld = false; // a screen or an item switch cancels the hold without a stab
            return;
        }

        boolean onCooldown = banzaiCooldown > 0 || MeleeAttackHandler.isMeleeOnCooldown(mc.player, heldItem);
        if (pressed && !meleeHeld && !banzaiRequested && !onCooldown) {
            meleeHeld = true;
            meleeHeldSlot = slot;
            banzaiHoldTicks = 0;
        }
        if (!meleeHeld) {
            return;
        }
        if (!keyDown) {
            // Released before the charge started (or tapped within one tick)
            meleeHeld = false;
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageMeleeAttack());
            return;
        }
        if (mc.player.isSprinting() && gunItem.hasBayonet(heldItem)) {
            if (++banzaiHoldTicks >= BANZAI_HOLD_TICKS) {
                meleeHeld = false;
                banzaiRequested = true;
                banzaiRequestAge = 0;
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageBanzai(true));
            }
        } else {
            banzaiHoldTicks = 0;
        }
    }

}
