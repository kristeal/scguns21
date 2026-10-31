package top.kristeal.item.animated;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.kristeal.client.KeyBinds;
import top.kristeal.client.render.armor.ExoSuitRenderer;
import top.kristeal.client.screen.ExoSuitMenu;
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.common.exosuit.ExoSuitUpgrade;
import top.kristeal.common.exosuit.ExoSuitUpgradeManager;
import top.kristeal.item.exosuit.DamageableUpgradeItem;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ExoSuitItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExoSuitItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
        super(pMaterial, pType, pProperties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ExoSuitRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity, ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = new ExoSuitRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                openExoSuitScreen((ServerPlayer) player, hand);
            }
            return InteractionResultHolder.success(itemStack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player != null && player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                openExoSuitScreen((ServerPlayer) player, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void openExoSuitScreen(ServerPlayer player, InteractionHand hand) {
        player.openMenu(new ExoSuitMenuProvider(hand), buf -> {
            buf.writeEnum(hand);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.scguns.exosuit.frame"));

        if (ExoSuitData.hasUpgrades(stack)) {
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.has_upgrades"));
            int upgradeCount = getCurrentUpgradeCount(stack);
            int maxSlots = getMaxUpgradeSlots();
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots_used", upgradeCount, maxSlots)
                    .withStyle(ChatFormatting.GRAY));

            addPowerCoreInfo(stack, tooltip);

            addPouchInfo(stack, tooltip);

            addUpgradeDurabilityInfo(stack, tooltip);
        } else {
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.no_upgrades"));
        }

        addSlotInformation(tooltip);
        tooltip.add(Component.translatable("info.scguns.exosuit_help",
                        KeyBinds.KEY_ATTACHMENTS.getTranslatedKeyMessage().getString().toUpperCase(Locale.ENGLISH))
                .withStyle(ChatFormatting.YELLOW));
    }

    private void addPowerCoreInfo(ItemStack stack, List<Component> tooltip) {
        if (this.getType() != ArmorItem.Type.CHESTPLATE) {
            return;
        }

        ItemStack powerCore = findPowerCore(stack);
        if (!powerCore.isEmpty()) {
            IEnergyStorage energy = powerCore.getCapability(Capabilities.EnergyStorage.ITEM);
            int energyStored = energy != null ? energy.getEnergyStored() : 0;
            int maxEnergy = energy != null ? energy.getMaxEnergyStored() : 0;

            if (maxEnergy > 0) {
                int energyPercent = (energyStored * 100) / maxEnergy;
                ChatFormatting energyColor = getEnergyColor(energyPercent);

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.power_core").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.energy_level", energyPercent)
                        .withStyle(energyColor));
            }
        }
    }

    private ItemStack findPowerCore(ItemStack stack) {
        for (int slot = 0; slot < getMaxUpgradeSlots(); slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("power_core")) {
                    return upgradeItem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ChatFormatting getEnergyColor(int energyPercent) {
        if (energyPercent > 75) {
            return ChatFormatting.GREEN;
        } else if (energyPercent > 50) {
            return ChatFormatting.YELLOW;
        } else if (energyPercent > 25) {
            return ChatFormatting.GOLD;
        } else if (energyPercent > 0) {
            return ChatFormatting.RED;
        } else {
            return ChatFormatting.DARK_RED;
        }
    }

    private void addUpgradeDurabilityInfo(ItemStack stack, List<Component> tooltip) {
        boolean hasAnyDamageableUpgrades = false;

        for (int slot = 0; slot < getMaxUpgradeSlots(); slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && isUpgradeDamageable(upgradeItem)) {
                    if (!hasAnyDamageableUpgrades) {
                        tooltip.add(Component.literal(""));
                        tooltip.add(Component.translatable("tooltip.scguns.exosuit.upgrade_condition").withStyle(ChatFormatting.YELLOW));
                        hasAnyDamageableUpgrades = true;
                    }

                    String upgradeName = getUpgradeDisplayName(upgrade, upgradeItem);
                    int durabilityPercent = getDurabilityPercentage(upgradeItem);
                    ChatFormatting color = getDurabilityColor(durabilityPercent);

                    tooltip.add(Component.translatable("tooltip.scguns.exosuit.upgrade_durability", upgradeName, durabilityPercent)
                            .withStyle(color));
                }
            }
        }
    }

    private String getUpgradeDisplayName(ExoSuitUpgrade upgrade, ItemStack upgradeItem) {
        String upgradeType = upgrade.getType();

        if ("hud".equals(upgradeType)) {
            if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.NightVisionModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.night_vision").getString();
            } else if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.TargetTrackerModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.target_tracker").getString();
            } else if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.GasMaskModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.gas_mask").getString();
            } else if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.RebreatherModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.rebreather").getString();
            } else {
                return Component.translatable("upgrade.scguns.exosuit.hud_system").getString();
            }
        }
        if ("breathing".equals(upgradeType)) {
            if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.GasMaskModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.gas_mask").getString();
            } else if (upgradeItem.getItem() instanceof top.kristeal.item.exosuit.RebreatherModuleItem) {
                return Component.translatable("upgrade.scguns.exosuit.rebreather").getString();
            } else {
                return Component.translatable("upgrade.scguns.exosuit.life_support").getString();
            }
        }
        return switch (upgradeType) {
            case "plating" -> Component.translatable("upgrade.scguns.exosuit.armor_plating").getString();
            case "pauldron" -> Component.translatable("upgrade.scguns.exosuit.pauldron").getString();
            case "power_core" -> Component.translatable("upgrade.scguns.exosuit.power_core").getString();
            case "utility" -> Component.translatable("upgrade.scguns.exosuit.utility_module").getString();
            case "knee_guard" -> Component.translatable("upgrade.scguns.exosuit.knee_guard").getString();
            case "mobility" -> Component.translatable("upgrade.scguns.exosuit.mobility_system").getString();
            default -> {
                String itemName = upgradeItem.getHoverName().getString();
                if (itemName.startsWith("Heavy ")) {
                    itemName = itemName.substring(6);
                }
                yield itemName;
            }
        };
    }

    private int getDurabilityPercentage(ItemStack upgradeItem) {
        if (!isUpgradeDamageable(upgradeItem)) {
            return 100;
        }

        int maxDamage = upgradeItem.getMaxDamage();
        int currentDamage = upgradeItem.getDamageValue();

        if (maxDamage <= 0) {
            return 100;
        }

        int remainingDurability = maxDamage - currentDamage;
        return Math.max(0, (remainingDurability * 100) / maxDamage);
    }

    private ChatFormatting getDurabilityColor(int durabilityPercent) {
        if (durabilityPercent > 75) {
            return ChatFormatting.GREEN;
        } else if (durabilityPercent > 50) {
            return ChatFormatting.YELLOW;
        } else if (durabilityPercent > 25) {
            return ChatFormatting.GOLD;
        } else if (durabilityPercent > 0) {
            return ChatFormatting.RED;
        } else {
            return ChatFormatting.DARK_RED;
        }
    }

    private boolean isUpgradeDamageable(ItemStack upgradeItem) {
        if (upgradeItem.isEmpty()) {
            return false;
        }
        if (upgradeItem.getItem() instanceof DamageableUpgradeItem) {
            return true;
        }
        if (upgradeItem.isDamageableItem()) {
            return true;
        }
        return upgradeItem.getMaxDamage() > 0;
    }

    private void addPouchInfo(ItemStack stack, List<Component> tooltip) {
        if (this.getType() != ArmorItem.Type.CHESTPLATE) {
            return;
        }

        ItemStack pouchUpgrade = findPouchUpgrade(stack);
        if (!pouchUpgrade.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade != null) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouches").withStyle(ChatFormatting.YELLOW));
                String pouchName = pouchUpgrade.getHoverName().getString();
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_equipped", pouchName)
                        .withStyle(ChatFormatting.GREEN));
                if (isPouchEmpty(stack, pouchUpgrade)) {
                    tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_empty")
                            .withStyle(ChatFormatting.DARK_GREEN));
                } else {
                    tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_has_items")
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    private ItemStack findPouchUpgrade(ItemStack stack) {
        for (int slot = 0; slot < getMaxUpgradeSlots(); slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("pouches")) {
                    return upgradeItem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isPouchEmpty(ItemStack chestplate, ItemStack pouchUpgrade) {
        try {
            String pouchId = pouchUpgrade.getItem().toString();
            CompoundTag pouchData = ExoSuitData.getCustomTag(chestplate).getCompound("PouchData");

            if (!pouchData.contains(pouchId)) {
                return true;
            }

            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
                return true;
            }

            net.neoforged.neoforge.items.ItemStackHandler handler = new net.neoforged.neoforge.items.ItemStackHandler(upgrade.getDisplay().getStorageSize());
            handler.deserializeNBT(ExoSuitData.REGISTRY_ACCESS, pouchData.getCompound(pouchId));

            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private void addSlotInformation(List<Component> tooltip) {
        switch (this.getType()) {
            case HELMET:
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.helmet"));
                break;
            case CHESTPLATE:
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.chest"));
                break;
            case LEGGINGS:
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.legs"));
                break;
            case BOOTS:
                tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.boots"));
                break;
            case BODY:
                break;
        }
    }

    public int getMaxUpgradeSlots() {
        return switch (this.getType()) {
            case HELMET -> 3;      // Plating, HUD, Breathing
            case CHESTPLATE -> 4;  // Fixed to 4 instead of 5 to match the screen layout
            case LEGGINGS -> 3;    // Plating, Knee Guards, Utility
            case BOOTS -> 2;       // Plating, Mobility
            case BODY -> 3;
        };
    }

    public int getCurrentUpgradeCount(ItemStack stack) {
        CompoundTag upgradeData = ExoSuitData.getUpgradeData(stack);

        if (upgradeData.contains("Upgrades")) {
            ListTag upgradeList = upgradeData.getList("Upgrades", 10); // 10 = CompoundTag type
            return upgradeList.size();
        }

        return 0;
    }

    private PlayState predicate(AnimationState animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.exo_suit.idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public record ExoSuitMenuProvider(InteractionHand hand) implements MenuProvider {

        @Override
        public @NotNull Component getDisplayName() {
            return Component.translatable("container.scguns.exosuit");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
            return new ExoSuitMenu(id, playerInventory, hand);
        }
    }
}
