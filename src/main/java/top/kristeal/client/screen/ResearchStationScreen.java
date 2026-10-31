package top.kristeal.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.Reference;
import top.kristeal.blockentity.ResearchStationBlockEntity;
import top.kristeal.item.GunItem;
import top.kristeal.research.ResearchHelper;
import top.kristeal.research.ResearchRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Research progression: the Research Station screen, drawn from {@code textures/gui/research_station_gui.png}
 * (hand-painted; the positions below must match it).
 * Top: the blueprint's entries (3 rows, scrollbar) and the FE gauge. Middle: the 6 material slots (an empty one shows
 * a ghost of its material; under each, "provided/still needed"), the arrow (JEI recipe area), the blueprint slot and
 * the Start/Stop button (its disabled look is painted into the background). Bottom: the progress bar.
 */
public class ResearchStationScreen extends AbstractContainerScreen<ResearchStationMenu> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/gui/research_station_gui.png");

    // layout (screen-relative)
    private static final int LIST_X = 36, LIST_Y = 15, LIST_WIDTH = 143, ROW_HEIGHT = 20, VISIBLE_ROWS = 3;
    private static final int SCROLL_X = 181, SCROLL_HEIGHT = 60;
    private static final int COUNT_Y = ResearchStationMenu.MATERIALS_Y + 18;
    private static final int BUTTON_X = 201, BUTTON_Y = 82, BUTTON_SIZE = 13;
    private static final int BAR_X = 30, BAR_Y = 111, BAR_WIDTH = 166, BAR_HEIGHT = 4;
    private static final int ENERGY_X = 205, ENERGY_Y = 15, ENERGY_WIDTH = 4, ENERGY_HEIGHT = 60;

    // sprites (texture-relative)
    private static final int ICON_LOCKED_U = 236, ICON_LOCKED_V = 230, ICON_PROTOTYPE_U = 228, ICON_PROTOTYPE_V = 230;
    private static final int ICON_RESEARCHED_U = 236, ICON_RESEARCHED_V = 222, ICON_SIZE = 7;
    private static final int KNOB_U = 244, KNOB_OFF_U = 250, KNOB_V = 222, KNOB_WIDTH = 6, KNOB_HEIGHT = 15;
    private static final int BUTTON_U = 193, BUTTON_HOVER_U = 177, BUTTON_V = 224, BUTTON_SPRITE_OFFSET = -1;
    private static final int BAR_U = 1, BAR_V = 223;
    private static final int ENERGY_U = 233, ENERGY_V = 15;
    private static final int ROW_SELECTED_U = 0, ROW_HOVER_U = 136, ROW_V = 240, ROW_SPRITE_HEIGHT = 16;
    private static final int ROW_SELECTED_WIDTH = 136, ROW_HOVER_WIDTH = 120;

    private static final int TEXT_DARK = 0x1F3A5F;
    private static final int TEXT_OK = 0x1F6F2A;
    private static final int TEXT_BAD = 0x9F1F1F;
    private static final int PROTOTYPE_MARK = 0xFFC9921C;

    private int scroll;
    private boolean draggingScrollbar;

    public ResearchStationScreen(ResearchStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 222;
        this.inventoryLabelX = ResearchStationMenu.INVENTORY_X;
        this.inventoryLabelY = ResearchStationMenu.INVENTORY_Y - 11;
    }

    /* ---- data ---- */

    private int maxScroll(int entryCount) {
        return Math.max(0, entryCount - VISIBLE_ROWS);
    }

    private boolean canToggle(@Nullable ResearchRecipe selected) {
        return selected != null && ResearchHelper.getState(menu.getBlueprint(), selected) != ResearchHelper.State.RESEARCHED;
    }

    /** Guns get a prototype at the threshold; other entries (ExoSuit pieces) need the full research. */
    private static boolean hasPrototype(ResearchRecipe entry) {
        return entry.getEntry() instanceof GunItem;
    }

    /** The ghost shown in an empty material slot (cycles through an ingredient's options every second). */
    private static ItemStack shownMaterial(SizedIngredient material) {
        ItemStack[] options = material.ingredient().getItems();
        if (options.length == 0) {
            return ItemStack.EMPTY;
        }
        return options[(int) (System.currentTimeMillis() / 1000 % options.length)].copyWithCount(1);
    }

    /** How many of material {@code m} its slot holds (0 if it holds something else). */
    private int provided(SizedIngredient material, int m) {
        ItemStack stack = menu.getMaterialStack(m);
        return !stack.isEmpty() && material.ingredient().test(stack) ? stack.getCount() : 0;
    }

    /** Index of the material slot under the mouse, or -1. */
    private int materialSlotAt(double mouseX, double mouseY) {
        for (int m = 0; m < ResearchStationBlockEntity.MATERIAL_SLOTS; m++) {
            int x = leftPos + ResearchStationMenu.MATERIALS_X + m * ResearchStationMenu.MATERIAL_SPACING;
            int y = topPos + ResearchStationMenu.MATERIALS_Y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return m;
            }
        }
        return -1;
    }

    /** For JEI: the ghost material under the mouse (empty material slots only) and its on-screen area, or null. */
    @Nullable
    public MaterialHit getMaterialUnderMouse(double mouseX, double mouseY) {
        ResearchRecipe selected = menu.getSelectedRecipe();
        int m = materialSlotAt(mouseX, mouseY);
        if (selected == null || m < 0 || m >= selected.getMaterials().size() || !menu.getMaterialStack(m).isEmpty()) {
            return null;
        }
        ItemStack shown = shownMaterial(selected.getMaterials().get(m));
        int x = leftPos + ResearchStationMenu.MATERIALS_X + m * ResearchStationMenu.MATERIAL_SPACING;
        return shown.isEmpty() ? null : new MaterialHit(shown, new Rect2i(x, topPos + ResearchStationMenu.MATERIALS_Y, 16, 16));
    }

    public record MaterialHit(ItemStack stack, Rect2i area) {}

    /* ---- drawing ---- */

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderGhosts(graphics);
    }

    /** Ghosts of the selected entry's materials in empty slots; empty slots it doesn't use are dimmed. */
    private void renderGhosts(GuiGraphics graphics) {
        ResearchRecipe selected = menu.getSelectedRecipe();
        int used = selected == null ? 0 : selected.getMaterials().size();
        for (int m = 0; m < ResearchStationBlockEntity.MATERIAL_SLOTS; m++) {
            if (!menu.getMaterialStack(m).isEmpty()) {
                continue;
            }
            int x = leftPos + ResearchStationMenu.MATERIALS_X + m * ResearchStationMenu.MATERIAL_SPACING;
            int y = topPos + ResearchStationMenu.MATERIALS_Y;
            if (m >= used) {
                graphics.fill(x, y, x + 16, y + 16, 0x304F7FB3);
            } else {
                graphics.renderFakeItem(shownMaterial(selected.getMaterials().get(m)), x, y);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x, y, x + 16, y + 16, 0xA0A7CFEF);
                graphics.pose().popPose();
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_DARK, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<ResearchRecipe> entries = menu.getEntries();
        ItemStack blueprint = menu.getBlueprint();
        scroll = Mth.clamp(scroll, 0, maxScroll(entries.size()));

        List<Component> tooltip = renderList(graphics, mouseX, mouseY, entries, blueprint);
        renderScrollbar(graphics, entries.size());

        ResearchRecipe selected = menu.getSelectedRecipe();
        if (selected != null) {
            List<Component> selectedTooltip = renderSelected(graphics, mouseX, mouseY, blueprint, selected);
            if (tooltip == null) {
                tooltip = selectedTooltip;
            }
        }
        List<Component> buttonTooltip = renderButton(graphics, mouseX, mouseY, selected);
        if (tooltip == null) {
            tooltip = buttonTooltip;
        }
        List<Component> energyTooltip = renderEnergy(graphics, mouseX, mouseY);
        if (tooltip == null) {
            tooltip = energyTooltip;
        }

        if (tooltip != null) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Draws the FE gauge; returns its tooltip when hovered. */
    @Nullable
    private List<Component> renderEnergy(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + ENERGY_X, y = topPos + ENERGY_Y;
        int energy = menu.getEnergy();
        int filled = Math.round(ENERGY_HEIGHT * Mth.clamp(energy / (float) ResearchStationBlockEntity.ENERGY_CAPACITY, 0, 1));
        if (filled > 0) {
            int top = ENERGY_HEIGHT - filled;
            graphics.blit(TEXTURE, x, y + top, ENERGY_U, ENERGY_V + top, ENERGY_WIDTH, filled);
        }
        // the 4 px gauge is hard to hit, so its frame counts too
        if (mouseX < x - 1 || mouseX >= x + ENERGY_WIDTH + 1 || mouseY < y - 1 || mouseY >= y + ENERGY_HEIGHT + 1) {
            return null;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.scguns.research_station.tooltip.energy", energy, ResearchStationBlockEntity.ENERGY_CAPACITY));
        int cost = ResearchHelper.getEnergyPerTick();
        lines.add((cost > 0
                ? Component.translatable("screen.scguns.research_station.tooltip.energy_cost", cost)
                : Component.translatable("screen.scguns.research_station.tooltip.energy_free")).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /** Draws the entry rows; returns the tooltip of the hovered row, if any. */
    @Nullable
    private List<Component> renderList(GuiGraphics graphics, int mouseX, int mouseY, List<ResearchRecipe> entries, ItemStack blueprint) {
        int listX = leftPos + LIST_X, listY = topPos + LIST_Y;
        if (blueprint.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.scguns.research_station.insert_blueprint"), listX + 4, listY + 4, LIST_WIDTH - 8, TEXT_DARK);
            return null;
        }
        if (entries.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.scguns.research_station.nothing_to_research"), listX + 4, listY + 4, LIST_WIDTH - 8, TEXT_DARK);
            return null;
        }

        List<Component> tooltip = null;
        for (int row = 0; row < VISIBLE_ROWS && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            ResearchRecipe entry = entries.get(index);
            int ry = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= ry && mouseY < ry + ROW_HEIGHT;
            int highlightX = listX + (LIST_WIDTH - ROW_SELECTED_WIDTH) / 2;
            if (index == menu.getSelectedIndex()) {
                graphics.blit(TEXTURE, highlightX, ry + 2, ROW_SELECTED_U, ROW_V, ROW_SELECTED_WIDTH, ROW_SPRITE_HEIGHT);
            } else if (hovered) {
                graphics.blit(TEXTURE, highlightX, ry + 2, ROW_HOVER_U, ROW_V, ROW_HOVER_WIDTH, ROW_SPRITE_HEIGHT);
            }

            ItemStack icon = new ItemStack(entry.getEntry());
            graphics.renderItem(icon, listX + 2, ry + 2);
            ResearchHelper.State state = ResearchHelper.getState(blueprint, entry);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200); // above the item
            graphics.blit(TEXTURE, listX + 12, ry + 12, iconU(state), iconV(state), ICON_SIZE, ICON_SIZE);
            graphics.pose().popPose();

            String percent = Math.round(ResearchHelper.getProgress(blueprint, entry) * 100) + "%";
            int nameWidth = LIST_WIDTH - 27 - font.width(percent);
            graphics.drawString(font, font.plainSubstrByWidth(icon.getHoverName().getString(), nameWidth), listX + 21, ry + 6, TEXT_DARK, false);
            graphics.drawString(font, percent, listX + LIST_WIDTH - 4 - font.width(percent), ry + 6, TEXT_DARK, false);

            if (hovered) {
                List<Component> lines = new ArrayList<>();
                lines.add(icon.getHoverName());
                lines.add(Component.translatable("screen.scguns.research_station.tooltip." + state.name().toLowerCase())
                        .withStyle(state == ResearchHelper.State.RESEARCHED ? ChatFormatting.GREEN
                                : state == ResearchHelper.State.PROTOTYPE ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                lines.addAll(progressTooltip(blueprint, entry));
                tooltip = lines;
            }
        }
        return tooltip;
    }

    /** % researched and the time left at this server's speed. */
    private List<Component> progressTooltip(ItemStack blueprint, ResearchRecipe entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.scguns.research_station.tooltip.progress",
                Math.round(ResearchHelper.getProgress(blueprint, entry) * 100)).withStyle(ChatFormatting.GRAY));
        int done = ResearchHelper.getSteps(blueprint, entry.getEntry());
        if (done < entry.getSteps()) {
            int ticksLeft = (entry.getSteps() - done) * ResearchHelper.getStepTime(entry);
            lines.add(Component.translatable("screen.scguns.research_station.tooltip.time_left", formatTicks(ticksLeft)).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private void renderScrollbar(GuiGraphics graphics, int entryCount) {
        int max = maxScroll(entryCount);
        int knobY = topPos + LIST_Y + (max == 0 ? 0 : (SCROLL_HEIGHT - KNOB_HEIGHT) * scroll / max);
        graphics.blit(TEXTURE, leftPos + SCROLL_X, knobY, max == 0 ? KNOB_OFF_U : KNOB_U, KNOB_V, KNOB_WIDTH, KNOB_HEIGHT);
    }

    /**
     * Draws the counts under the material slots and the progress bar; returns the tooltip of the hovered empty
     * material slot or of the bar, if any.
     */
    @Nullable
    private List<Component> renderSelected(GuiGraphics graphics, int mouseX, int mouseY, ItemStack blueprint, ResearchRecipe selected) {
        int x = leftPos, y = topPos;
        int done = ResearchHelper.getSteps(blueprint, selected.getEntry());
        boolean complete = done >= selected.getSteps();
        List<Component> tooltip = null;

        if (!complete) {
            List<SizedIngredient> materials = selected.getMaterials();
            List<Integer> stepCounts = selected.getStepCounts(done + 1);
            List<Integer> remaining = selected.getRemainingCounts(done);
            int hoveredSlot = materialSlotAt(mouseX, mouseY);
            for (int m = 0; m < materials.size() && m < ResearchStationBlockEntity.MATERIAL_SLOTS; m++) {
                int have = provided(materials.get(m), m);
                int left = remaining.get(m);
                int step = stepCounts.get(m);
                String text = have + "/" + left;
                int color = have >= left ? TEXT_OK : have >= step ? TEXT_DARK : TEXT_BAD;
                int cx = x + ResearchStationMenu.MATERIALS_X + m * ResearchStationMenu.MATERIAL_SPACING + 8;
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1.0F);
                graphics.drawString(font, text, cx * 2 - font.width(text) / 2, (y + COUNT_Y) * 2, color, false);
                graphics.pose().popPose();

                if (m == hoveredSlot && menu.getMaterialStack(m).isEmpty()) {
                    tooltip = new ArrayList<>();
                    tooltip.add(shownMaterial(materials.get(m)).getHoverName());
                    tooltip.add(Component.translatable("screen.scguns.research_station.tooltip.needed_now", step).withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("screen.scguns.research_station.tooltip.still_needed", left).withStyle(ChatFormatting.GRAY));
                }
            }
        }

        // progress bar: the whole research, with the running step filling in smoothly
        float stepFraction = complete || menu.getStepTime() <= 0 ? 0 : Mth.clamp(menu.getProgress() / (float) menu.getStepTime(), 0, 1);
        float progress = Math.min(1, (done + stepFraction) / selected.getSteps());
        int bx = x + BAR_X, by = y + BAR_Y;
        int filled = Math.round(BAR_WIDTH * progress);
        if (filled > 0) {
            graphics.blit(TEXTURE, bx, by, BAR_U, BAR_V, filled, BAR_HEIGHT);
        }
        if (hasPrototype(selected)) {
            int mark = bx + (int) Math.round(BAR_WIDTH * ResearchHelper.prototypeThreshold());
            graphics.fill(mark, by - 1, mark + 1, by + BAR_HEIGHT + 1, PROTOTYPE_MARK);
        }
        if (mouseX >= bx && mouseX < bx + BAR_WIDTH && mouseY >= by - 2 && mouseY < by + BAR_HEIGHT + 2) {
            tooltip = progressTooltip(blueprint, selected);
        }
        return tooltip;
    }

    /** Draws the Start/Stop button over its painted disabled look when usable; returns its label when hovered. */
    @Nullable
    private List<Component> renderButton(GuiGraphics graphics, int mouseX, int mouseY, @Nullable ResearchRecipe selected) {
        boolean active = canToggle(selected);
        boolean hovered = isOverButton(mouseX, mouseY);
        if (active) {
            graphics.blit(TEXTURE, leftPos + BUTTON_X + BUTTON_SPRITE_OFFSET, topPos + BUTTON_Y, hovered ? BUTTON_HOVER_U : BUTTON_U, BUTTON_V, BUTTON_SIZE, BUTTON_SIZE);
        }
        if (!hovered) {
            return null;
        }
        Component label = Component.translatable(menu.isRunning() ? "screen.scguns.research_station.stop" : "screen.scguns.research_station.start");
        return List.of(active ? label : label.copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int iconU(ResearchHelper.State state) {
        return switch (state) {
            case RESEARCHED -> ICON_RESEARCHED_U;
            case PROTOTYPE -> ICON_PROTOTYPE_U;
            case LOCKED -> ICON_LOCKED_U;
        };
    }

    private static int iconV(ResearchHelper.State state) {
        return switch (state) {
            case RESEARCHED -> ICON_RESEARCHED_V;
            case PROTOTYPE -> ICON_PROTOTYPE_V;
            case LOCKED -> ICON_LOCKED_V;
        };
    }

    private static String formatTicks(int ticks) {
        int seconds = (ticks + 19) / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    /* ---- input ---- */

    private boolean isOverButton(double mouseX, double mouseY) {
        int bx = leftPos + BUTTON_X, by = topPos + BUTTON_Y;
        return mouseX >= bx && mouseX < bx + BUTTON_SIZE && mouseY >= by && mouseY < by + BUTTON_SIZE;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        return mouseX >= lx && mouseX < lx + LIST_WIDTH && mouseY >= ly && mouseY < ly + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int sx = leftPos + SCROLL_X, sy = topPos + LIST_Y;
        return mouseX >= sx && mouseX < sx + KNOB_WIDTH && mouseY >= sy && mouseY < sy + SCROLL_HEIGHT;
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void scrollTo(double mouseY, int entryCount) {
        int max = maxScroll(entryCount);
        float position = (float) (mouseY - topPos - LIST_Y - KNOB_HEIGHT / 2.0) / (SCROLL_HEIGHT - KNOB_HEIGHT);
        scroll = Mth.clamp(Math.round(position * max), 0, max);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ResearchRecipe> entries = menu.getEntries();
            if (isOverButton(mouseX, mouseY) && canToggle(menu.getSelectedRecipe())) {
                click(ResearchStationMenu.BUTTON_TOGGLE);
                return true;
            }
            if (isOverScrollbar(mouseX, mouseY) && maxScroll(entries.size()) > 0) {
                draggingScrollbar = true;
                scrollTo(mouseY, entries.size());
                return true;
            }
            if (isOverList(mouseX, mouseY)) {
                int index = scroll + (int) ((mouseY - topPos - LIST_Y) / ROW_HEIGHT);
                if (index < entries.size()) {
                    click(ResearchStationMenu.BUTTON_SELECT + index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollTo(mouseY, menu.getEntries().size());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverList(mouseX, mouseY) || isOverScrollbar(mouseX, mouseY)) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll(menu.getEntries().size()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
