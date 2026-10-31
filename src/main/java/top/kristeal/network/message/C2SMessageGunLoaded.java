package top.kristeal.network.message;

import top.kristeal.common.GunStackData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.kristeal.common.Gun;
import top.kristeal.common.ReloadTracker;
import top.kristeal.common.ReloadType;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.network.PacketHandler;

public class C2SMessageGunLoaded implements CustomPacketPayload
{
    public static final Type<C2SMessageGunLoaded> TYPE = new Type<>(Reference.id("c2s_gun_loaded"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageGunLoaded> STREAM_CODEC = StreamCodec.ofMember(C2SMessageGunLoaded::encode, C2SMessageGunLoaded::decode);

    public C2SMessageGunLoaded() {}

    public void encode(RegistryFriendlyByteBuf buffer)
    {}

    public static C2SMessageGunLoaded decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageGunLoaded();
    }
    public static void handle(C2SMessageGunLoaded message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && !player.isSpectator()) {
                ItemStack heldItem = player.getMainHandItem();
                if (heldItem.getItem() instanceof GunItem) {
                    if (!heldItem.getItem().getClass().getPackageName().startsWith("top.kristeal")) {
                        return;
                    }
                    Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);
                    CompoundTag tag = GunStackData.liveState(heldItem);

                    ReloadTracker tracker = new ReloadTracker(player);

                    if (gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                        tracker.increaseMagAmmo(player);
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        tag.remove("IsReloading");
                        tag.remove("scguns:IsReloading");
                        tag.remove("InCriticalReloadPhase");

                        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageUpdateAmmo(GunStackData.getAmmoCount(heldItem))); // Fix: only the loading player

                        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageReload(false)); // Fix: only the loading player

                    } else if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                        tracker.reloadItem(player);
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        tag.remove("IsReloading");
                        tag.remove("scguns:IsReloading");
                        tag.remove("InCriticalReloadPhase");

                        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageUpdateAmmo(GunStackData.getAmmoCount(heldItem))); // Fix: only the loading player
                        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageReload(false)); // Fix: only the loading player

                    } else if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                        tracker.increaseAmmo(player);

                        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageUpdateAmmo(GunStackData.getAmmoCount(heldItem))); // Fix: only the loading player

                        boolean weaponFull = tracker.isWeaponFull(player);
                        boolean hasNoAmmo = tracker.hasNoAmmo(player);

                        if (weaponFull || hasNoAmmo) {
                            if (player.getMainHandItem().getItem() instanceof AnimatedGunItem) {
                                // Mark that we should stop after the current animation loop finishes
                                tag.putBoolean("scguns:ShouldStopAfterLoop", true);
                                // DON'T immediately set RELOADING to false - let the animation complete
                                // The client-side animation will handle this when the loop finishes
                            } else {
                                // For non-animated guns, stop immediately as before
                                ModSyncedDataKeys.RELOADING.setValue(player, false);
                                tag.remove("IsReloading");
                                tag.remove("scguns:IsReloading");

                                PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageReload(false)); // Fix: only the loading player
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
