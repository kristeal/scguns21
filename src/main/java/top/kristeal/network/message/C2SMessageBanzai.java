package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.handler.MeleeAttackHandler;

/**
 * Starts the bayonet charge when the melee key goes down while sprinting, and stops it when the key is released.
 * The server decides whether the charge can start and ends it on its own as well (see MeleeAttackHandler#tickBanzai).
 */
public record C2SMessageBanzai(boolean start) implements CustomPacketPayload
{
    public static final Type<C2SMessageBanzai> TYPE = new Type<>(Reference.id("c2s_banzai"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageBanzai> STREAM_CODEC = StreamCodec.ofMember(C2SMessageBanzai::encode, C2SMessageBanzai::decode);

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.start);
    }

    public static C2SMessageBanzai decode(RegistryFriendlyByteBuf buffer)
    {
        return new C2SMessageBanzai(buffer.readBoolean());
    }

    public static void handle(C2SMessageBanzai message, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
            if (message.start) {
                MeleeAttackHandler.startBanzai(player);
            } else {
                MeleeAttackHandler.stopBanzai(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
