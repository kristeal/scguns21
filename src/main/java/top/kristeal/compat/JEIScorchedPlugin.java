package top.kristeal.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import top.kristeal.Reference;
import top.kristeal.client.screen.*;
import top.kristeal.init.ModBlocks;
import top.kristeal.init.ModItems;
import top.kristeal.progression.ProgressionMode;
import top.kristeal.research.ResearchHelper;
import mezz.jei.api.runtime.IJeiRuntime;
import javax.annotation.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@JeiPlugin
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class JEIScorchedPlugin implements IModPlugin {
    @Nullable private static IJeiRuntime runtime;
    private static boolean researchHidden;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        researchHidden = false;
        onProgressionModeChanged();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Raid mode hides the research category and the Research Station; Research mode shows them again. */
    public static void onProgressionModeChanged() {
        if (runtime == null) {
            return;
        }
        boolean hide = !ProgressionMode.isResearch();
        if (hide == researchHidden) {
            return;
        }
        researchHidden = hide;
        List<ItemStack> station = List.of(new ItemStack(ModBlocks.RESEARCH_STATION.get()));
        if (hide) {
            runtime.getRecipeManager().hideRecipeCategory(ResearchCategory.RESEARCH_TYPE);
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, station);
        } else {
            runtime.getRecipeManager().unhideRecipeCategory(ResearchCategory.RESEARCH_TYPE);
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, station);
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Reference.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GunBenchCategory(guiHelper));
        registration.addRecipeCategories(new MaceratorCategory(guiHelper));
        registration.addRecipeCategories(new PoweredMaceratorCategory(guiHelper));
        registration.addRecipeCategories(new MechanicalPressCategory(guiHelper));
        registration.addRecipeCategories(new PoweredMechanicalPressCategory(guiHelper));
        registration.addRecipeCategories(new LightningBatteryCategory(guiHelper));
        registration.addRecipeCategories(new ResearchCategory(guiHelper)); // research progression

    }
    public static MutableComponent getTranslation(String key, Object... args) {
        return Component.translatable("scguns." + key, args);
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        assert Minecraft.getInstance().level != null;
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        registration.addIngredientInfo(new ItemStack(ModItems.REPAIR_KIT.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.repair_kit"));

        registration.addIngredientInfo(new ItemStack(ModBlocks.VENT_COLLECTOR.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.vent_collector"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.NITER_GLASS.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.niter_glass"));
        registration.addIngredientInfo(new ItemStack(ModItems.BLASPHEMY.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.blasphemy"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.LIGHTNING_BATTERY.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.lightning_battery"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.LIGHTNING_ROD_CONNECTOR.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.lightning_rod_connector"));
        registration.addIngredientInfo(new ItemStack(ModItems.PISTOL_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.pistol_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.RIFLE_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.rifle_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.SHOTGUN_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.shotgun_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.MAGNUM_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.magnum_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.ENERGY_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.energy_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.ROCKET_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.rocket_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.SPECIAL_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.special_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.EMPTY_CASING_POUCH.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.empty_casing_pouch"));
        registration.addIngredientInfo(new ItemStack(ModItems.CREATIVE_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.creative_ammo_box"));
        registration.addIngredientInfo(new ItemStack(ModItems.DISHES_POUCH.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.dishes_pouch"));
        registration.addIngredientInfo(new ItemStack(ModItems.ROCK_POUCH.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.rock_pouch"));
        registration.addIngredientInfo(new ItemStack(ModItems.ANCIENT_BRASS.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.ancient_brass"));


        registration.addIngredientInfo(new ItemStack(ModBlocks.BASIC_TURRET.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.basic_turret"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.AUTO_TURRET.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.auto_turret"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.SHOTGUN_TURRET.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.shotgun_turret"));
        registration.addIngredientInfo(new ItemStack(ModItems.TEAM_LOG.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.team_log"));
        registration.addIngredientInfo(new ItemStack(ModItems.ENEMY_LOG.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.enemy_log"));

        registration.addIngredientInfo(new ItemStack(ModItems.SUPER_SHOTGUN.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.super_shotgun"));
        registration.addIngredientInfo(new ItemStack(ModItems.AUREOUS_SLAG.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.aureous_slag"));

        registration.addIngredientInfo(new ItemStack(ModBlocks.HOSTILE_TURRET_TARGETING_BLOCK.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.hostile_turret_targeting_block"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.PLAYER_TURRET_TARGETING_BLOCK.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.player_turret_targeting_block"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.TURRET_TARGETING_BLOCK.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.turret_targeting_block"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.FIRE_RATE_TURRET_MODULE.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.fire_rate_turret_module"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.DAMAGE_TURRET_MODULE.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.damage_turret_module"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.RANGE_TURRET_MODULE.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.range_turret_module"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.SHELL_CATCHER_TURRET_MODULE.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.shell_catcher_turret_module"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.CRYONITER.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.cryoniter"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.THERMOLITH.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.thermolith"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.PENETRATOR.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.penetrator"));

        registration.addIngredientInfo(new ItemStack(ModItems.NITER_DUST.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.niter_dust"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.ADVANCED_COMPOSTER.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.advanced_composter"));



        List<GunBenchRecipe> gunBenchRecipes = recipeManager.getAllRecipesFor(GunBenchRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
        List<MaceratorRecipe> maceratorRecipes = recipeManager.getAllRecipesFor(MaceratorRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
        List<PoweredMaceratorRecipe> poweredMaceratorRecipes = recipeManager.getAllRecipesFor(PoweredMaceratorRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
        List<MechanicalPressRecipe> mechanicalPressRecipes = recipeManager.getAllRecipesFor(MechanicalPressRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();

        List<PoweredMechanicalPressRecipe> poweredMechanicalPressRecipes = recipeManager.getAllRecipesFor(PoweredMechanicalPressRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
        registration.addRecipes(GunBenchCategory.GUN_BENCH_TYPE, gunBenchRecipes);
        registration.addRecipes(MaceratorCategory.MACERATING_TYPE, maceratorRecipes);
        registration.addRecipes(PoweredMaceratorCategory.POWERED_MACERATING_TYPE, poweredMaceratorRecipes);
        registration.addRecipes(MechanicalPressCategory.MECHANICAL_PRESS_TYPE, mechanicalPressRecipes);
        registration.addRecipes(PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE, poweredMechanicalPressRecipes);
        registration.addRecipes(LightningBatteryCategory.LIGHTNING_BATTERY_TYPE, recipeManager.getAllRecipesFor(LightningBatteryRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList());

        // Research recipes and a short blueprint description (sources are left to loot addons). The text follows the
        // progression mode at the time JEI loads; a mode switch shows the other one after the next rejoin or /reload.
        registration.addRecipes(ResearchCategory.RESEARCH_TYPE, ResearchHelper.getRecipes(recipeManager).stream().map(RecipeHolder::value).toList());
        for (var blueprint : List.of(ModItems.ANTIQUE_BLUEPRINT, ModItems.FRONTIER_BLUEPRINT, ModItems.COPPER_BLUEPRINT, ModItems.IRON_BLUEPRINT,
                ModItems.WRECKER_BLUEPRINT, ModItems.TREATED_BRASS_BLUEPRINT, ModItems.DIAMOND_STEEL_BLUEPRINT, ModItems.PIGLIN_BLUEPRINT,
                ModItems.OCEAN_BLUEPRINT, ModItems.DEEP_DARK_BLUEPRINT, ModItems.END_BLUEPRINT, ModItems.SCORCHED_BLUEPRINT, ModItems.EXO_SUIT_BLUEPRINT)) {
            registration.addIngredientInfo(new ItemStack(blueprint.get()), VanillaTypes.ITEM_STACK,
                    getTranslation(ProgressionMode.isResearch() ? "jei.info.blueprint_research" : "jei.info.blueprint_raid"));
        }
        registration.addIngredientInfo(new ItemStack(ModBlocks.RESEARCH_STATION.get()), VanillaTypes.ITEM_STACK, getTranslation("jei.info.research_station"));


    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(GunBenchScreen.class, 100, 47, 30, 20, GunBenchCategory.GUN_BENCH_TYPE);
        registration.addRecipeClickArea(ResearchStationScreen.class, 152, 82, 17, 15, ResearchCategory.RESEARCH_TYPE); // The arrow to the blueprint slot
        // R/U on the station's (drawn) material slots
        registration.addGuiContainerHandler(ResearchStationScreen.class, new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() {
            @Override
            public java.util.Optional<? extends mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    mezz.jei.api.gui.builder.IClickableIngredientFactory builder, ResearchStationScreen screen, double mouseX, double mouseY) {
                ResearchStationScreen.MaterialHit hit = screen.getMaterialUnderMouse(mouseX, mouseY);
                if (hit == null) {
                    return java.util.Optional.empty();
                }
                return builder.createBuilder(hit.stack()).buildWithArea(hit.area());
            }
        });
        registration.addRecipeClickArea(MaceratorScreen.class, 80, 25, 30, 20, MaceratorCategory.MACERATING_TYPE);
        registration.addRecipeClickArea(PoweredMaceratorScreen.class, 80, 25, 25, 20, PoweredMaceratorCategory.POWERED_MACERATING_TYPE);
        registration.addRecipeClickArea(MechanicalPressScreen.class, 80, 25, 25, 20, MechanicalPressCategory.MECHANICAL_PRESS_TYPE);
        registration.addRecipeClickArea(PoweredMechanicalPressScreen.class, 80, 25, 25, 20, PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE);
        registration.addRecipeClickArea(LightningBatteryScreen.class, 80, 32, 25, 20, LightningBatteryCategory.LIGHTNING_BATTERY_TYPE);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GUN_BENCH.get()), GunBenchCategory.GUN_BENCH_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RESEARCH_STATION.get()), ResearchCategory.RESEARCH_TYPE); // research progression
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACERATOR.get()), MaceratorCategory.MACERATING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.POWERED_MACERATOR.get()), PoweredMaceratorCategory.POWERED_MACERATING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MECHANICAL_PRESS.get()), MechanicalPressCategory.MECHANICAL_PRESS_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.POWERED_MECHANICAL_PRESS.get()), PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LIGHTNING_BATTERY.get()), LightningBatteryCategory.LIGHTNING_BATTERY_TYPE);
    }
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new GunBenchTransferInfo());
    }
}


