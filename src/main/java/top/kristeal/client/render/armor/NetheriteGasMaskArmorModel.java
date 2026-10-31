package top.kristeal.client.render.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import top.kristeal.Reference;
import top.kristeal.item.animated.NetheriteGasMaskArmorItem;

public class NetheriteGasMaskArmorModel extends GeoModel<NetheriteGasMaskArmorItem> {
    @Override
    public ResourceLocation getModelResource(NetheriteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/netherite_respirator.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NetheriteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/netherite_respirator.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NetheriteGasMaskArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/netherite_gas_mask.animation.json");
    }
}
