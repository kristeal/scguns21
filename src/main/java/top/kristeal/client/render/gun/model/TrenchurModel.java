package top.kristeal.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.client.SpecialModels;
import top.kristeal.client.render.gun.IOverrideModel;
import top.kristeal.client.util.RenderUtil;
import top.kristeal.common.Gun;
import top.kristeal.event.GunFireEvent;
import top.kristeal.init.ModItems;
import top.kristeal.item.attachment.IAttachment;

/**
 * Since we want to have an animation for the charging handle, we will be overriding the standard model rendering.
 * This also allows us to replace the model for the different stocks.
 */
public class TrenchurModel implements IOverrideModel {

    private static final int TOTAL_SHOTS = 7;

    @SuppressWarnings("resource")
    @Override
    public void render(float partialTicks, ItemDisplayContext transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        RenderUtil.renderModel(SpecialModels.TRENCHUR_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
        RenderUtil.renderModel(SpecialModels.TRENCHUR_DRUM.getModel(), stack, matrixStack, buffer, light, overlay);
        renderBarrelAttachments(matrixStack, buffer, stack, light, overlay);

        if (entity.equals(Minecraft.getInstance().player)) {
            matrixStack.pushPose();
            matrixStack.translate(0, -0.30, 0.33);
            ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
            float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
            cooldown = (float) ease(cooldown);
            float rotationAngle = -cooldown * 38;
            matrixStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
            matrixStack.translate(0, 0.30, -0.33);
            RenderUtil.renderModel(SpecialModels.TRENCHUR_HAMMER.getModel(), stack, matrixStack, buffer, light, overlay);

            matrixStack.popPose();
        }
    }

    private void renderBarrelAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
        boolean hasExtendedBarrel = false;

        if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
            if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
                RenderUtil.renderModel(SpecialModels.TRENCHUR_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
                hasExtendedBarrel = true;
            } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
                RenderUtil.renderModel(SpecialModels.TRENCHUR_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
            } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
                RenderUtil.renderModel(SpecialModels.TRENCHUR_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
            } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
                RenderUtil.renderModel(SpecialModels.TRENCHUR_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
            }
        }
        if (!hasExtendedBarrel) {
            RenderUtil.renderModel(SpecialModels.TRENCHUR_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
        }

    }


    private double ease(double x) {
        return 1 - Math.pow(1 - x, 4);
    }

}
