package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import top.kristeal.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public class C2SMessagePreFireSound implements CustomPacketPayload
{
    public static final Type<C2SMessagePreFireSound> TYPE = new Type<>(Reference.id("c2s_pre_fire_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessagePreFireSound> STREAM_CODEC = StreamCodec.ofMember(C2SMessagePreFireSound::encode, C2SMessagePreFireSound::decode);


    public C2SMessagePreFireSound() {}

    public C2SMessagePreFireSound(Player player)
    {
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {

    }
    public static C2SMessagePreFireSound decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessagePreFireSound();
    }
    public static void handle(C2SMessagePreFireSound message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null)
            {
                ServerPlayHandler.handlePreFireSound(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
