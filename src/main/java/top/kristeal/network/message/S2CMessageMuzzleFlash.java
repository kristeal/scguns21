package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;

/**
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload. The wire format is unchanged; the client
 * work (spawning the turret muzzle-flash particle) moved to ClientPlayHandler.handleTurretMuzzleFlash.
 */
public class S2CMessageMuzzleFlash implements CustomPacketPayload {
    public static final Type<S2CMessageMuzzleFlash> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_muzzle_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageMuzzleFlash> STREAM_CODEC = StreamCodec.ofMember(S2CMessageMuzzleFlash::encode, S2CMessageMuzzleFlash::decode);

    private Vec3 position;
    private float yaw;
    private float pitch;

    public S2CMessageMuzzleFlash() {}

    public S2CMessageMuzzleFlash(Vec3 position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(this.position.x);
        buffer.writeDouble(this.position.y);
        buffer.writeDouble(this.position.z);
        buffer.writeFloat(this.yaw);
        buffer.writeFloat(this.pitch);
    }

    public static S2CMessageMuzzleFlash decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageMuzzleFlash(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMessageMuzzleFlash message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleTurretMuzzleFlash(message));
    }

    public Vec3 getPosition() { return this.position; }
    public float getYaw() { return this.yaw; }
    public float getPitch() { return this.pitch; }
}
