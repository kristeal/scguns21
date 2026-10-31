package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.NetheriteGasMaskArmorItem;

public class NetheriteGasMaskArmorRenderer extends GeoArmorRenderer<NetheriteGasMaskArmorItem> {
    public NetheriteGasMaskArmorRenderer() {
        super(new NetheriteGasMaskArmorModel());
    }
}
