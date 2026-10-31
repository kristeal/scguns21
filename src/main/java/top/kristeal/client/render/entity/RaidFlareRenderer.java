package top.kristeal.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.kristeal.Reference;
import top.kristeal.entity.projectile.RaidFlareEntity;

/**
 * Draws a flare in flight as the item of the raid it starts.
 */
public class RaidFlareRenderer extends EntityRenderer<RaidFlareEntity>
{
    private final ItemRenderer itemRenderer;

    public RaidFlareRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RaidFlareEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (entity.tickCount < 2)
        {
            return;
        }

        ItemStack flareStack = getFlareItemStack(entity.getRaidId());
        if (!flareStack.isEmpty())
        {
            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
            this.itemRenderer.renderStatic(flareStack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static ItemStack getFlareItemStack(String raidId)
    {
        if (raidId.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        String itemId = "piglin".equals(raidId) ? "gold_flare" : raidId + "_flare";
        return BuiltInRegistries.ITEM.getOptional(Reference.id(itemId)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public ResourceLocation getTextureLocation(RaidFlareEntity entity)
    {
        return Reference.id("textures/entity/raid_flare.png");
    }
}
