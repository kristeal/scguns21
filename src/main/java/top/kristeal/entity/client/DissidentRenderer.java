package top.kristeal.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import top.kristeal.Reference;
import top.kristeal.entity.monster.DissidentEntity;

public class DissidentRenderer extends MobRenderer<DissidentEntity, DissidentModel<DissidentEntity>> {
    public DissidentRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DissidentModel<>(pContext.bakeLayer(ModModelLayers.DISSIDENT_LAYER)), 0.7f);
    }
    @Override
    public ResourceLocation getTextureLocation(DissidentEntity pEntity) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/dissident.png");
    }
    @Override
    public void render(DissidentEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.15f, 1.15f, 1.15f);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}


