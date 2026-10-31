package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.common.TurretManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Fix for a reference bug: turret configs are server data, so a dedicated-server client had none and its turret tooltips
 * were empty. The server sends the raw config JSON on login and after every datapack reload.
 */
public record S2CMessageTurretConfigs(Map<ResourceLocation, String> configs) implements CustomPacketPayload {
    public static final Type<S2CMessageTurretConfigs> TYPE = new Type<>(Reference.id("s2c_turret_configs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageTurretConfigs> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.STRING_UTF8), S2CMessageTurretConfigs::configs,
            S2CMessageTurretConfigs::new);

    public static void handle(S2CMessageTurretConfigs message, IPayloadContext context) {
        if (context.connection().isMemoryConnection()) {
            return; // singleplayer/LAN host: the integrated server already filled the shared map
        }
        context.enqueueWork(() -> TurretManager.applySynced(message.configs()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
