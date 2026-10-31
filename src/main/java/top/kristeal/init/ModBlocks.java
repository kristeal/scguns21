package top.kristeal.init;

import net.minecraft.world.level.block.LiquidBlock;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;
import top.kristeal.block.*;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(Reference.MOD_ID);

    // Vicious acid
    public static final DeferredBlock<LiquidBlock> VICIOUS_ACID_BLOCK = REGISTER.register("vicious_acid_block",
            () -> new ViciousAcidBlock(ModFluids.VICIOUS_ACID_SOURCE.get(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.WATER)
                    .noCollission()
                    .strength(100.0F)
                    .lightLevel((state) -> 4)
                    .noLootTable()));

    public static final DeferredBlock<Block> VICIOUS_ACID_CAULDRON = REGISTER.register("vicious_acid_cauldron",
            () -> new ViciousAcidCauldronBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CAULDRON)
                    .lightLevel((state) -> 4)));

    // Registered directly (no BlockItem), as in the reference
    public static final DeferredBlock<Block> FAKE_SOUL_FIRE = REGISTER.register("fake_soul_fire",
            () -> new FakeSoulFireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_FIRE), 2.0F));

    public static final DeferredBlock<Block> TEMPORARY_LIGHT = REGISTER.register("temporary_light",
            TemporaryLightBlock::new);
    public static final DeferredBlock<Block> GUN_SHELF = register("gun_shelf",
            () -> new GunShelfBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS)
                    .strength(0.5F)));
    public static final DeferredBlock<Block> AMMO_BOX = register("ammo_box",
            () -> new AmmoBoxBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(2.5F)));
    public static final DeferredBlock<Block> ADVANCED_COMPOSTER = register("advanced_composter",
            () -> new AdvancedComposterBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.COMPOSTER)
                    .strength(0.5F)));
    public static final DeferredBlock<Block> POWDER_KEG = register("powder_keg",
            () -> new PowderKegBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.BARREL)
                    .strength(0.5F)));
    public static final DeferredBlock<Block> NITRO_KEG = register("nitro_keg",
            () -> new NitroKegBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.BARREL)
                    .strength(0.5F)));
    public static final DeferredBlock<Block> CRYONITER = register("cryoniter",
            () -> new CryoniterBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)));
    public static final DeferredBlock<Block> THERMOLITH = register("thermolith",
            () -> new ThermolithBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)));
    public static final DeferredBlock<Block> BASIC_TURRET = register("basic_turret",
            () -> new BasicTurretBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> SHOTGUN_TURRET = register("shotgun_turret",
            () -> new ShotgunTurretBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> CHARGED_AMETHYST_RELAY = register("charged_amethyst_relay",
            () -> new ChargedAmethystRelayBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.REPEATER)
                    .strength(0.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> AUTO_TURRET = register("auto_turret",
            () -> new AutoTurretBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> SNIPER_TURRET = register("sniper_turret",
            () -> new SniperTurretBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(8.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> ENEMY_TURRET = register("enemy_turret",
            () -> new EnemyTurretBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(8.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> POLAR_GENERATOR = register("polar_generator",
            () -> new PolarGeneratorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.COW_BELL)
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> LIGHTNING_ROD_CONNECTOR = register("lightning_rod_connector",
            () -> new LightningRodConnectorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.LIGHTNING_ROD)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> LIGHTNING_BATTERY = register("lightning_battery",
            () -> new LightningBattery(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.COW_BELL)
                    .strength(3.0F)
                    .noOcclusion()
                    .lightLevel((state) -> state.getValue(LightningBattery.CHARGED) ? 15 : 0)));

    public static final DeferredBlock<Block> MACERATOR = register("macerator",
            () -> new MaceratorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.HAT)
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> POWERED_MACERATOR = register("powered_macerator",
            () -> new PoweredMaceratorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.HAT)
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> MECHANICAL_PRESS = register("mechanical_press",
            () -> new MechanicalPressBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BANJO)
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> POWERED_MECHANICAL_PRESS = register("powered_mechanical_press",
            () -> new PoweredMechanicalPressBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BANJO)
                    .strength(3.0F)
                    .noOcclusion()));
    // Research progression; placeholder model (the Gun Bench's)
    public static final DeferredBlock<Block> RESEARCH_STATION = register("research_station",
            () -> new ResearchStationBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CRAFTING_TABLE).instrument(NoteBlockInstrument.GUITAR)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> GUN_BENCH = register("gun_bench",
            () -> new GunBenchBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CRAFTING_TABLE).instrument(NoteBlockInstrument.GUITAR)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> DIAMOND_STEEL_GRATE = register("diamond_steel_grate",
            () -> new GrateBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> CHISELED_DIAMOND_STEEL_BLOCK = register("chiseled_diamond_steel_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));
    public static final DeferredBlock<Block> CUT_DIAMOND_STEEL = register("cut_diamond_steel",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F)));

    public static final DeferredBlock<Block> CUT_DIAMOND_STEEL_SLAB = register("cut_diamond_steel_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F)));

    public static final DeferredBlock<Block> CUT_DIAMOND_STEEL_STAIRS = register("cut_diamond_steel_stairs",
            () -> new StairBlock(ModBlocks.CUT_DIAMOND_STEEL.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F)));

    public static final DeferredBlock<Block> DIAMOND_STEEL_BARS = register("diamond_steel_bars",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> DIAMOND_STEEL_PANEL = register("diamond_steel_panel",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));

    public static final DeferredBlock<Block> DIAMOND_STEEL_PILLAR = register("diamond_steel_pillar",
            () -> new PillarBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));

    public static final DeferredBlock<Block> DIAMOND_STEEL_LAMP = register("diamond_steel_lamp",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)
                    .lightLevel((state) -> 15)));

    public static final DeferredBlock<Block> DIAMOND_STEEL_TILES = register("diamond_steel_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));
    public static final DeferredBlock<Block> DIAMOND_STEEL_TILES_SLAB = register("diamond_steel_tiles_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));

    public static final DeferredBlock<Block> DIAMOND_STEEL_TILES_STAIRS = register("diamond_steel_tiles_stairs",
            () -> new StairBlock(ModBlocks.DIAMOND_STEEL_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));

    public static final DeferredBlock<Block> ANTHRALITE_BLOCK = register("anthralite_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));

    public static final DeferredBlock<Block> ANTHRALITE_GRATE = register("anthralite_grate",
            () -> new GrateBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> ANTHRALITE_TILES = register("anthralite_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> ANTHRALITE_PLATES = register("anthralite_plates",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> CHISELED_ANTHRALITE_BLOCK = register("chiseled_anthralite_block",
            () -> new RedstoneLampBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .lightLevel((state) -> 0)));
    public static final DeferredBlock<Block> CUT_ANTHRALITE = register("cut_anthralite",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));

    public static final DeferredBlock<Block> CUT_ANTHRALITE_SLAB = register("cut_anthralite_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));
    public static final DeferredBlock<Block> CUT_ANTHRALITE_STAIRS = register("cut_anthralite_stairs",
            () -> new StairBlock(ModBlocks.CUT_ANTHRALITE.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));
    public static final DeferredBlock<Block> ANTHRALITE_TILES_SLAB = register("anthralite_tiles_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> ANTHRALITE_TILES_STAIRS = register("anthralite_tiles_stairs",
            () -> new StairBlock(ModBlocks.ANTHRALITE_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));

    public static final DeferredBlock<Block> ANTHRALITE_PILLAR = register("anthralite_pillar",
            () -> new PillarBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> ANTHRALITE_LAMP = register("anthralite_lamp",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .lightLevel((state) -> 15)));

    public static final DeferredBlock<Block> TREATED_IRON_BLOCK = register("treated_iron_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> CHISELED_TREATED_IRON_BLOCK = register("chiseled_treated_iron_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> CUT_TREATED_IRON = register("cut_treated_iron",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));

    public static final DeferredBlock<Block> TREATED_IRON_LAMP = register("treated_iron_lamp",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .lightLevel((state) -> 15)));

    public static final DeferredBlock<Block> CUT_TREATED_IRON_SLAB = register("cut_treated_iron_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));
    public static final DeferredBlock<Block> CUT_TREATED_IRON_STAIRS = register("cut_treated_iron_stairs",
            () -> new StairBlock(ModBlocks.CUT_TREATED_IRON.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)));

    public static final DeferredBlock<Block> TREATED_IRON_PLATES = register("treated_iron_plates",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> TREATED_IRON_BARS = register("treated_iron_bars",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TREATED_BRASS_BLOCK = register("treated_brass_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> VEHEMENT_COAL_BLOCK = registerBurnable("vehement_coal_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.COAL_BLOCK).instrument(NoteBlockInstrument.DIDGERIDOO)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)), 43200);

    public static final DeferredBlock<Block> PLASMA_BLOCK = registerBurnable("plasma_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel((state) -> 15)
                    .strength(3.0F)), 16200);

    public static final DeferredBlock<Block> DIAMOND_STEEL_BLOCK = register("diamond_steel_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> RAW_ANTHRALITE_BLOCK = register("raw_anthralite_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));
    public static final DeferredBlock<Block> ANTHRALITE_ORE = register("anthralite_ore",
            () -> new DropExperienceBlock(ConstantInt.of(0), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_ORE).mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
    public static final DeferredBlock<Block> DEEPSLATE_ANTHRALITE_ORE = register("deepslate_anthralite_ore",
            () -> new DropExperienceBlock(ConstantInt.of(0), BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_IRON_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> SULFUR_BLOCK = registerBurnable("sulfur_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)), 7200);
    public static final DeferredBlock<Block> ANCIENT_BRASS_BLOCK = register("ancient_brass_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.RAW_COPPER_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)));

    public static final DeferredBlock<Block> SKIBIDI = register("skibidi",
            () -> new BasicDirectionalBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.RAW_COPPER_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> FELIX_MEMORIAL = register("felix_memorial",
            () -> new MemorialBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANDESITE)
                    .requiresCorrectToolForDrops()
                    .strength(4.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TURRET_TARGETING_BLOCK = register("turret_targeting_module",
            () -> new TurretTargetingBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> PLAYER_TURRET_TARGETING_BLOCK = register("player_turret_targeting_module",
            () -> new PlayerTurretTargetingBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> HOSTILE_TURRET_TARGETING_BLOCK = register("hostile_turret_targeting_module",
            () -> new HostileTurretTargetingBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> FIRE_RATE_TURRET_MODULE = register("fire_rate_turret_module",
            () -> new FireRateModuleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> DAMAGE_TURRET_MODULE = register("damage_turret_module",
            () -> new DamageModuleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> RANGE_TURRET_MODULE = register("range_turret_module",
            () -> new RangeModuleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> SHELL_CATCHER_TURRET_MODULE = register("shell_catcher_turret_module",
            () -> new ShellCatcherModuleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> AMMO_TURRET_MODULE = register("ammo_turret_module",
            () -> new AmmoModuleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .randomTicks()
                    .noOcclusion()));

    public static final DeferredBlock<Block> RICH_PHOSPHORITE = register("rich_phosphorite",
            () -> new DropExperienceBlock(UniformInt.of(0, 1), BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)));
    public static final DeferredBlock<Block> SULFUR_ORE = register("sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
    public static final DeferredBlock<Block> DEEPSLATE_SULFUR_ORE = register("deepslate_sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofLegacyCopy(Blocks.COAL_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> NETHER_SULFUR_ORE = register("nether_sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 1), BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.NETHER_GOLD_ORE)));
    public static final DeferredBlock<Block> VEHEMENT_COAL_ORE = register("vehement_coal_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 1), BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.NETHER_GOLD_ORE)));

    public static final DeferredBlock<NiterLayerBlock> NITER_LAYER = register("niter",
            () -> new NiterLayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<SulfurLayerBlock> SULFUR_LAYER = register("sulfur",
            () -> new SulfurLayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<BatGuanoLayerBlock> BAT_GUANO_LAYER = register("bat_guano_layer",
            () -> new BatGuanoLayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<PhosphorLayerBlock> PHOSPHOR_LAYER = register("phosphor_layer",
            () -> new PhosphorLayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<Block> NITER_BLOCK = register("niter_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.SAND)));

    public static final DeferredBlock<Block> GUANO_CANDLE = register("guano_candle",
            () -> new GuanoCandleBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CANDLE)
                    .lightLevel((state) -> state.getValue(CandleBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> PENETRATOR = register("penetrator",
            () -> new PenetratorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .strength(1.0F), 10));

    public static final DeferredBlock<Block> PLASMA_LANTERN = register("plasma_lantern",
            () -> new LanternBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.LANTERN)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(3.0F)
                    .lightLevel((state) -> 15)));

    public static final DeferredBlock<Block> GEOTHERMAL_VENT = register("geothermal_vent",
            () -> new GeothermalVentBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .noOcclusion()
                    .randomTicks()));
    public static final DeferredBlock<Block> SULFUR_VENT = register("sulfur_vent",
            () -> new SulfurVentBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OBSIDIAN)
                    .requiresCorrectToolForDrops()
                    .strength(12.0F)
                    .noOcclusion()
                    .randomTicks()));

    public static final DeferredBlock<Block> VENT_COLLECTOR = register("vent_collector",
            () -> new VentCollectorBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE).noOcclusion()
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .noOcclusion()
                    .randomTicks()));

    public static final DeferredBlock<Block> NITER_GLASS = register("niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> WHITE_NITER_GLASS = register("white_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> ORANGE_NITER_GLASS = register("orange_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> MAGENTA_NITER_GLASS = register("magenta_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> LIGHT_BLUE_NITER_GLASS = register("light_blue_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> YELLOW_NITER_GLASS = register("yellow_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> LIME_NITER_GLASS = register("lime_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> PINK_NITER_GLASS = register("pink_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> GRAY_NITER_GLASS = register("gray_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> LIGHT_GRAY_NITER_GLASS = register("light_gray_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> CYAN_NITER_GLASS = register("cyan_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> PURPLE_NITER_GLASS = register("purple_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> BLUE_NITER_GLASS = register("blue_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> BROWN_NITER_GLASS = register("brown_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> GREEN_NITER_GLASS = register("green_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> RED_NITER_GLASS = register("red_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> BLACK_NITER_GLASS = register("black_niter_glass",
            () -> new NiterGlassBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS)));
    public static final DeferredBlock<Block> RAW_PHOSPHOR_BLOCK = register("raw_phosphor_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)));
    public static final DeferredBlock<Block> PHOSPHORITE = register("phosphorite",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F)));
    public static final DeferredBlock<Block> SMOOTH_PHOSPHORITE = register("smooth_phosphorite",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> POLISHED_PHOSPHORITE = register("polished_phosphorite",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> PHOSPHORITE_BRICKS = register("phosphorite_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> CRACKED_PHOSPHORITE_BRICKS = register("cracked_phosphorite_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> PHOSPHORITE_BRICK_SLAB = register("phosphorite_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> PHOSPHORITE_BRICK_STAIRS = register("phosphorite_brick_stairs",
            () -> new StairBlock(ModBlocks.PHOSPHORITE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));
    public static final DeferredBlock<Block> PHOSPHORITE_BRICK_WALL = register("phosphorite_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)));

    public static final DeferredBlock<Block> ASGHARIAN_PILLAR = register("asgharian_pillar",
            () -> new AsgharianPillarBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> ASGHARIAN_BRICKS = register("asgharian_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));

    public static final DeferredBlock<Block> REINFORCED_ASGHARIAN_TILES = register("reinforced_asgharian_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(6.0F)));

    public static final DeferredBlock<Block> ASGHARIAN_TILES = register("asgharian_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> CRACKED_ASGHARIAN_TILES = register("cracked_asgharian_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> MOSSY_ASGHARIAN_BRICKS = register("mossy_asgharian_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> MOSSY_ASGHARIAN_TILES = register("mossy_asgharian_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> CHISELED_ASGHARIAN_BRICKS = register("chiseled_asgharian_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> POLISHED_ASGHARIAN_PANEL = register("polished_asgharian_panel",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));

    public static final DeferredBlock<Block> CRACKED_ASGHARIAN_BRICKS = register("cracked_asgharian_bricks",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> ASGHARIAN_BRICK_SLAB = register("asgharian_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> ASGHARIAN_BRICK_STAIRS = register("asgharian_brick_stairs",
            () -> new StairBlock(ModBlocks.ASGHARIAN_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));
    public static final DeferredBlock<Block> ASGHARIAN_BRICK_WALL = register("asgharian_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING)
                    .strength(4.0F)));

    public static final DeferredBlock<Block> TREATED_BRASS_PLATES = register("treated_brass_plates",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)));
    public static final DeferredBlock<Block> CUT_TREATED_BRASS = register("cut_treated_brass",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(2.5F)));

    public static final DeferredBlock<Block> CUT_TREATED_BRASS_SLAB = register("cut_treated_brass_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(2.5F)));
    public static final DeferredBlock<Block> CUT_TREATED_BRASS_STAIRS = register("cut_treated_brass_stairs",
            () -> new StairBlock(ModBlocks.CUT_TREATED_BRASS.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(2.5F)));

    public static final DeferredBlock<Block> TREATED_IRON_GRATE = register("treated_iron_grate",
            () -> new GrateBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TREATED_IRON_GRATE_PANE = register("treated_iron_grate_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TREATED_BRASS_GRATE_PANE = register("treated_brass_grate_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> DIAMOND_STEEL_GRATE_PANE = register("diamond_steel_grate_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .strength(5.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> ANTHRALITE_GRATE_PANE = register("anthralite_grate_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TREATED_BRASS_GRATE = register("treated_brass_grate",
            () -> new GrateBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CHISELED_TREATED_BRASS_BLOCK = register("chiseled_treated_brass_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)));
    public static final DeferredBlock<Block> TREATED_BRASS_TILES = register("treated_brass_tiles",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)));
    public static final DeferredBlock<Block> TREATED_BRASS_TILES_SLAB = register("treated_brass_tiles_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)));
    public static final DeferredBlock<Block> TREATED_BRASS_TILES_STAIRS = register("treated_brass_tiles_stairs",
            () -> new StairBlock(ModBlocks.TREATED_BRASS_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)));

    public static final DeferredBlock<Block> TREATED_BRASS_LAMP = register("treated_brass_lamp",
            () -> new RedstoneLampBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.REDSTONE_LAMP)
                    .requiresCorrectToolForDrops().instrument(NoteBlockInstrument.FLUTE)
                    .strength(3.0F)
                    .lightLevel((state) -> state.getValue(RedstoneLampBlock.LIT) ? 15 : 0)));

    public static final DeferredBlock<Block> SCORCHED_BLOCK = register("scorched_block",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.5F)
                    .lightLevel((state) -> 10)));

    public static final DeferredBlock<Block> SANDBAG = register("sandbag",
            () -> new SandbagBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.SANDSTONE)
                    .strength(0.5F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> SUPPLY_CRATE = register("supply_crate",
            () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS)
                    .strength(1.0F)));

    private static <T extends Block> DeferredBlock<T> register(String id, Supplier<T> blockSupplier) {
        return register(id, blockSupplier, block1 -> new BlockItem(block1, new Item.Properties()));
    }

    private static <T extends Block> DeferredBlock<T> register(String id, Supplier<T> blockSupplier, @Nullable Function<T, BlockItem> supplier) {
        DeferredBlock<T> registryObject = REGISTER.register(id, blockSupplier);
        if (supplier != null) {
            ModItems.REGISTER.register(id, () -> supplier.apply(registryObject.get()));
        }
        return registryObject;
    }

    private static <T extends Block> DeferredBlock<T> registerBurnable(String id, Supplier<T> blockSupplier, int burnTime) {
        return register(id, blockSupplier, block -> new BlockItem(block, new Item.Properties()) {
            @Override
            public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
                return burnTime;
            }
        });
    }
}
