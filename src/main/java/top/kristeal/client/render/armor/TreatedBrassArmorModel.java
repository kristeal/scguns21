package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.TreatedBrassArmorItem;

public class TreatedBrassArmorModel extends GeoModel<TreatedBrassArmorItem> {
    @Override
    public ResourceLocation getModelResource(TreatedBrassArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/treated_brass_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TreatedBrassArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/treated_brass_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TreatedBrassArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/treated_brass_armor.animation.json");
    }
}
