package top.kristeal.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;

import java.util.UUID;

public class S2CMessageSyncExoSuitUpgrades implements CustomPacketPayload {
    public static final Type<S2CMessageSyncExoSuitUpgrades> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_sync_exosuit_upgrades"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageSyncExoSuitUpgrades> STREAM_CODEC = StreamCodec.ofMember(S2CMessageSyncExoSuitUpgrades::encode, S2CMessageSyncExoSuitUpgrades::decode);

    private UUID playerId;
    private EquipmentSlot armorSlot;
    private CompoundTag upgradeData;

    public S2CMessageSyncExoSuitUpgrades() {}

    public S2CMessageSyncExoSuitUpgrades(UUID playerId, EquipmentSlot armorSlot, CompoundTag upgradeData) {
        this.playerId = playerId;
        this.armorSlot = armorSlot;
        this.upgradeData = upgradeData;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.playerId);
        buffer.writeEnum(this.armorSlot);
        buffer.writeNbt(this.upgradeData);
    }

    public static S2CMessageSyncExoSuitUpgrades decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        EquipmentSlot armorSlot = buffer.readEnum(EquipmentSlot.class);
        CompoundTag upgradeData = buffer.readNbt();
        return new S2CMessageSyncExoSuitUpgrades(playerId, armorSlot, upgradeData != null ? upgradeData : new CompoundTag());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMessageSyncExoSuitUpgrades message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleSyncExoSuitUpgrades(message));
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public EquipmentSlot getArmorSlot() {
        return armorSlot;
    }

    public CompoundTag getUpgradeData() {
        return upgradeData;
    }
}
