package top.kristeal.item;

import net.minecraft.world.item.Item;

public class ScorchedItem extends Item {
    public ScorchedItem(Properties properties) {
        super(properties.fireResistant());
    }
}
