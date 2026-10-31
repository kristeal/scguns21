package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.AnthraliteGasMaskArmorItem;

public class AnthraliteGasMaskArmorModel extends GeoModel<AnthraliteGasMaskArmorItem> {
    @Override
    public ResourceLocation getModelResource(AnthraliteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/anthralite_respirator.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnthraliteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/anthralite_respirator.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnthraliteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/anthralite_gas_mask.animation.json");
    }
}
