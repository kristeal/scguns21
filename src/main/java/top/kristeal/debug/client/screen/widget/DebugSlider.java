package top.kristeal.debug.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import org.jetbrains.annotations.NotNull;
import top.kristeal.debug.IDebugWidget;

import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 * <p>
 * Port notes: Forge's {@code ForgeSlider} is NeoForge's {@link ExtendedSlider}. {@code widgets.png} is gone since
 * 1.20.2, so the handle (the only part the reference drew, as two 4-pixel halves) is the vanilla slider handle sprite.
 */
public class DebugSlider extends ExtendedSlider implements IDebugWidget {
    private static final ResourceLocation HANDLE_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
    private static final ResourceLocation HANDLE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");

    private final Consumer<Double> callback;

    public DebugSlider(double minValue, double maxValue, double currentValue, double stepSize, int precision, Consumer<Double> callback) {
        super(0, 0, 0, 14, Component.empty(), Component.empty(), minValue, maxValue, currentValue, stepSize, precision, true);
        this.callback = callback;
    }

    @Override
    protected void applyValue() {
        this.callback.accept(this.getValue());
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation handle = this.isHoveredOrFocused() ? HANDLE_HIGHLIGHTED_SPRITE : HANDLE_SPRITE;
        graphics.blitSprite(handle, this.getX() + (int) (this.value * (double) (this.width - 8)), this.getY(), 8, this.height);
    }
}
