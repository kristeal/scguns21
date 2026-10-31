package top.kristeal.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.common.exosuit.ExoSuitUpgradeManager;

import java.util.HashMap;
import java.util.Map;

public class S2CMessageSyncUpgradeRegistry implements CustomPacketPayload {
    public static final Type<S2CMessageSyncUpgradeRegistry> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_sync_upgrade_registry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageSyncUpgradeRegistry> STREAM_CODEC = StreamCodec.ofMember(S2CMessageSyncUpgradeRegistry::encode, S2CMessageSyncUpgradeRegistry::decode);

    private Map<ResourceLocation, CompoundTag> upgradeData;

    public S2CMessageSyncUpgradeRegistry() {
        this.upgradeData = new HashMap<>();
    }

    public S2CMessageSyncUpgradeRegistry(Map<ResourceLocation, CompoundTag> upgradeData) {
        this.upgradeData = upgradeData;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.upgradeData.size());
        for (Map.Entry<ResourceLocation, CompoundTag> entry : this.upgradeData.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeNbt(entry.getValue());
        }
    }

    public static S2CMessageSyncUpgradeRegistry decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<ResourceLocation, CompoundTag> upgradeData = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ResourceLocation itemId = buffer.readResourceLocation();
            CompoundTag tag = buffer.readNbt();
            if (tag != null) {
                upgradeData.put(itemId, tag);
            }
        }

        return new S2CMessageSyncUpgradeRegistry(upgradeData);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMessageSyncUpgradeRegistry message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ExoSuitUpgradeManager.deserializeUpgrades(message.getUpgradeData());
        });
    }

    public Map<ResourceLocation, CompoundTag> getUpgradeData() {
        return upgradeData;
    }
}
