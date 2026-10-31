package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.CogKnightArmorItem;

public class CogKnightArmorRenderer extends GeoArmorRenderer<CogKnightArmorItem> {
    public CogKnightArmorRenderer() {
        super(new CogKnightArmorModel());
    }
}
