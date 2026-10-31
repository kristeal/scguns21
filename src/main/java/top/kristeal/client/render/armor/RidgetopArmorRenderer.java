package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.RidgetopArmorItem;

public class RidgetopArmorRenderer extends GeoArmorRenderer<RidgetopArmorItem> {
    public RidgetopArmorRenderer() {
        super(new RidgetopArmorModel());
    }
}
