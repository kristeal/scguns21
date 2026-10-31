package top.kristeal.client.screen;

import top.kristeal.init.ModDataComponents;
import top.kristeal.common.GunAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import top.kristeal.common.Gun;
import top.kristeal.common.container.slot.AttachmentSlot;
import top.kristeal.item.attachment.IAttachment;

/**
 * Author: MrCrayfish
 */
public class AttachmentContainer extends AbstractContainerMenu
{
    private final ItemStack weapon;
    private final Container playerInventory;
    private final Container weaponInventory = new SimpleContainer(IAttachment.Type.values().length)
    {
        @Override
        public void setChanged()
        {
            super.setChanged();
            AttachmentContainer.this.slotsChanged(this);
        }
    };
    private boolean loaded = false;

    public AttachmentContainer(int windowId, Inventory playerInventory, ItemStack stack)
    {
        this(windowId, playerInventory);
        ItemStack[] attachments = new ItemStack[IAttachment.Type.values().length];
        for(int i = 0; i < attachments.length; i++)
        {
            attachments[i] = Gun.getAttachment(IAttachment.Type.values()[i], stack);
        }
        for(int i = 0; i < attachments.length; i++)
        {
            this.weaponInventory.setItem(i, attachments[i]);
        }
        this.loaded = true;
    }

    public AttachmentContainer(int windowId, Inventory playerInventory)
    {
        super(ModMenuTypes.ATTACHMENTS.get() /* Port note (Batch 25): ModContainers is folded into ModMenuTypes */, windowId);
        this.weapon = playerInventory.getSelected();
        this.playerInventory = playerInventory;

        int numSlots = Math.min(5, IAttachment.Type.values().length);
        int centerX = 88 - (numSlots * 18) / 2 + 18 - 5 - 6;

        for(int i = 0; i < numSlots; i++)
        {
            this.addSlot(new AttachmentSlot(this, this.weaponInventory, this.weapon, IAttachment.Type.values()[i], playerInventory.player, i, centerX + i * 18, 89 + 19));
        }

        for(int i = 0; i < 3; i++)
        {
            for(int j = 0; j < 9; j++)
            {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 20 + j * 18 - 6, 110 + i * 18 + 19));
            }
        }

        for(int i = 0; i < 9; i++)
        {
            if(i == playerInventory.selected)
            {
                this.addSlot(new Slot(playerInventory, i, 20 + i * 18 - 6, 168 + 19)
                {
                    @Override
                    public boolean mayPickup(Player playerIn)
                    {
                        return false;
                    }
                });
            }
            else
            {
                this.addSlot(new Slot(playerInventory, i, 20 + i * 18 - 6, 168 + 19));
            }
        }
    }

    public boolean isLoaded()
    {
        return this.loaded;
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        return true;
    }

    @Override
    public void slotsChanged(Container inventoryIn)
    {
        // Port note: the attachments are the ATTACHMENTS component; the whole map is rebuilt from the slots,
        // as the reference rebuilt its "Attachments" compound
        GunAttachments attachments = GunAttachments.EMPTY;

        for(int i = 0; i < this.getWeaponInventory().getContainerSize(); i++)
        {
            ItemStack attachment = this.getSlot(i).getItem();
            if(attachment.getItem() instanceof SwordItem)
            {
                attachments = attachments.with("Barrel", attachment.copy());
            }
            if(attachment.getItem() instanceof IAttachment)
            {
                attachments = attachments.with(((IAttachment) attachment.getItem()).getType().getTagKey(), attachment.copy());
            }
        }

        if(attachments.isEmpty()) this.weapon.remove(ModDataComponents.ATTACHMENTS);
        else this.weapon.set(ModDataComponents.ATTACHMENTS, attachments);
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index)
    {
        ItemStack copyStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if(slot != null && slot.hasItem())
        {
            ItemStack slotStack = slot.getItem();
            copyStack = slotStack.copy();
            if(index < this.weaponInventory.getContainerSize())
            {
                if(!this.moveItemStackTo(slotStack, this.weaponInventory.getContainerSize(), this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if(!this.moveItemStackTo(slotStack, 0, this.weaponInventory.getContainerSize(), false))
            {
                return ItemStack.EMPTY;
            }

            if(slotStack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }

        return copyStack;
    }

    public Container getPlayerInventory()
    {
        return this.playerInventory;
    }

    public Container getWeaponInventory()
    {
        return this.weaponInventory;
    }
}