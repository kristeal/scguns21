package top.kristeal.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import top.kristeal.Reference;
import top.kristeal.init.ModBlocks;
import top.kristeal.research.ResearchHelper;
import top.kristeal.research.ResearchRecipe;

import java.util.Arrays;
import java.util.List;

/**
 * Research progression: JEI category for {@code scguns:research} recipes. Top row: blueprint, arrow,
 * researched entry and the research length. Second row: the total materials (fed in over the research). Bottom: the
 * prototype threshold.
 */
public class ResearchCategory implements IRecipeCategory<ResearchRecipe> {
    public static final ResourceLocation UID = Reference.id("research");
    public static final RecipeType<ResearchRecipe> RESEARCH_TYPE = new RecipeType<>(UID, ResearchRecipe.class);

    private static final int WIDTH = 160, HEIGHT = 60;
    private static final int MATERIALS_Y = 24, MATERIAL_SPACING = 20;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public ResearchCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.RESEARCH_STATION.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<ResearchRecipe> getRecipeType() {
        return RESEARCH_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scguns.research.title");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ResearchRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1)
                .addItemStack(new ItemStack(recipe.getBlueprint()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.scguns.research.blueprint_kept").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 49, 1)
                .addItemStack(new ItemStack(recipe.getEntry()));

        List<SizedIngredient> materials = recipe.getMaterials();
        for (int i = 0; i < materials.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * MATERIAL_SPACING, MATERIALS_Y)
                    .addItemStacks(scaled(materials.get(i), recipe.getTotalCount(i)));
        }
    }

    @Override
    public void draw(ResearchRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        slot.draw(graphics, 0, 0);
        arrow.draw(graphics, 22, 1);
        slot.draw(graphics, 48, 0);
        for (int i = 0; i < recipe.getMaterials().size(); i++) {
            slot.draw(graphics, i * MATERIAL_SPACING, MATERIALS_Y - 1);
        }

        var font = Minecraft.getInstance().font;
        int seconds = ResearchHelper.getStepTime(recipe) * recipe.getSteps() / 20;
        Component time = Component.translatable("jei.scguns.research.time", seconds / 60, String.format("%02d", seconds % 60));
        graphics.drawString(font, time, 72, 5, 0x404040, false);
        int energy = ResearchHelper.getEnergyPerTick();
        if (energy > 0) {
            graphics.drawString(font, Component.translatable("jei.scguns.research.energy", energy), 72, 14, 0x606060, false);
        }
        Component prototype = Component.translatable("jei.scguns.research.prototype",
                Math.round(ResearchHelper.prototypeThreshold() * 100));
        graphics.drawString(font, prototype, 1, HEIGHT - 10, 0x606060, false);
    }

    private static java.util.List<net.minecraft.world.item.ItemStack> scaled(SizedIngredient material, int count) {
        return Arrays.stream(material.getItems()).map(stack -> stack.copyWithCount(count)).toList();
    }
}
