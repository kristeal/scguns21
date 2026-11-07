package top.kristeal.item;

import net.minecraft.world.item.Item;
import top.kristeal.item.attachment.impl.Scope;

//Fake scope item that gives the same buffs as reflex sight without it being an actul reflex sight
public class HiddenScopeItem extends ScopeItem
{
    public HiddenScopeItem(Scope scope, Item.Properties properties)
    {
        super(scope, properties, false);
    }

    @Override
    public boolean isVisibleOnGun()
    {
        return false;
    }
}
