package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.ExoSuitItem;

public class ExoSuitModel extends GeoModel<ExoSuitItem> {
    @Override
    public ResourceLocation getModelResource(ExoSuitItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/exo_suit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ExoSuitItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/exo_suit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ExoSuitItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/exo_suit.animation.json");
    }
}
