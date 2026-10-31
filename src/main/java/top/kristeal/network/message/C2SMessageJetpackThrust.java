package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.common.exosuit.ExoSuitFlightHandler;

public class C2SMessageJetpackThrust implements CustomPacketPayload {
    public static final Type<C2SMessageJetpackThrust> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_jetpack_thrust"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageJetpackThrust> STREAM_CODEC = StreamCodec.ofMember(C2SMessageJetpackThrust::encode, C2SMessageJetpackThrust::decode);

    private boolean isThrusting;

    public C2SMessageJetpackThrust() {}

    public C2SMessageJetpackThrust(boolean isThrusting) {
        this.isThrusting = isThrusting;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.isThrusting);
    }

    public static C2SMessageJetpackThrust decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageJetpackThrust(buffer.readBoolean());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageJetpackThrust message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
                ExoSuitFlightHandler.setPlayerThrusting(player, message.isThrusting);
            }
        });
    }
}
