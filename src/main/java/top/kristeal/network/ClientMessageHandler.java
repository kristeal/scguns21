package top.kristeal.network;

import top.kristeal.common.GunStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.util.GunModifierHelper;
@OnlyIn(Dist.CLIENT)
public class ClientMessageHandler {
    public static boolean handleUpdateAmmo(int ammoCount) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
                CompoundTag tag = GunStackData.liveState(heldItem);
                GunStackData.setAmmoCount(heldItem, ammoCount); // Port note: AMMO_COUNT component
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(heldItem, ((GunItem)heldItem.getItem()).getModifiedGun(heldItem));
                if (ammoCount >= maxAmmo && tag.getBoolean("scguns:IsReloading")) {
                    tag.putString("scguns:ReloadState", "STOPPING");
                    tag.putBoolean("scguns:IsPlayingReloadStop", true);
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    tag.remove("scguns:IsReloading");
                }
            }
        }
        return true;
    }
}