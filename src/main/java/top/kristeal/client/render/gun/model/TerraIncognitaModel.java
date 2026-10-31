package top.kristeal.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.kristeal.client.SpecialModels;
import top.kristeal.client.render.gun.IOverrideModel;
import top.kristeal.client.util.RenderUtil;
import top.kristeal.common.Gun;
import top.kristeal.init.ModItems;
import top.kristeal.item.attachment.IAttachment;

/**
 * Since we want to have an animation for the charging handle, we will be overriding the standard model rendering.
 * This also allows us to replace the model for the different stocks.
 */
public class TerraIncognitaModel implements IOverrideModel {

    @SuppressWarnings("resource")
    @Override
    public void render(float partialTicks, ItemDisplayContext transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {

        RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
        if ((Gun.getScope(stack) == null))
            RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
        else
            RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);

    }
}
