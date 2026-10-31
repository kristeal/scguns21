package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.BrassMaskArmorItem;

public class BrassMaskArmorRenderer extends GeoArmorRenderer<BrassMaskArmorItem> {
    public BrassMaskArmorRenderer() {
        super(new BrassMaskArmorModel());
    }
}
