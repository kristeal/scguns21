package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;

public class S2CMessageStunGrenade implements CustomPacketPayload
{
    public static final Type<S2CMessageStunGrenade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_stun_grenade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageStunGrenade> STREAM_CODEC = StreamCodec.ofMember(S2CMessageStunGrenade::encode, S2CMessageStunGrenade::decode);

    private double x, y, z;

    public S2CMessageStunGrenade() {}

    public S2CMessageStunGrenade(double x, double y, double z)
    {
        this.z = z;
        this.y = y;
        this.x = x;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
    }

    public static S2CMessageStunGrenade decode(RegistryFriendlyByteBuf buffer)
    {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        return new S2CMessageStunGrenade(x, y, z);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(S2CMessageStunGrenade message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleExplosionStunGrenade(message));
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getZ()
    {
        return z;
    }
}
