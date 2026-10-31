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
import top.kristeal.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class S2CMessageProjectileHitBlock implements CustomPacketPayload
{
    public static final Type<S2CMessageProjectileHitBlock> TYPE = new Type<>(Reference.id("s2c_projectile_hit_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageProjectileHitBlock> STREAM_CODEC = StreamCodec.ofMember(S2CMessageProjectileHitBlock::encode, S2CMessageProjectileHitBlock::decode);

    private double x;
    private double y;
    private double z;
    private BlockPos pos;
    private Direction face;

    public S2CMessageProjectileHitBlock() {}

    public S2CMessageProjectileHitBlock(double x, double y, double z, BlockPos pos, Direction face)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pos = pos;
        this.face = face;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        buffer.writeBlockPos(this.pos);
        buffer.writeEnum(this.face);
    }
    public static S2CMessageProjectileHitBlock decode(RegistryFriendlyByteBuf buffer)
    {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        BlockPos pos = buffer.readBlockPos();
        Direction face = buffer.readEnum(Direction.class);
        return new S2CMessageProjectileHitBlock(x, y, z, pos, face);
    }
    public static void handle(S2CMessageProjectileHitBlock message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleProjectileHitBlock(message));
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

    public BlockPos getPos()
    {
        return this.pos;
    }

    public Direction getFace()
    {
        return this.face;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
