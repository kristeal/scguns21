package top.kristeal.client.render.crosshair;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import top.kristeal.Reference;
import top.kristeal.client.handler.CrosshairHandler;
import top.kristeal.client.handler.GunRenderingHandler;
import top.kristeal.client.handler.HUDRenderHandler;

/**
 * Author: MrCrayfish
 */
public class SpecialHitMarker extends Crosshair {
    private static final ResourceLocation NORMAL_HITMARKER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/crosshair/special_hit_marker.png");
    private static final ResourceLocation ALT_HITMARKER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/crosshair/special_hit_marker2.png");

    public SpecialHitMarker() {
        super(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "special_hit_marker"));
    }

    @Override
    public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks) {
        stack.pushPose();

        float alpha = 1.0F; // - (float) AimingHandler.get().getNormalisedAdsProgress();
        float size = 9.0F;
        float hitMarkerProgress = HUDRenderHandler.getHitMarkerProgress(partialTicks);
        boolean crit = HUDRenderHandler.getHitMarkerCrit();
        float scale = 1.5F + (hitMarkerProgress * 1.0F);
        stack.translate(Math.round((windowWidth) / 2F) - 0.5, Math.round((windowHeight) / 2F) - 0.5, 0);
        stack.scale(scale, scale, scale);
        stack.translate(-size / 2F, -size / 2F, 0);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, (!crit ? 1.0F : 0.25F), (!crit ? 1.0F : 0.25F), 1.0F - (hitMarkerProgress * 0.3F));
        boolean useAltHitMarker = CrosshairHandler.get().getCurrentCrosshair().getLocation().equals(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "hit_marker"));
        ResourceLocation texture = (useAltHitMarker ? ALT_HITMARKER : NORMAL_HITMARKER);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();

        Matrix4f matrix = stack.last().pose();
        BufferBuilder buffer; // Port note: 1.21 Tesselator#begin returns a fresh BufferBuilder
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, 0, size, 0).setUv(0, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, size, 0, 0).setUv(1, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, 0, 0, 0).setUv(0, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        // Fix: the reference never reset the global shader colour, so the HUD drawn after a crit was tinted red and faded
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        stack.popPose();
    }
}

