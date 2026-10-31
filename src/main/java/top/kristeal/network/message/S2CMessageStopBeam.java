package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import top.kristeal.client.network.ClientPlayHandler;

import java.util.UUID;

public class S2CMessageStopBeam implements CustomPacketPayload
{
    public static final Type<S2CMessageStopBeam> TYPE = new Type<>(Reference.id("s2c_stop_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageStopBeam> STREAM_CODEC = StreamCodec.ofMember(S2CMessageStopBeam::encode, S2CMessageStopBeam::decode);

    private UUID playerId;

    public S2CMessageStopBeam() {}

    public S2CMessageStopBeam(UUID playerId) {
        this.playerId = playerId;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeUUID(this.playerId);
    }
    public static S2CMessageStopBeam decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageStopBeam(buffer.readUUID());
    }
    public static void handle(S2CMessageStopBeam message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleStopBeam(message));
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
