package top.kristeal.entity.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import top.kristeal.Reference;
import top.kristeal.entity.monster.TheMerchantEntity;

public class TheMerchantRenderer extends MobRenderer<TheMerchantEntity, TheMerchantModel<TheMerchantEntity>> {
    public TheMerchantRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new TheMerchantModel<>(pContext.bakeLayer(ModModelLayers.THE_MERCHANT_LAYER)), 0.7f);
    }
    @Override
    public ResourceLocation getTextureLocation(TheMerchantEntity pEntity) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/the_merchant.png");
    }
}


