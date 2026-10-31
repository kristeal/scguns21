package top.kristeal.compat;
import top.kristeal.progression.ProgressionMode;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.ChatFormatting;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import top.kristeal.Reference;
import top.kristeal.client.screen.GunBenchRecipe;
import top.kristeal.init.ModBlocks;

import java.awt.*;

public class GunBenchCategory implements IRecipeCategory<GunBenchRecipe> {
    public static final ResourceLocation UID = Reference.id("gun_bench");
    public static final ResourceLocation TEXTURE = Reference.id("textures/gui/gun_bench_gui.png");
    public static final RecipeType<GunBenchRecipe> GUN_BENCH_TYPE = new RecipeType<>(UID, GunBenchRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;
    private final int offsetX = 22;
    private final int offsetY = 12;
    private final int slotOffsetX = -22;
    private final int slotOffsetY = -12;
    public GunBenchCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, offsetX, offsetY, 141, 59);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.GUN_BENCH.get()));
    }

    @Override
    public RecipeType<GunBenchRecipe> getRecipeType() {
        return GUN_BENCH_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.scguns.gun_bench");
    }

    // Port note: JEI 19 deprecates getBackground() for removal; the size is reported here and the same
    // texture region is drawn first in draw()
    @Override
    public int getWidth() {
        return this.background.getWidth();
    }

    @Override
    public int getHeight() {
        return this.background.getHeight();
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GunBenchRecipe recipe, IFocusGroup focuses) {
        int[] slotX = {26, 44, 62, 80, 26, 44, 62, 80, 26, 62, 116};
        int[] slotY = {17, 17, 17, 17, 35, 35, 35, 35, 53, 53, 17}; // Add the position for the blueprint slot

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            if (!ingredients.get(i).isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, slotX[i] + slotOffsetX, slotY[i] + slotOffsetY)
                        .addIngredients(ingredients.get(i));
            }
        }

        // Add blueprint slot
        Ingredient blueprint = recipe.getBlueprint();
        if (!blueprint.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX[10] + slotOffsetX, slotY[10] + slotOffsetY)
                    .addIngredients(blueprint)
                    // Research mode: the blueprint must have this entry researched
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        if (ProgressionMode.isResearch()) {
                            tooltip.add(Component.translatable("jei.scguns.gun_bench.needs_research").withStyle(ChatFormatting.GOLD));
                        }
                    });
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 140 + slotOffsetX, 44 + slotOffsetY)
                .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(GunBenchRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics);
    }
}
