package top.kristeal.client.render.gun.animated;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import top.kristeal.item.animated.AnimatedGunItem;


public class AnimatedGunModel extends DefaultedItemGeoModel<AnimatedGunItem> {
    private final String modelPath;

    public AnimatedGunModel(ResourceLocation path) {
        super(path);
        this.modelPath = path.getPath();
    }

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem gunItem) {
        return ResourceLocation.fromNamespaceAndPath("scguns", "geo/item/gun/" + modelPath + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem gunItem) {
        return ResourceLocation.fromNamespaceAndPath("scguns", "textures/animated/gun/" + modelPath + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem gunItem) {
        return ResourceLocation.fromNamespaceAndPath("scguns", "animations/item/" + modelPath + ".animation.json");
    }
}