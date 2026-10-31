package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import top.kristeal.client.network.ClientPlayHandler;

import java.util.UUID;

public class S2CMessageBeamUpdate implements CustomPacketPayload
{
    public static final Type<S2CMessageBeamUpdate> TYPE = new Type<>(Reference.id("s2c_beam_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBeamUpdate> STREAM_CODEC = StreamCodec.ofMember(S2CMessageBeamUpdate::encode, S2CMessageBeamUpdate::decode);


    private UUID playerId;
    private Vec3 startPos;
    private Vec3 endPos;

    public S2CMessageBeamUpdate() {}

    public S2CMessageBeamUpdate(UUID playerId, Vec3 startPos, Vec3 endPos) {
        this.playerId = playerId;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeUUID(this.playerId);
        buffer.writeDouble(this.startPos.x);
        buffer.writeDouble(this.startPos.y);
        buffer.writeDouble(this.startPos.z);
        buffer.writeDouble(this.endPos.x);
        buffer.writeDouble(this.endPos.y);
        buffer.writeDouble(this.endPos.z);
    }
    public static S2CMessageBeamUpdate decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        Vec3 startPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 endPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new S2CMessageBeamUpdate(playerId, startPos, endPos);
    }
    public static void handle(S2CMessageBeamUpdate message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleBeamUpdate(message));
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public Vec3 getStartPos() { return startPos; }
    public Vec3 getEndPos() { return endPos; }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
