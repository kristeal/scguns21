package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.DiamondSteelArmorItem;

public class DiamondSteelArmorModel extends GeoModel<DiamondSteelArmorItem> {
    @Override
    public ResourceLocation getModelResource(DiamondSteelArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/diamond_steel_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DiamondSteelArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/diamond_steel_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DiamondSteelArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/diamond_steel_armor.animation.json");
    }
}
