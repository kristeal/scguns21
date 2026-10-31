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
public class S2CMessageRemoveProjectile implements CustomPacketPayload
{
    public static final Type<S2CMessageRemoveProjectile> TYPE = new Type<>(Reference.id("s2c_remove_projectile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageRemoveProjectile> STREAM_CODEC = StreamCodec.ofMember(S2CMessageRemoveProjectile::encode, S2CMessageRemoveProjectile::decode);

    private int entityId;

    public S2CMessageRemoveProjectile() {}

    public S2CMessageRemoveProjectile(int entityId)
    {
        this.entityId = entityId;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.entityId);
    }
    public static S2CMessageRemoveProjectile decode(RegistryFriendlyByteBuf buffer)
    {
        return new S2CMessageRemoveProjectile(buffer.readInt());
    }
    public static void handle(S2CMessageRemoveProjectile message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleRemoveProjectile(message));
    }


    public int getEntityId()
    {
        return this.entityId;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
