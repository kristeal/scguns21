package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.AdrienArmorItem;

public class AdrienArmorRenderer extends GeoArmorRenderer<AdrienArmorItem> {
    public AdrienArmorRenderer() {
        super(new AdrienArmorModel());
    }
}