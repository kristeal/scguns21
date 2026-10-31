package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.kristeal.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class C2SMessageUnload implements CustomPacketPayload
{
    public static final Type<C2SMessageUnload> TYPE = new Type<>(Reference.id("c2s_unload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageUnload> STREAM_CODEC = StreamCodec.ofMember(C2SMessageUnload::encode, C2SMessageUnload::decode);

    public void encode(RegistryFriendlyByteBuf buffer)
    {}
    public static C2SMessageUnload decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessageUnload();
    }
    public static void handle(C2SMessageUnload message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.handleUnload(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
