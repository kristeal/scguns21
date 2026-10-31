package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.kristeal.client.handler.BeamHandler;
import top.kristeal.common.network.ServerPlayHandler;

public class C2SMessageStopBeam implements CustomPacketPayload
{
    public static final Type<C2SMessageStopBeam> TYPE = new Type<>(Reference.id("c2s_stop_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageStopBeam> STREAM_CODEC = StreamCodec.ofMember(C2SMessageStopBeam::encode, C2SMessageStopBeam::decode);

    public C2SMessageStopBeam() {}

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        // No data needs to be encoded for this message
    }
    public static C2SMessageStopBeam decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageStopBeam();
    }
    public static void handle(C2SMessageStopBeam message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                ServerPlayHandler.handleStopBeam(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
