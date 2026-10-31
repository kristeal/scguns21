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
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;

public class C2SMessageAim implements CustomPacketPayload
{
    public static final Type<C2SMessageAim> TYPE = new Type<>(Reference.id("c2s_aim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageAim> STREAM_CODEC = StreamCodec.ofMember(C2SMessageAim::encode, C2SMessageAim::decode);

    private boolean aiming;

    public C2SMessageAim() {}

    public C2SMessageAim(boolean aiming)
    {
        this.aiming = aiming;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.aiming);
    }
    public static C2SMessageAim decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessageAim(buffer.readBoolean());
    }
    public static void handle(C2SMessageAim message, IPayloadContext context) {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null && !player.isSpectator())
            {
                boolean currentlyReloading = ModSyncedDataKeys.RELOADING.getValue(player);

                if(currentlyReloading) {
                    ModSyncedDataKeys.AIMING.setValue(player, false);
                    return;
                }
                ItemStack heldItem = player.getMainHandItem();
                if(heldItem.getItem() instanceof GunItem) {
                    CompoundTag tag = GunStackData.liveState(heldItem);
                    boolean inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");

                    if(inCriticalPhase) {
                        ModSyncedDataKeys.AIMING.setValue(player, false);
                        return;
                    }
                }

                ModSyncedDataKeys.AIMING.setValue(player, message.aiming);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
