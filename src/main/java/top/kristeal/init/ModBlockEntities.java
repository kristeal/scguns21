package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.blockentity.*;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "scguns");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GunShelfBlockEntity>> GUN_SHELF_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("gun_shelf", () ->
                    BlockEntityType.Builder.of(GunShelfBlockEntity::new,
                            ModBlocks.GUN_SHELF.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedComposterBlockEntity>> ADVANCED_COMPOSTER =
            BLOCK_ENTITIES.register("advanced_composter", () ->
                    BlockEntityType.Builder.of(AdvancedComposterBlockEntity::new,
                            ModBlocks.ADVANCED_COMPOSTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowderKegBlockEntity>> POWDER_KEG =
            BLOCK_ENTITIES.register("powder_keg", () ->
                    BlockEntityType.Builder.of(PowderKegBlockEntity::new,
                            ModBlocks.POWDER_KEG.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NitroKegBlockEntity>> NITRO_KEG =
            BLOCK_ENTITIES.register("nitro_keg", () ->
                    BlockEntityType.Builder.of(NitroKegBlockEntity::new,
                            ModBlocks.NITRO_KEG.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargedAmethystRelayBlockEntity>> CHARGED_AMETHYST_RELAY =
            BLOCK_ENTITIES.register("charged_amethyst_relay", () ->
                    BlockEntityType.Builder.of(ChargedAmethystRelayBlockEntity::new,
                            ModBlocks.CHARGED_AMETHYST_RELAY.get()).build(null));
    // Research progression
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchStationBlockEntity>> RESEARCH_STATION =
            BLOCK_ENTITIES.register("research_station", () ->
                    BlockEntityType.Builder.of(ResearchStationBlockEntity::new,
                            ModBlocks.RESEARCH_STATION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GunBenchBlockEntity>> GUN_BENCH =
            BLOCK_ENTITIES.register("gun_bench", () ->
                    BlockEntityType.Builder.of(GunBenchBlockEntity::new,
                            ModBlocks.GUN_BENCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoniterBlockEntity>> CRYONITER =
            BLOCK_ENTITIES.register("cryoniter", () ->
                    BlockEntityType.Builder.of(CryoniterBlockEntity::new,
                            ModBlocks.CRYONITER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermolithBlockEntity>> THERMOLITH =
            BLOCK_ENTITIES.register("thermolith", () ->
                    BlockEntityType.Builder.of(ThermolithBlockEntity::new,
                            ModBlocks.THERMOLITH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PolarGeneratorBlockEntity>> POLAR_GENERATOR =
            BLOCK_ENTITIES.register("polar_generator", () ->
                    BlockEntityType.Builder.of(PolarGeneratorBlockEntity::new,
                            ModBlocks.POLAR_GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PenetratorBlockEntity>> PENETRATOR =
            BLOCK_ENTITIES.register("penetrator", () ->
                    BlockEntityType.Builder.of(PenetratorBlockEntity::new,
                            ModBlocks.PENETRATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaceratorBlockEntity>> MACERATOR =
            BLOCK_ENTITIES.register("macerator", () ->
                    BlockEntityType.Builder.of(MaceratorBlockEntity::new,
                            ModBlocks.MACERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredMaceratorBlockEntity>> POWERED_MACERATOR =
            BLOCK_ENTITIES.register("powered_macerator", () ->
                    BlockEntityType.Builder.of(PoweredMaceratorBlockEntity::new,
                            ModBlocks.POWERED_MACERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPressBlockEntity>> MECHANICAL_PRESS =
            BLOCK_ENTITIES.register("mechanical_press", () ->
                    BlockEntityType.Builder.of(MechanicalPressBlockEntity::new,
                            ModBlocks.MECHANICAL_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredMechanicalPressBlockEntity>> POWERED_MECHANICAL_PRESS =
            BLOCK_ENTITIES.register("powered_mechanical_press", () ->
                    BlockEntityType.Builder.of(PoweredMechanicalPressBlockEntity::new,
                            ModBlocks.POWERED_MECHANICAL_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightningBatteryBlockEntity>> LIGHTNING_BATTERY =
            BLOCK_ENTITIES.register("lightning_battery", () ->
                    BlockEntityType.Builder.of(LightningBatteryBlockEntity::new,
                            ModBlocks.LIGHTNING_BATTERY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicTurretBlockEntity>> BASIC_TURRET =
            BLOCK_ENTITIES.register("basic_turret", () ->
                    BlockEntityType.Builder.of(BasicTurretBlockEntity::new,
                            ModBlocks.BASIC_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoTurretBlockEntity>> AUTO_TURRET =
            BLOCK_ENTITIES.register("auto_turret", () ->
                    BlockEntityType.Builder.of(AutoTurretBlockEntity::new,
                            ModBlocks.AUTO_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SniperTurretBlockEntity>> SNIPER_TURRET =
            BLOCK_ENTITIES.register("sniper_turret", () ->
                    BlockEntityType.Builder.of(SniperTurretBlockEntity::new,
                            ModBlocks.SNIPER_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnemyTurretBlockEntity>> ENEMY_TURRET =
            BLOCK_ENTITIES.register("enemy_turret", () ->
                    BlockEntityType.Builder.of(EnemyTurretBlockEntity::new,
                            ModBlocks.ENEMY_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShotgunTurretBlockEntity>> SHOTGUN_TURRET =
            BLOCK_ENTITIES.register("shotgun_turret", () ->
                    BlockEntityType.Builder.of(ShotgunTurretBlockEntity::new,
                            ModBlocks.SHOTGUN_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShellCatcherModuleBlockEntity>> SHELL_CATCHER_MODULE =
            BLOCK_ENTITIES.register("shell_catcher_module", () ->
                    BlockEntityType.Builder.of(ShellCatcherModuleBlockEntity::new,
                            ModBlocks.SHELL_CATCHER_TURRET_MODULE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoModuleBlockEntity>> AMMO_MODULE =
            BLOCK_ENTITIES.register("ammo_module", () ->
                    BlockEntityType.Builder.of(AmmoModuleBlockEntity::new,
                            ModBlocks.AMMO_TURRET_MODULE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoBoxBlockEntity>> AMMO_BOX =
            BLOCK_ENTITIES.register("ammo_box", () ->
                    BlockEntityType.Builder.of(AmmoBoxBlockEntity::new,
                            ModBlocks.AMMO_BOX.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VentCollectorBlockEntity>> VENT_COLLECTOR = BLOCK_ENTITIES.register("vent_collector",
            () -> BlockEntityType.Builder.of(VentCollectorBlockEntity::new, ModBlocks.VENT_COLLECTOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
