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
public class C2SMessageLeftOverAmmo implements CustomPacketPayload
{
    public static final Type<C2SMessageLeftOverAmmo> TYPE = new Type<>(Reference.id("c2s_left_over_ammo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageLeftOverAmmo> STREAM_CODEC = StreamCodec.ofMember(C2SMessageLeftOverAmmo::encode, C2SMessageLeftOverAmmo::decode);

    public void encode(RegistryFriendlyByteBuf buffer)
    {}
    public static C2SMessageLeftOverAmmo decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessageLeftOverAmmo();
    }
    public static void handle(C2SMessageLeftOverAmmo message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.handleExtraAmmo(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
