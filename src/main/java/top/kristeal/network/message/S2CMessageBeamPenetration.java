package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.kristeal.client.network.ClientPlayHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class S2CMessageBeamPenetration implements CustomPacketPayload
{
    public static final Type<S2CMessageBeamPenetration> TYPE = new Type<>(Reference.id("s2c_beam_penetration"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBeamPenetration> STREAM_CODEC = StreamCodec.ofMember(S2CMessageBeamPenetration::encode, S2CMessageBeamPenetration::decode);

    private UUID playerId;
    private List<GlassPenetrationData> penetrations;

    // Data class to hold penetration information
    public static class GlassPenetrationData {
        private final Vec3 position;
        private final Direction face;
        private final BlockPos blockPos;

        public GlassPenetrationData(Vec3 position, Direction face, BlockPos blockPos) {
            this.position = position;
            this.face = face;
            this.blockPos = blockPos;
        }

        public Vec3 getPosition() { return position; }
        public Direction getFace() { return face; }
        public BlockPos getBlockPos() { return blockPos; }
    }

    // Default constructor required for decoding
    public S2CMessageBeamPenetration() {
        this.penetrations = new ArrayList<>();
    }

    public S2CMessageBeamPenetration(UUID playerId, List<BlockHitResult> glassPenetrations) {
        this.playerId = playerId;
        this.penetrations = glassPenetrations.stream()
                .map(hit -> new GlassPenetrationData(
                        hit.getLocation(),
                        hit.getDirection(),
                        hit.getBlockPos()))
                .collect(Collectors.toList());
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeUUID(this.playerId);
        buffer.writeInt(this.penetrations.size());

        for (GlassPenetrationData penetration : this.penetrations) {
            // Write position
            buffer.writeDouble(penetration.position.x);
            buffer.writeDouble(penetration.position.y);
            buffer.writeDouble(penetration.position.z);

            // Write direction
            buffer.writeEnum(penetration.face);

            // Write block position
            buffer.writeBlockPos(penetration.blockPos);
        }
    }
    public static S2CMessageBeamPenetration decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        int size = buffer.readInt();

        List<BlockHitResult> penetrations = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Vec3 position = new Vec3(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
            Direction face = buffer.readEnum(Direction.class);
            BlockPos blockPos = buffer.readBlockPos();

            penetrations.add(new BlockHitResult(position, face, blockPos, false));
        }

        return new S2CMessageBeamPenetration(playerId, penetrations);
    }
    public static void handle(S2CMessageBeamPenetration message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleBeamPenetration(message));
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public List<GlassPenetrationData> getPenetrations() { return penetrations; }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
