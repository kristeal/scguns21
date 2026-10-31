package top.kristeal.common;

import top.kristeal.annotation.Ignored;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Author: MrCrayfish
 */
public class CustomGun implements INBTSerializable<CompoundTag>
{
    @Ignored
    public ItemStack model;
    public Gun gun;

    public ItemStack getModel()
    {
        return this.model;
    }

    public Gun getGun()
    {
        return this.gun;
    }

    // Port note: 1.21 INBTSerializable passes registry access, which saving the model ItemStack needs
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag compound = new CompoundTag();
        compound.put("Model", this.model.saveOptional(provider));
        compound.put("Gun", this.gun.serializeNBT());
        return compound;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compound)
    {
        this.model = ItemStack.parseOptional(provider, compound.getCompound("Model"));
        this.gun = Gun.create(compound.getCompound("Gun"));
    }
}
