package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.IronMaskArmorItem;

public class IronMaskArmorRenderer extends GeoArmorRenderer<IronMaskArmorItem> {
    public IronMaskArmorRenderer() {
        super(new IronMaskArmorModel());
    }
}
