package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.ScrapArmorItem;


public class ScrapArmorRenderer extends GeoArmorRenderer<ScrapArmorItem> {
    public ScrapArmorRenderer() {
        super(new ScrapArmorModel());
    }
}