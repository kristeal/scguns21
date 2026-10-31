package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.AnthraliteGasMaskArmorItem;

public class AnthraliteGasMaskArmorRenderer extends GeoArmorRenderer<AnthraliteGasMaskArmorItem> {
    public AnthraliteGasMaskArmorRenderer() {
        super(new AnthraliteGasMaskArmorModel());
    }
}
