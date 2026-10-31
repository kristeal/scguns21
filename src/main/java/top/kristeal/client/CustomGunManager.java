package top.kristeal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import top.kristeal.Reference;
import top.kristeal.common.CustomGun;
import top.kristeal.common.GunStackData;
import top.kristeal.init.ModItems;
import top.kristeal.network.message.S2CMessageUpdateGuns;

import java.util.Map;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: custom guns now arrive with S2CMessageUpdateGuns; the reference's Framework
 * LoginData is gone.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class CustomGunManager
{
    private static Map<ResourceLocation, CustomGun> customGunMap;

    public static boolean updateCustomGuns(S2CMessageUpdateGuns message)
    {
        return updateCustomGuns(message.getCustomGuns());
    }

    private static boolean updateCustomGuns(Map<ResourceLocation, CustomGun> customGunMap)
    {
        CustomGunManager.customGunMap = customGunMap;
        return true;
    }

    public static void fill(CreativeModeTab.Output output)
    {
        // Custom guns are built on the M3 Carabine: a name component, GUN_OVERRIDE {Gun, Custom, Model} and AMMO_COUNT.
        if(customGunMap != null)
        {
            customGunMap.forEach((id, gun) ->
            {
                ItemStack stack = new ItemStack(ModItems.M3_CARABINE.get());
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item." + id.getNamespace() + "." + id.getPath() + ".name"));
                HolderLookup.Provider provider = Minecraft.getInstance().level.registryAccess();
                GunStackData.updateGunOverride(stack, tag -> {
                    tag.put("Model", gun.getModel().saveOptional(provider));
                    tag.put("Gun", gun.getGun().serializeNBT());
                    tag.putBoolean("Custom", true);
                });
                GunStackData.setAmmoCount(stack, gun.getGun().getReloads().getMaxAmmo());
                output.accept(stack);
            });
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event)
    {
        customGunMap = null;
    }
}
