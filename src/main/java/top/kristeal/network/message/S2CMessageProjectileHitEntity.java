package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import top.kristeal.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class S2CMessageProjectileHitEntity implements CustomPacketPayload
{
    public static final Type<S2CMessageProjectileHitEntity> TYPE = new Type<>(Reference.id("s2c_projectile_hit_entity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageProjectileHitEntity> STREAM_CODEC = StreamCodec.ofMember(S2CMessageProjectileHitEntity::encode, S2CMessageProjectileHitEntity::decode);

    private double x;
    private double y;
    private double z;
    private int type;
    private boolean player;

    public S2CMessageProjectileHitEntity() {}

    public S2CMessageProjectileHitEntity(double x, double y, double z, int type, boolean player)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.player = player;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        buffer.writeByte(this.type);
        buffer.writeBoolean(this.player);
    }
    public static S2CMessageProjectileHitEntity decode(RegistryFriendlyByteBuf buffer)
    {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        byte type = buffer.readByte();
        boolean player = buffer.readBoolean();
        return new S2CMessageProjectileHitEntity(x, y, z, type, player);
    }
    public static void handle(S2CMessageProjectileHitEntity message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleProjectileHitEntity(message));
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getZ()
    {
        return this.z;
    }

    public boolean isHeadshot()
    {
        return this.type == HitType.HEADSHOT;
    }

    public boolean isCritical()
    {
        return this.type == HitType.CRITICAL;
    }

    public boolean isPlayer()
    {
        return this.player;
    }

    public static class HitType
    {
        public static final int NORMAL = 0;
        public static final int HEADSHOT = 1;
        public static final int CRITICAL = 2;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
