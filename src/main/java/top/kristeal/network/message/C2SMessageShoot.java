package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import top.kristeal.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class C2SMessageShoot implements CustomPacketPayload
{
    public static final Type<C2SMessageShoot> TYPE = new Type<>(Reference.id("c2s_shoot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageShoot> STREAM_CODEC = StreamCodec.ofMember(C2SMessageShoot::encode, C2SMessageShoot::decode);

    private float rotationYaw;
    private float rotationPitch;

    public C2SMessageShoot() {}

    public C2SMessageShoot(Player player)
    {
        this.rotationYaw = player.getYRot();
        this.rotationPitch = player.getXRot();
    }

    public C2SMessageShoot(float rotationYaw, float rotationPitch)
    {
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeFloat(this.rotationYaw);
        buffer.writeFloat(this.rotationPitch);
    }
    public static C2SMessageShoot decode(RegistryFriendlyByteBuf buffer)
    {
        float rotationYaw = buffer.readFloat();
        float rotationPitch = buffer.readFloat();
        return new C2SMessageShoot(rotationYaw, rotationPitch);
    }
    public static void handle(C2SMessageShoot message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null)
            {
                ServerPlayHandler.handleShoot(message, player);
            }
        });
    }

    public float getRotationYaw()
    {
        return this.rotationYaw;
    }

    public float getRotationPitch()
    {
        return this.rotationPitch;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
