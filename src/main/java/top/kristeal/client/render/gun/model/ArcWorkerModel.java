package top.kristeal.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Item;
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
public class ArcWorkerModel implements IOverrideModel {


    @SuppressWarnings("resource")
    @Override
    public void render(float partialTicks, ItemDisplayContext transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        if ((Gun.getScope(stack) == null))
            RenderUtil.renderModel(SpecialModels.ARC_WORKER_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
        else
            RenderUtil.renderModel(SpecialModels.ARC_WORKER_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
        RenderUtil.renderModel(SpecialModels.ARC_WORKER_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
        if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
            if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get())
                RenderUtil.renderModel(SpecialModels.ARC_WORKER_WOODEN_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
            else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get())
                RenderUtil.renderModel(SpecialModels.ARC_WORKER_LIGHT_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
            else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get())
                RenderUtil.renderModel(SpecialModels.ARC_WORKER_HEAVY_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);
            else if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get())
                RenderUtil.renderModel(SpecialModels.ARC_WORKER_HEAVY_STOCK.getModel(), stack, matrixStack, buffer, light, overlay);


        }
      
    }
}