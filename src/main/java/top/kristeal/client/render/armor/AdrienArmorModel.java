package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.AdrienArmorItem;

public class AdrienArmorModel extends GeoModel<AdrienArmorItem> {
    @Override
    public ResourceLocation getModelResource(AdrienArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/adrien_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AdrienArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/adrien_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AdrienArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/adrien_armor.animation.json");
    }
}