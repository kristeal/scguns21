package top.kristeal.item;

import top.kristeal.item.attachment.IScope;
import top.kristeal.item.attachment.impl.Scope;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

/**
 * A basic scope attachment item implementation with color support
 *
 * Author: MrCrayfish
 */
public class ScopeItem extends AttachmentItem implements IScope, IColored
{
    private final Scope scope;
    private final boolean colored;

    public ScopeItem(Scope scope, Item.Properties properties)
    {
        super(properties);
        this.scope = scope;
        this.colored = true;
    }

    public ScopeItem(Scope scope, Item.Properties properties, boolean colored)
    {
        super(properties);
        this.scope = scope;
        this.colored = colored;
    }

    @Override
    public Scope getProperties()
    {
        return this.scope;
    }

    @Override
    public boolean canColor(ItemStack stack)
    {
        return this.colored;
    }

    // Port note: 1.21 replaces canApplyAtEnchantingTable with supportsEnchantment (enchantments are holders)
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment)
    {
        return enchantment.is(Enchantments.BINDING_CURSE) || super.supportsEnchantment(stack, enchantment);
    }
}
