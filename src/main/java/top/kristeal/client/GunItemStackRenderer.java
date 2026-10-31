package top.kristeal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.kristeal.client.handler.GunRenderingHandler;

/**
 * Author: MrCrayfish
 */
public class GunItemStackRenderer extends BlockEntityWithoutLevelRenderer
{
    public GunItemStackRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext display, PoseStack poseStack, MultiBufferSource source, int light, int overlay)
    {
        poseStack.popPose();

        poseStack.pushPose();
        {
            Minecraft mc = Minecraft.getInstance();
            if(display == ItemDisplayContext.GROUND)
            {
                GunRenderingHandler.get().applyWeaponScale(stack, poseStack);
            }
            GunRenderingHandler.get().renderWeapon(mc.player, stack, display, poseStack, source, light, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)); // Fix: was the frame delta
        }
        poseStack.popPose();

        poseStack.pushPose();
    }
}
