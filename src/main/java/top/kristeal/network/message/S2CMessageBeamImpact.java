package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import top.kristeal.client.handler.BeamHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CMessageBeamImpact implements CustomPacketPayload
{
    public static final Type<S2CMessageBeamImpact> TYPE = new Type<>(Reference.id("s2c_beam_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBeamImpact> STREAM_CODEC = StreamCodec.ofMember(S2CMessageBeamImpact::encode, S2CMessageBeamImpact::decode);


    private Vec3 hitPosition;
    private UUID playerUUID;

    // Default constructor required for decoding
    public S2CMessageBeamImpact() {}

    public S2CMessageBeamImpact(Vec3 hitPosition, UUID playerUUID) {
        this.hitPosition = hitPosition;
        this.playerUUID = playerUUID;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.hitPosition.x);
        buffer.writeDouble(this.hitPosition.y);
        buffer.writeDouble(this.hitPosition.z);
        buffer.writeUUID(this.playerUUID);
    }
    public static S2CMessageBeamImpact decode(RegistryFriendlyByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        Vec3 hitPosition = new Vec3(x, y, z);
        UUID playerUUID = buffer.readUUID();
        return new S2CMessageBeamImpact(hitPosition, playerUUID);
    }
    public static void handle(S2CMessageBeamImpact message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel world = Minecraft.getInstance().level;
            if (world != null) {
                Player player = world.getPlayerByUUID(message.playerUUID);
                if (player != null) {
                    BeamHandler.spawnBeamImpactParticles(world, message.hitPosition, player);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
