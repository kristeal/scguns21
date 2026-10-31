package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.RedcoatArmorItem;


public class RedcoatArmorRenderer extends GeoArmorRenderer<RedcoatArmorItem> {
    public RedcoatArmorRenderer() {
        super(new RedcoatArmorModel());
    }
}