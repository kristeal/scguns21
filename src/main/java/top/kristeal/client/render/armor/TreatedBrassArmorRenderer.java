package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.TreatedBrassArmorItem;

public class TreatedBrassArmorRenderer extends GeoArmorRenderer<TreatedBrassArmorItem> {
    public TreatedBrassArmorRenderer() {
        super(new TreatedBrassArmorModel());
    }
}
