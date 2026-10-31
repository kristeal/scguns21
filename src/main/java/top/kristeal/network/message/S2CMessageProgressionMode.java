package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;
import top.kristeal.progression.ProgressionMode;

/** The server's progression mode, sent on login, after datapack reloads and when it changes. */
public record S2CMessageProgressionMode(ProgressionMode mode) implements CustomPacketPayload {
    public static final Type<S2CMessageProgressionMode> TYPE = new Type<>(Reference.id("s2c_progression_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageProgressionMode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> ProgressionMode.values()[i], ProgressionMode::ordinal), S2CMessageProgressionMode::mode,
            S2CMessageProgressionMode::new);

    public static void handle(S2CMessageProgressionMode message, IPayloadContext context) {
        boolean remote = !context.connection().isMemoryConnection(); // the integrated server shares the config
        context.enqueueWork(() -> ClientPlayHandler.handleProgressionMode(message.mode(), remote));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
