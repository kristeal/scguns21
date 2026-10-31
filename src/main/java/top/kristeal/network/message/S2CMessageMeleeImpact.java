package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.handler.GunRenderingHandler;

/**
 * Tells the attacking player's client that a bayonet hit or a charge wall bounce landed, so it plays the impact kick.
 * The server used to call the client-only renderer directly.
 */
public record S2CMessageMeleeImpact() implements CustomPacketPayload
{
    public static final Type<S2CMessageMeleeImpact> TYPE = new Type<>(Reference.id("s2c_melee_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageMeleeImpact> STREAM_CODEC = StreamCodec.unit(new S2CMessageMeleeImpact());

    public static void handle(S2CMessageMeleeImpact message, IPayloadContext context)
    {
        context.enqueueWork(() -> GunRenderingHandler.get().triggerBanzaiImpact());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
