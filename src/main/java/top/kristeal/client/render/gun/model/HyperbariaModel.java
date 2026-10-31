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


public class HyperbariaModel implements IOverrideModel {

    @SuppressWarnings("resource")
    @Override
    public void render(float partialTicks, ItemDisplayContext transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {

        RenderUtil.renderModel(SpecialModels.HYPERBARIA_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);

        if (entity.equals(Minecraft.getInstance().player)) {
            renderAnimatedParts(stack, matrixStack, buffer, light, overlay);
        }
    }

    private void renderAnimatedParts(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {

        matrixStack.pushPose();
        matrixStack.translate(0, -5.8 * 0.0625, 0);
        ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
        float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        cooldown = (float) ease(cooldown);
        matrixStack.translate(0, 0, cooldown / 8);
        matrixStack.translate(0, 5.8 * 0.0625, 0);
        RenderUtil.renderModel(SpecialModels.HYPERBARIA_BOLT.getModel(), stack, matrixStack, buffer, light, overlay);
        matrixStack.popPose();
    }

    private double ease(double x) {
        return 1 - Math.pow(1 - (2 * x), 4);
    }
}