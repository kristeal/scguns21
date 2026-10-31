package top.kristeal.client.render.crosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import top.kristeal.client.handler.AimingHandler;

/**
 * Author: MrCrayfish
 */
public class TexturedCrosshair extends Crosshair
{
    private final ResourceLocation texture;
    private final boolean blend;

    public TexturedCrosshair(ResourceLocation id)
    {
        this(id, true);
    }

    public TexturedCrosshair(ResourceLocation id, boolean blend)
    {
        super(id);
        this.texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/crosshair/" + id.getPath() + ".png");
        this.blend = blend;
    }

    @Override
    public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks)
    {
        stack.pushPose();

        float alpha = 1.0F - (float) AimingHandler.get().getNormalisedAdsProgress();
        float size = 8.0F;
        stack.translate((windowWidth - size) / 2F, (windowHeight - size) / 2F, 0);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, this.texture);
        RenderSystem.enableBlend();

        if(this.blend)
        {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        Matrix4f matrix = stack.last().pose();
        BufferBuilder buffer; // Port note: 1.21 Tesselator#begin returns a fresh BufferBuilder
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, 0, size, 0).setUv(0, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, size, 0, 0).setUv(1, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, 0, 0, 0).setUv(0, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        if(this.blend)
        {
            RenderSystem.defaultBlendFunc();
        }

        stack.popPose();
    }
}