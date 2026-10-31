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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.kristeal.client.handler.ReloadHandler;
import top.kristeal.common.Gun;
import top.kristeal.common.ReloadType;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class C2SMessageReload implements CustomPacketPayload
{
    public static final Type<C2SMessageReload> TYPE = new Type<>(Reference.id("c2s_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageReload> STREAM_CODEC = StreamCodec.ofMember(C2SMessageReload::encode, C2SMessageReload::decode);

    private boolean reload;

    public C2SMessageReload() {}

    public C2SMessageReload(boolean reload) {
        this.reload = reload;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.reload);
    }
    public static C2SMessageReload decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageReload(buffer.readBoolean());
    }
    public static void handle(C2SMessageReload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && !player.isSpectator()) {
                ItemStack heldItem = player.getMainHandItem();
                if (!(heldItem.getItem() instanceof GunItem) ||
                        !heldItem.getItem().getClass().getPackageName().startsWith("top.kristeal")) {
                    return;
                }

                CompoundTag tag = GunStackData.liveState(heldItem);
                Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);
                boolean currentlyReloading = ModSyncedDataKeys.RELOADING.getValue(player);
                boolean currentlyAiming = ModSyncedDataKeys.AIMING.getValue(player);
                boolean inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");

                if (message.reload) {
                    if (!currentlyReloading && !inCriticalPhase) {
                        if(currentlyAiming) {
                            ModSyncedDataKeys.AIMING.setValue(player, false);
                        }

                        ModSyncedDataKeys.RELOADING.setValue(player, true);
                        tag.putBoolean("IsReloading", true);
                        tag.putBoolean("scguns:IsReloading", true);
                        if (gun.getReloads().getReloadType() != ReloadType.MANUAL) {
                            tag.putBoolean("InCriticalReloadPhase", true);
                        }
                        tag.remove("scguns:ReloadState");
                    }
                } else {
                    if (gun.getReloads().getReloadType() != ReloadType.MANUAL && inCriticalPhase) {
                        return;
                    }
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    tag.remove("IsReloading");
                    tag.remove("scguns:IsReloading");
                    tag.remove("InCriticalReloadPhase");
                    tag.remove("scguns:ReloadState");
                    if (gun.getReloads().getReloadType() == ReloadType.MANUAL &&
                            tag.getBoolean("scguns:IsReloading") &&
                            !tag.getBoolean("scguns:IsPlayingReloadStop")) {
                        tag.putBoolean("scguns:IsPlayingReloadStop", true);
                        tag.putString("scguns:ReloadState", "STOPPING");
                        ReloadHandler.loaded(player);
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
