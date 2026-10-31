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
import top.kristeal.network.BufferUtil;

/**
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload. The wire format is unchanged; the client
 * work (creating the TurretBulletTrail) moved to ClientPlayHandler.handleTurretBulletTrail.
 */
public class S2CMessageTurretBulletTrail implements CustomPacketPayload {
    public static final Type<S2CMessageTurretBulletTrail> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_turret_bullet_trail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageTurretBulletTrail> STREAM_CODEC = StreamCodec.ofMember(S2CMessageTurretBulletTrail::encode, S2CMessageTurretBulletTrail::decode);

    private int entityId;
    private Vec3 position;
    private Vec3 motion;
    private int trailColor;
    private double trailLengthMultiplier;
    private int maxAge;
    private double trailThickness;

    public S2CMessageTurretBulletTrail() {}

    public S2CMessageTurretBulletTrail(int entityId, Vec3 position, Vec3 motion,
                                       int trailColor, double trailLengthMultiplier,
                                       int maxAge, double trailThickness) {
        this.entityId = entityId;
        this.position = position;
        this.motion = motion;
        this.trailColor = trailColor;
        this.trailLengthMultiplier = trailLengthMultiplier;
        this.maxAge = maxAge;
        this.trailThickness = trailThickness;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        BufferUtil.writeVec3(buffer, this.position);
        BufferUtil.writeVec3(buffer, this.motion);
        buffer.writeInt(this.trailColor);
        buffer.writeDouble(this.trailLengthMultiplier);
        buffer.writeInt(this.maxAge);
        buffer.writeDouble(this.trailThickness);
    }

    public static S2CMessageTurretBulletTrail decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        Vec3 position = BufferUtil.readVec3(buffer);
        Vec3 motion = BufferUtil.readVec3(buffer);
        int trailColor = buffer.readInt();
        double trailLengthMultiplier = buffer.readDouble();
        int maxAge = buffer.readInt();
        double trailThickness = buffer.readDouble();

        return new S2CMessageTurretBulletTrail(entityId, position, motion, trailColor,
                trailLengthMultiplier, maxAge, trailThickness);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMessageTurretBulletTrail message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleTurretBulletTrail(message));
    }

    public int getEntityId() { return this.entityId; }
    public Vec3 getPosition() { return this.position; }
    public Vec3 getMotion() { return this.motion; }
    public int getTrailColor() { return this.trailColor; }
    public double getTrailLengthMultiplier() { return this.trailLengthMultiplier; }
    public int getMaxAge() { return this.maxAge; }
    public double getTrailThickness() { return this.trailThickness; }
}
