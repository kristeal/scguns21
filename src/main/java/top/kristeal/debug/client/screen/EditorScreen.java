package top.kristeal.debug.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import top.kristeal.Reference;
import top.kristeal.debug.IDebugWidget;
import top.kristeal.debug.IEditorMenu;
import top.kristeal.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 * <p>
 * Port notes (1.21):
 * <ul>
 *     <li>The window is drawn with {@link GuiGraphics#blit} (stretched regions) instead of hand-built POSITION_TEX quads.</li>
 *     <li>{@link Screen#render} now draws the blurred menu background first; the reference's screen had none, so
 *     {@link #renderBackground} is a no-op and the world stays visible behind the window.</li>
 *     <li>The property list positions itself with {@code setX}/{@code getY}/{@code getBottom} (the 1.20 {@code x0}/{@code y0}
 *     fields are gone), scissors itself, and its menu background and separators are switched off
 *     (the reference called {@code setRenderBackground(false)}/{@code setRenderTopAndBottom(false)}).</li>
 * </ul>
 */
public class EditorScreen extends Screen
{
    private static final ResourceLocation WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/debug.png");
    private static final int WIDTH = 150;

    private final Screen parent;
    private final IEditorMenu menu;
    private PropertyList list;
    private int windowWidth, windowHeight;
    private int windowLeft, windowTop;

    public EditorScreen(Screen parent, IEditorMenu menu)
    {
        super(menu.getEditorLabel());
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init()
    {
        List<Pair<Component, Supplier<IDebugWidget>>> widgets = new ArrayList<>();
        this.menu.getEditorWidgets(widgets);

        this.windowWidth = WIDTH;
        this.windowHeight = widgets.size() * PropertyList.ITEM_HEIGHT + 20 + 10; // 20 is the header
        this.windowLeft = 10;
        this.windowTop = (this.height - this.windowHeight) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            Minecraft.getInstance().setScreen(this.parent);
        }).pos(this.windowLeft + WIDTH - 12 - 4, this.windowTop + 4).size(12, 12).build());

        this.list = new PropertyList();
        this.list.setX(this.windowLeft + 10);
        this.addWidget(this.list);

        widgets.forEach(pair -> {
            if(pair.getRight().get() instanceof AbstractWidget widget) {
                this.list.addEntry(new PropertyEntry(pair.getLeft(), widget));
            }
        });
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        this.drawHeader(pGuiGraphics, this.windowLeft, this.windowTop, this.windowWidth);
        this.drawBody(pGuiGraphics, this.windowLeft + 4, this.windowTop + 20, this.windowWidth - 8, this.windowHeight - 20); // Remove header height
        this.list.render(pGuiGraphics, mouseX, mouseY, partialTicks);
        super.render(pGuiGraphics, mouseX, mouseY, partialTicks);
        pGuiGraphics.drawString(this.font, this.getTitle(), this.windowLeft + 5, this.windowTop + 6, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private void drawHeader(GuiGraphics graphics, int x, int y, int width)
    {
        int height = 20;
        this.drawTexturedRect(graphics, x, y, 0, 0, 2, 2, 2, 2);                           /* Top left corner */
        this.drawTexturedRect(graphics, x + width - 2, y, 3, 0, 2, 2, 2, 2);               /* Top right corner */
        this.drawTexturedRect(graphics, x, y + height - 2, 0, 18, 2, 2, 2, 2);             /* Bottom left corner */
        this.drawTexturedRect(graphics, x + width - 2, y + height - 2, 3, 18, 2, 2, 2, 2); /* Bottom right corner */
        this.drawTexturedRect(graphics, x + 2, y, 2, 0, width - 4, 2, 1, 2);               /* Top border */
        this.drawTexturedRect(graphics, x + 2, y + height - 2, 2, 18, width - 4, 2, 1, 2); /* Bottom border */
        this.drawTexturedRect(graphics, x, y + 2, 0, 2, 2, height - 4, 2, 16);             /* Left border */
        this.drawTexturedRect(graphics, x + width - 2, y + 2, 3, 2, 2, height - 4, 2, 16); /* Right border */
        this.drawTexturedRect(graphics, x + 2, y + 2, 2, 2, width - 4, height - 4, 1, 16); /* Center */
    }

    private void drawBody(GuiGraphics graphics, int x, int y, int width, int height)
    {
        this.drawTexturedRect(graphics, x, y + height - 2, 5, 3, 2, 2, 2, 2);             /* Bottom left corner */
        this.drawTexturedRect(graphics, x + width - 2, y + height - 2, 8, 3, 2, 2, 2, 2); /* Bottom right corner */
        this.drawTexturedRect(graphics, x + 2, y + height - 2, 7, 3, width - 4, 2, 1, 2); /* Bottom border */
        this.drawTexturedRect(graphics, x, y, 5, 2, 2, height - 2, 2, 1);                 /* Left border */
        this.drawTexturedRect(graphics, x + width - 2, y, 8, 2, 2, height - 2, 2, 1);     /* Right border */
        this.drawTexturedRect(graphics, x + 2, y, 7, 2, width - 4, height - 2, 1, 1);     /* Center */
    }

    // Draws the texture region (u, v, textureWidth x textureHeight) of the 256x256 window texture stretched over width x height
    private void drawTexturedRect(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight)
    {
        graphics.blit(WINDOW_TEXTURE, x, y, width, height, u, v, textureWidth, textureHeight, 256, 256);
    }

    private class PropertyList extends ContainerObjectSelectionList<PropertyEntry>
    {
        public static final int ITEM_HEIGHT = 34;

        public PropertyList()
        {
            super(EditorScreen.this.minecraft, EditorScreen.this.windowWidth - 20, EditorScreen.this.windowHeight - 25, EditorScreen.this.windowTop + 20, 34);
        }

        @Override
        public int addEntry(PropertyEntry entry)
        {
            return super.addEntry(entry);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {} // Port note: updateNarration is final in 1.21

        @Override
        protected int getScrollbarPosition()
        {
            return this.getX() + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return super.getRowLeft() - 2;
        }

        @Override
        public int getRowWidth()
        {
            return EditorScreen.this.windowWidth - 20 - 2 - (this.getMaxScroll() > 0 ? 6 : 0);
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {}

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {}
    }

    private class PropertyEntry extends ContainerObjectSelectionList.Entry<PropertyEntry>
    {
        private final Component label;
        private final AbstractWidget widget;

        public PropertyEntry(Component label, AbstractWidget widget)
        {
            this.label = label;
            this.widget = widget;
        }

        @Override
        public void render(GuiGraphics pGuiGraphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            pGuiGraphics.drawString(EditorScreen.this.getMinecraft().font, this.label, left + 5, top, 0xFFFFFF);
            this.widget.setX(left);
            this.widget.setY(top + 10);
            this.widget.setWidth(rowWidth);
            this.widget.render(pGuiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of();
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(this.widget);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return ScreenUtil.isMouseWithin(EditorScreen.this.list.getRowLeft(), EditorScreen.this.list.getY(), EditorScreen.this.list.getRowWidth(), EditorScreen.this.list.getHeight(), (int) mouseX, (int) mouseY) && super.isMouseOver(mouseX, mouseY);
        }
    }
}
