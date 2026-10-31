package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import top.kristeal.client.network.ClientPlayHandler;

public class S2CMessageStopReload implements CustomPacketPayload
{
    public static final Type<S2CMessageStopReload> TYPE = new Type<>(Reference.id("s2c_stop_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageStopReload> STREAM_CODEC = StreamCodec.ofMember(S2CMessageStopReload::encode, S2CMessageStopReload::decode);


    public S2CMessageStopReload() {}

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        // No data to encode
    }
    public static S2CMessageStopReload decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageStopReload();
    }
    public static void handle(S2CMessageStopReload message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleStopReload(message));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
