package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.kristeal.init.ModSyncedDataKeys;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class C2SMessageShooting implements CustomPacketPayload
{
    public static final Type<C2SMessageShooting> TYPE = new Type<>(Reference.id("c2s_shooting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageShooting> STREAM_CODEC = StreamCodec.ofMember(C2SMessageShooting::encode, C2SMessageShooting::decode);

    private boolean shooting;

    public C2SMessageShooting() {}

    public C2SMessageShooting(boolean shooting)
    {
        this.shooting = shooting;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.shooting);
    }
    public static C2SMessageShooting decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessageShooting(buffer.readBoolean());
    }
    public static void handle(C2SMessageShooting message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null)
            {
                ModSyncedDataKeys.SHOOTING.setValue(player, message.shooting);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
