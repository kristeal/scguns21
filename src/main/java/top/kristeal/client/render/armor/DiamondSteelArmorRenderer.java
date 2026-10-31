package top.kristeal.client.render.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.kristeal.item.animated.DiamondSteelArmorItem;

public class DiamondSteelArmorRenderer extends GeoArmorRenderer<DiamondSteelArmorItem> {
    public DiamondSteelArmorRenderer() {
        super(new DiamondSteelArmorModel());
    }
}
