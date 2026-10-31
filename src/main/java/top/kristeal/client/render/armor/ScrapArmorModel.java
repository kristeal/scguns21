package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.ScrapArmorItem;

public class ScrapArmorModel extends GeoModel<ScrapArmorItem> {
    @Override
    public ResourceLocation getModelResource(ScrapArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/scrap_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ScrapArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/scrap_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ScrapArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/scrap_armor.animation.json");
    }
}