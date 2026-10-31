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
import top.kristeal.common.exosuit.ExoSuitPowerManager;

public class C2SMessageJetpackState implements CustomPacketPayload {
    public static final Type<C2SMessageJetpackState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_jetpack_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageJetpackState> STREAM_CODEC = StreamCodec.ofMember(C2SMessageJetpackState::encode, C2SMessageJetpackState::decode);

    private boolean isActive;

    public C2SMessageJetpackState() {}

    public C2SMessageJetpackState(boolean isActive) {
        this.isActive = isActive;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.isActive);
    }

    public static C2SMessageJetpackState decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageJetpackState(buffer.readBoolean());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageJetpackState message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
                if (ExoSuitPowerManager.isPowerEnabled(player, "utility")) {
                    ExoSuitFlightHandler.setJetpackActive(player, message.isActive);
                } else {
                    ExoSuitFlightHandler.setJetpackActive(player, false);
                }
            }
        });
    }
}
