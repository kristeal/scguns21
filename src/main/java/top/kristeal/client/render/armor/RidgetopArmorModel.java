package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.RidgetopArmorItem;

public class RidgetopArmorModel extends GeoModel<RidgetopArmorItem> {
    @Override
    public ResourceLocation getModelResource(RidgetopArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/ridgetop.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RidgetopArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/ridgetop.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RidgetopArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/ridgetop.animation.json");
    }
}
