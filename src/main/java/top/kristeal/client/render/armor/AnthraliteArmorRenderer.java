package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.AnthraliteArmorItem;

public class AnthraliteArmorRenderer extends GeoArmorRenderer<AnthraliteArmorItem> {
    public AnthraliteArmorRenderer() {
        super(new AnthraliteArmorModel());
    }
}