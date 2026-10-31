package top.kristeal.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.screen.ExoSuitMenu;
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.item.animated.ExoSuitItem;
import top.kristeal.network.PacketHandler;

import java.util.ArrayList;
import java.util.List;

public class C2SMessageSaveExoSuitUpgrades implements CustomPacketPayload {
    public static final Type<C2SMessageSaveExoSuitUpgrades> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_save_exosuit_upgrades"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageSaveExoSuitUpgrades> STREAM_CODEC = StreamCodec.ofMember(C2SMessageSaveExoSuitUpgrades::encode, C2SMessageSaveExoSuitUpgrades::decode);

    private List<ItemStack> upgradeStacks;

    public C2SMessageSaveExoSuitUpgrades() {
        this.upgradeStacks = new ArrayList<>();
    }

    public C2SMessageSaveExoSuitUpgrades(List<ItemStack> upgradeStacks) {
        this.upgradeStacks = upgradeStacks != null ? upgradeStacks : new ArrayList<>();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.upgradeStacks.size());
        for (ItemStack stack : this.upgradeStacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        }
    }

    public static C2SMessageSaveExoSuitUpgrades decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
        return new C2SMessageSaveExoSuitUpgrades(stacks);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageSaveExoSuitUpgrades message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof ExoSuitMenu menu) {
                ItemStack menuArmorPiece = menu.getArmorPiece();

                if (!menuArmorPiece.isEmpty() && menuArmorPiece.getItem() instanceof ExoSuitItem exoSuit) {
                    CompoundTag upgradeData = createUpgradeData(menuArmorPiece, message);
                    ExoSuitData.setUpgradeData(menuArmorPiece, upgradeData);

                    EquipmentSlot armorSlot = getEquipmentSlotForArmorType(exoSuit.getType());
                    ItemStack equippedPiece = serverPlayer.getItemBySlot(armorSlot);

                    // Only sync if the menu armor piece IS the equipped piece
                    if (!equippedPiece.isEmpty() &&
                            equippedPiece.getItem() instanceof ExoSuitItem &&
                            ItemStack.isSameItemSameComponents(menuArmorPiece, equippedPiece)) {

                        ExoSuitData.setUpgradeData(equippedPiece, upgradeData);
                        serverPlayer.setItemSlot(armorSlot, equippedPiece);

                        List<ServerPlayer> playersToSync = serverPlayer.serverLevel().getEntitiesOfClass(
                                ServerPlayer.class,
                                serverPlayer.getBoundingBox().inflate(128.0)
                        );

                        if (!playersToSync.contains(serverPlayer)) {
                            playersToSync.add(serverPlayer);
                        }

                        for (ServerPlayer nearbyPlayer : playersToSync) {
                            PacketHandler.getPlayChannel().sendToPlayer(
                                    () -> nearbyPlayer,
                                    new S2CMessageSyncExoSuitUpgrades(serverPlayer.getUUID(), armorSlot, upgradeData)
                            );
                        }
                    }
                }
            }
        });
    }

    private static @NotNull CompoundTag createUpgradeData(ItemStack menuArmorPiece, C2SMessageSaveExoSuitUpgrades message) {
        CompoundTag upgradeData = ExoSuitData.getUpgradeData(menuArmorPiece).copy();
        ListTag upgradeList = new ListTag();

        for (int i = 0; i < message.upgradeStacks.size(); i++) {
            ItemStack upgradeStack = message.upgradeStacks.get(i);
            if (!upgradeStack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                slotTag.put("Item", ExoSuitData.saveItemStack(upgradeStack));
                upgradeList.add(slotTag);
            }
        }
        upgradeData.put("Upgrades", upgradeList);
        return upgradeData;
    }

    private static EquipmentSlot getEquipmentSlotForArmorType(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            case BODY -> EquipmentSlot.BODY;
        };
    }
}
