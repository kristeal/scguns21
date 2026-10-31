package top.kristeal.client.render.gun.animated;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import top.kristeal.item.animated.AnimatedGunItem;


public class AttachmentRenderer extends GeoRenderLayer<AnimatedGunItem> {
    protected ItemStack currentItemStack;
    private static ResourceLocation model_resource = null;
    private static ResourceLocation texture_resource = null;
    private static ItemStack itemStack = null;

    public AttachmentRenderer(GeoRenderer entityRendererIn) {
        super(entityRendererIn);
    }

    public void updateAttachment(ItemStack attachmentStack) {
        itemStack = attachmentStack;
        // Port note: 1.21 Item#toString is the full id ("scguns:reflex_sight"), so the registry path replaces it
        String attachmentName = BuiltInRegistries.ITEM.getKey(attachmentStack.getItem()).getPath();
        model_resource = ResourceLocation.fromNamespaceAndPath("scguns", "geo/item/attachment/" + attachmentName + ".geo.json");
        texture_resource = ResourceLocation.fromNamespaceAndPath("scguns", "textures/animated/attachment/" + attachmentName + ".png");

    }

    public void renderForBone(PoseStack poseStack, AnimatedGunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (model_resource != null && texture_resource != null && itemStack != null) {
          AnimatedGunModel focusModel = new AnimatedGunModel(model_resource);
            ResourceLocation focusModelModelResource = model_resource;
            RenderType focusModelRenderLayer = RenderType.entityTranslucent(texture_resource);
            if (bone.getName().matches("attachment_bone") && !bone.getName().matches(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath()) && itemStack != null) {
                this.getRenderer().reRender(focusModel.getBakedModel(focusModelModelResource), poseStack, bufferSource, animatable, focusModelRenderLayer, bufferSource.getBuffer(focusModelRenderLayer), partialTick, packedLight, OverlayTexture.NO_OVERLAY, -1); // Port note: GeckoLib 4.9 packed colour (-1 = opaque white)
            }
        }

    }
}
