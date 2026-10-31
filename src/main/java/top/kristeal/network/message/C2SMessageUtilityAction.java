package top.kristeal.network.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.common.exosuit.*;
import top.kristeal.item.animated.ExoSuitItem;

/**
 * Context-aware utility action handler - opens pouches or toggles jetpack based on equipped upgrade
 */
public class C2SMessageUtilityAction implements CustomPacketPayload {
    public static final Type<C2SMessageUtilityAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_utility_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageUtilityAction> STREAM_CODEC = StreamCodec.ofMember(C2SMessageUtilityAction::encode, C2SMessageUtilityAction::decode);

    public C2SMessageUtilityAction() {
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
    }

    public static C2SMessageUtilityAction decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageUtilityAction();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageUtilityAction message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Find chestplate and check what utility upgrade is equipped
            ItemStack chestplate = player.getInventory().getArmor(2);
            if (!(chestplate.getItem() instanceof ExoSuitItem)) {
                return;
            }

            // Check utility slot (slot 3) for upgrade type
            ItemStack utilityUpgrade = ExoSuitData.getUpgradeInSlot(chestplate, 3);
            if (utilityUpgrade.isEmpty()) {
                return;
            }

            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(utilityUpgrade);
            if (upgrade == null) {
                return;
            }

            // Check if this is a jetpack (flight-capable utility upgrade)
            if (upgrade.getEffects().hasFlight()) {
                message.handleJetpackToggle(player);
            }
            // Check if this is a pouches upgrade
            else if (upgrade.getType().equals("pouches")) {
                ExoSuitPouchHandler.openPouchInventory(player);
            }
            // Handle other utility upgrades with storage
            else if (upgrade.getType().equals("utility") && upgrade.getDisplay().getStorageSize() > 0) {
                ExoSuitPouchHandler.openPouchInventory(player);
            }
            // For other utility upgrades without special actions, give feedback
            else {
                Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("exosuit.message.no_action")
                                .withStyle(ChatFormatting.GRAY));

                player.sendSystemMessage(feedbackMessage, true);
            }
        });
    }

    private void handleJetpackToggle(ServerPlayer player) {
        // Check if utility upgrade can function (since jetpack is a utility upgrade)
        if (!ExoSuitPowerManager.canUpgradeFunction(player, "utility")) {
            Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("exosuit.message.not_available",
                                    Component.translatable("exosuit.upgrade.jetpack"))
                            .withStyle(ChatFormatting.RED));

            player.sendSystemMessage(feedbackMessage, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5f, 0.8f);
            return;
        }

        // Toggle utility power (since jetpack is a utility upgrade)
        boolean newState = ExoSuitPowerManager.togglePower(player, "utility");

        // Send feedback
        String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
        ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;

        Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(statusKey,
                                Component.translatable("exosuit.upgrade.jetpack"))
                        .withStyle(statusColor));

        player.sendSystemMessage(feedbackMessage, true);

        // Play sound
        if (newState) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.8f);
        }
    }
}
