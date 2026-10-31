package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import top.kristeal.client.network.ClientPlayHandler;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;

public class S2CMessageReload implements CustomPacketPayload
{
    public static final Type<S2CMessageReload> TYPE = new Type<>(Reference.id("s2c_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageReload> STREAM_CODEC = StreamCodec.ofMember(S2CMessageReload::encode, S2CMessageReload::decode);

    private boolean reloading;

    public S2CMessageReload() {}

    public S2CMessageReload(boolean reloading) {
        this.reloading = reloading;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.reloading);
    }
    public static S2CMessageReload decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageReload(buffer.readBoolean());
    }
    public static void handle(S2CMessageReload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPlayHandler.handleReloadState(message.reloading);
        });
    }

    public boolean isReloading() {
        return this.reloading;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
