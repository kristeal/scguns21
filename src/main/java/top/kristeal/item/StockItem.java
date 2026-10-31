package top.kristeal.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.kristeal.item.attachment.IStock;
import top.kristeal.item.attachment.impl.Stock;

/**
 * A basic stock attachment item implementation with color support
 *
 * Author: MrCrayfish
 */
public class StockItem extends AttachmentItem implements IStock, IColored
{
    private final Stock stock;
    private final boolean colored;

    public StockItem(Stock stock, Properties properties)
    {
        super(properties);
        this.stock = stock;
        this.colored = true;
    }

    public StockItem(Stock stock, Properties properties, boolean colored)
    {
        super(properties);
        this.stock = stock;
        this.colored = colored;
    }

    @Override
    public Stock getProperties()
    {
        return this.stock;
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
