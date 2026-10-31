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
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.common.exosuit.ExoSuitFlightHandler;
import top.kristeal.common.exosuit.ExoSuitPowerManager;
import top.kristeal.common.exosuit.ExoSuitUpgrade;
import top.kristeal.common.exosuit.ExoSuitUpgradeManager;
import top.kristeal.item.animated.ExoSuitItem;
import top.kristeal.item.exosuit.GasMaskModuleItem;
import top.kristeal.item.exosuit.NightVisionModuleItem;
import top.kristeal.item.exosuit.RebreatherModuleItem;
import top.kristeal.item.exosuit.TargetTrackerModuleItem;

/**
 * Network message to toggle ExoSuit power components
 */
public class C2SMessageToggleExoSuitPower implements CustomPacketPayload {
    public static final Type<C2SMessageToggleExoSuitPower> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_toggle_exosuit_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageToggleExoSuitPower> STREAM_CODEC = StreamCodec.ofMember(C2SMessageToggleExoSuitPower::encode, C2SMessageToggleExoSuitPower::decode);

    public enum PowerType {
        HELMET_HUD("hud", "HUD Module"),
        BOOTS_MOBILITY("mobility", "Mobility Enhancement"),
        JETPACK("jetpack", "Jetpack");

        private final String upgradeType;
        private final String displayName;

        PowerType(String upgradeType, String displayName) {
            this.upgradeType = upgradeType;
            this.displayName = displayName;
        }

        public String getUpgradeType() {
            return upgradeType;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private PowerType powerType;

    public C2SMessageToggleExoSuitPower() {
    }

    public C2SMessageToggleExoSuitPower(PowerType powerType) {
        this.powerType = powerType;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.powerType);
    }

    public static C2SMessageToggleExoSuitPower decode(RegistryFriendlyByteBuf buffer) {
        C2SMessageToggleExoSuitPower message = new C2SMessageToggleExoSuitPower();
        message.powerType = buffer.readEnum(PowerType.class);
        return message;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageToggleExoSuitPower message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if ("jetpack".equals(message.powerType.getUpgradeType())) {
                boolean currentState = ExoSuitPowerManager.isPowerEnabled(player, "utility");
                boolean newState = !currentState;

                ExoSuitPowerManager.setPowerEnabled(player, "utility", newState);

                ExoSuitFlightHandler.setJetpackActive(player, newState);

                String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
                ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;

                Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(statusKey,
                                        Component.translatable("exosuit.upgrade.jetpack"))
                                .withStyle(statusColor));

                player.sendSystemMessage(feedbackMessage, true);

                if (newState) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
                } else {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.8f);
                }
                return;
            }
            if (!ExoSuitPowerManager.canUpgradeFunction(player, message.powerType.getUpgradeType())) {
                String moduleTranslationKey = getSpecificModuleTranslationKey(player, message.powerType.getUpgradeType());

                Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("exosuit.message.not_available",
                                        Component.translatable(moduleTranslationKey))
                                .withStyle(ChatFormatting.RED));

                player.sendSystemMessage(feedbackMessage, true);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5f, 0.8f);
                return;
            }

            boolean newState = ExoSuitPowerManager.togglePower(player, message.powerType.getUpgradeType());
            String moduleTranslationKey = getSpecificModuleTranslationKey(player, message.powerType.getUpgradeType());
            String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
            ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;

            Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(statusKey,
                                    Component.translatable(moduleTranslationKey))
                            .withStyle(statusColor));

            player.sendSystemMessage(feedbackMessage, true);
            if (newState) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
            } else {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.8f);
            }
        });
    }

    /**
     * Gets the specific translation key for the currently equipped module
     */
    private static String getSpecificModuleTranslationKey(ServerPlayer player, String upgradeType) {
        if ("hud".equals(upgradeType)) {
            ItemStack hudModule = findHudModule(player);
            if (!hudModule.isEmpty()) {
                if (hudModule.getItem() instanceof NightVisionModuleItem) {
                    return "exosuit.upgrade.night_vision";
                } else if (hudModule.getItem() instanceof TargetTrackerModuleItem) {
                    return "exosuit.upgrade.target_tracker";
                } else if (hudModule.getItem() instanceof GasMaskModuleItem) {
                    return "exosuit.upgrade.gas_mask";
                } else if (hudModule.getItem() instanceof RebreatherModuleItem) {
                    return "exosuit.upgrade.rebreather";
                }
            }
            return "exosuit.upgrade.hud";
        }

        return "exosuit.upgrade." + upgradeType;
    }

    /**
     * Finds the currently equipped HUD module in the player's helmet
     */
    private static ItemStack findHudModule(ServerPlayer player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem exosuit &&
                    exosuit.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET) {
                for (int slot = 0; slot < 4; slot++) {
                    ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
                    if (!upgradeItem.isEmpty()) {
                        ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                        if (upgrade != null && upgrade.getType().equals("hud")) {
                            return upgradeItem;
                        }
                    }
                }
                break;
            }
        }
        return ItemStack.EMPTY;
    }
}
