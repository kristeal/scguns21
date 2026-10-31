package top.kristeal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.kristeal.Config;
import top.kristeal.client.KeyBinds;
import top.kristeal.common.*;
import top.kristeal.init.ModEnchantments;
import top.kristeal.init.ModItems;
import top.kristeal.init.ModTags;
import top.kristeal.item.attachment.IAttachment;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class GunItem extends Item implements IColored, IMeta {
    private final WeakHashMap<CustomData, Gun> modifiedGunCache = new WeakHashMap<>(); // Port note: keyed by the GUN_OVERRIDE component
    private Gun gun = new Gun();

    public GunItem(Item.Properties properties) {
        super(properties);
    }

    public void setGun(NetworkGunManager.Supplier supplier) {
        this.gun = supplier.getGun();
    }

    public Gun getGun() {
        return this.gun;
    }
    /** Research progression: "Prototype <gun>" for prototypes. */
    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return GunStackData.isPrototype(stack) ? Component.translatable("item.scguns.prototype_name", name) : name;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Gun modifiedGun = this.getModifiedGun(stack);

        if (GunStackData.isPrototype(stack)) { // research progression
            tooltip.add(Component.translatable("info.scguns.prototype",
                    Math.round((1.0F - GunModifierHelper.getPrototypeDamageMultiplier(stack)) * 100)).withStyle(ChatFormatting.RED));
        }

        ResourceLocation advantage = modifiedGun.getProjectile().getAdvantage();
        float baseDamage = modifiedGun.getProjectile().getDamage();
        baseDamage = GunModifierHelper.getModifiedProjectileDamage(stack, baseDamage);
        baseDamage = GunEnchantmentHelper.getAcceleratorDamage(stack, baseDamage);
        baseDamage = GunEnchantmentHelper.getHeavyShotDamage(stack, baseDamage);

        baseDamage *= Config.COMMON.gameplay.globalDamageMultiplier.get().floatValue();
        baseDamage *= GunModifierHelper.getPrototypeDamageMultiplier(stack); // research progression
        String additionalDamageText = "";
        {
            if (GunStackData.hasAdditionalDamage(stack)) { // Port note: ADDITIONAL_DAMAGE component
                float additionalDamage = GunStackData.getAdditionalDamage(stack);
                additionalDamage += GunModifierHelper.getAdditionalDamage(stack, false);

                if (additionalDamage > 0) {
                    additionalDamageText = ChatFormatting.GREEN + " +" + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(additionalDamage);
                } else if (additionalDamage < 0) {
                    additionalDamageText = ChatFormatting.RED + " " + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(additionalDamage);
                }
            }
        }

        tooltip.add(Component.translatable("info.scguns.damage")
                .append(": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(baseDamage) + additionalDamageText).withStyle(ChatFormatting.WHITE)));
        float baseArmorPen = modifiedGun.getProjectile().getArmorPen();
        float puncturingPen = GunEnchantmentHelper.getPuncturingArmorBypass(stack);
        float totalArmorPen = baseArmorPen + puncturingPen;

        if (totalArmorPen > 0) {
            String armorPenText = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(totalArmorPen);
            tooltip.add(Component.translatable("info.scguns.armor_penetration")
                    .append(": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(armorPenText).withStyle(ChatFormatting.YELLOW)));
        }

        if (!advantage.equals(ModTags.Entities.NONE.location())) {
            tooltip.add(Component.translatable("info.scguns.advantage").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("advantage." + advantage).withStyle(ChatFormatting.GOLD)));
        }
        String fireMode = modifiedGun.getGeneral().getFireMode().id().toString();
        tooltip.add(Component.translatable("info.scguns.fire_mode").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("fire_mode." + fireMode).withStyle(ChatFormatting.WHITE)));

        Item ammo = modifiedGun.getProjectile().getItem();
        Item reloadItem = modifiedGun.getReloads().getReloadItem();
        if (modifiedGun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
            ammo = reloadItem;
        }

        // Port note: the reference showed these lines only once the stack had any NBT, which the tag-creating calls
        // (e.g. getBarWidth) made true almost immediately; with components they always show.
        {
            if (GunStackData.isIgnoreAmmo(stack)) {
                tooltip.add(Component.translatable("info.scguns.ignore_ammo").withStyle(ChatFormatting.AQUA));
            } else {
                int ammoCount = GunStackData.getAmmoCount(stack);
                tooltip.add(Component.translatable("info.scguns.ammo")
                        .append(": ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(ammoCount + "/" + GunModifierHelper.getModifiedAmmoCapacity(stack, modifiedGun)).withStyle(ChatFormatting.WHITE)));
            }
        }
        float totalMeleeDamage = getTotalMeleeDamage(stack);
        if (totalMeleeDamage > 0) {
            String meleeDamageText = (totalMeleeDamage % 1.0 == 0) ? String.format("%d", (int) totalMeleeDamage) : String.format("%.1f", totalMeleeDamage);
            tooltip.add(Component.translatable("info.scguns.melee_damage")
                    .append(": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(meleeDamageText).withStyle(ChatFormatting.WHITE)));
        }
        ResourceLocation effectLocation = modifiedGun.getProjectile().getImpactEffect();
        if (effectLocation != null) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectLocation);
            if (effect != null) {
                tooltip.add(Component.translatable("info.scguns.impact_effect").withStyle(ChatFormatting.GRAY)
                        .append(": ")
                        .append(Component.translatable(effect.getDescriptionId()).withStyle(ChatFormatting.BLUE)));
            }
        }

        Gun.WeaponType weaponType = modifiedGun.getGeneral().getWeaponType();
        if (weaponType != null) {
            String weaponTypeKey = "desc.scguns." + weaponType.name().toLowerCase();
            tooltip.add(Component.translatable("info.scguns.weapon_type").withStyle(ChatFormatting.GRAY)
                    .append(": ")
                    .append(Component.translatable(weaponTypeKey).withStyle(ChatFormatting.AQUA)));
        }
        if (ammo != null) {
            tooltip.add(Component.translatable("info.scguns.ammo_type", Component.translatable(ammo.getDescriptionId()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }

        if (modifiedGun.getGeneral().allowsAmmoChange() && !modifiedGun.getGeneral().getAvailableAmmoTypes().isEmpty()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("info.scguns.available_ammo_types").withStyle(ChatFormatting.GRAY));

                int currentIndex = modifiedGun.getGeneral().getCurrentAmmoTypeIndex();
                List<String> ammoTypes = modifiedGun.getGeneral().getAvailableAmmoTypes();

                for (int i = 0; i < ammoTypes.size(); i++) {
                    String ammoType = ammoTypes.get(i);
                    ResourceLocation ammoLocation = ResourceLocation.parse(
                            ammoType.contains(":") ? ammoType : top.kristeal.Reference.MOD_ID + ":" + ammoType
                    );
                    Item ammoItem = BuiltInRegistries.ITEM.get(ammoLocation);

                    if (ammoItem != null) {
                        Component ammoName = Component.translatable(ammoItem.getDescriptionId());
                        if (i == currentIndex) {
                            tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.YELLOW)
                                    .append(ammoName.copy().withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(" ✓").withStyle(ChatFormatting.GREEN)));
                        } else {
                            tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.GRAY)
                                    .append(ammoName.copy().withStyle(ChatFormatting.WHITE)));
                        }
                    }
                }
            } else {
                tooltip.add(Component.translatable("info.scguns.ammo_swap_available").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            }
        }
        if (GunStackData.isPrototype(stack)) {
            // The attachment key only says "prototypes can't take attachments", so the hint says that instead
            tooltip.add(Component.translatable("info.scguns.prototype_no_attachments").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("info.scguns.attachment_help", KeyBinds.KEY_ATTACHMENTS.getTranslatedKeyMessage().getString().toUpperCase(Locale.ENGLISH)).withStyle(ChatFormatting.YELLOW));
        }
    }



    /** Melee and charge bonus of the bayonet built into some guns (#scguns:built_in_bayonet). */
    public static final float BUILT_IN_BAYONET_DAMAGE = 5.0F;

    /** The bayonet's bonus to melee and charge damage: the attached bayonet's, or the built-in one's. */
    public float getBayonetAdditionalDamage(ItemStack gunStack) {
        float additionalDamage = isBuiltInBayonetGun(gunStack) ? BUILT_IN_BAYONET_DAMAGE : 0.0F;
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
            if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
                additionalDamage += ((BayonetItem) attachmentStack.getItem()).getAdditionalDamage();
            }
        }
        return additionalDamage;
    }

    public float getTotalMeleeDamage(ItemStack stack) {
        Gun gun = this.getModifiedGun(stack);
        float baseMeleeDamage = gun.getGeneral().getMeleeDamage();
        float bayonetDamage = getBayonetAdditionalDamage(stack);
        return baseMeleeDamage + bayonetDamage;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        Gun gun = ((GunItem) stack.getItem()).getModifiedGun(stack);
        return gun.getGeneral().getRate() * 4;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        this.getModifiedGun(stack); // Port note: the reference's stack.getOrCreateTag() here only forced an NBT tag to exist
        return Math.round(13.0F - (float) stack.getDamageValue() * 13.0F / (float) this.getMaxDamage(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (stack.getDamageValue() >= (stack.getMaxDamage() - 1)) {
            return 0x808080;
        }

        if (stack.getDamageValue() >= (stack.getMaxDamage() / 1.5)) {
            return Objects.requireNonNull(ChatFormatting.RED.getColor());
        }
        float stackMaxDamage = this.getMaxDamage(stack);
        float f = Math.max(0.0F, (stackMaxDamage - (float) stack.getDamageValue()) / stackMaxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public Gun getModifiedGun(ItemStack stack) {
        CustomData override = GunStackData.getGunOverride(stack); // Port note: GUN_OVERRIDE holds the reference's "Gun" and "Custom"
        CompoundTag tagCompound = override != null ? override.copyTag() : null;
        if (tagCompound != null && tagCompound.contains("Gun", Tag.TAG_COMPOUND)) {
            return this.modifiedGunCache.computeIfAbsent(override, item -> {
                if (tagCompound.getBoolean("Custom")) {
                    return Gun.create(tagCompound.getCompound("Gun"));
                } else {
                    Gun gunCopy = this.gun.copy();
                    gunCopy.deserializeNBT(tagCompound.getCompound("Gun"));
                    return gunCopy;
                }
            });
        }
        return this.gun;
    }

    // Port note: canApplyAtEnchantingTable became supportsEnchantment (holders). The reference's SEMI_AUTO_GUN branch
    // is dropped with the category: no enchantment used it. Mining guns still accept Fortune and Silk Touch.
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.is(ModTags.Items.MINING_GUN)) {
            if (enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.SILK_TOUCH)) {
                return true;
            }
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return this.getMaxStackSize(stack) == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 13;
    }

    // Port note: NeoForge 1.21 removed Item#initializeClient. GunItemStackRenderer is registered for every non-GeckoLib
    // GunItem in ClientHandler#onRegisterClientExtensions (AnimatedGunItem gets AnimatedGunRenderer from GeckoLib instead).

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
        return pRepair.is(ModItems.REPAIR_KIT.get());
    }

    public ItemStack getAttachment(ItemStack heldItem, IAttachment.Type type) {
        return Gun.getAttachment(type, heldItem);
    }

    public boolean hasBayonet(ItemStack gunStack) {
        if (isBuiltInBayonetGun(gunStack)) {
            return true;
        }
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
            if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
                return true;
            }
        }
        return false;
    }
    public boolean isBuiltInBayonetGun(ItemStack gunStack) {
        return gunStack.is(ModTags.Items.BUILT_IN_BAYONET);
    }


    public boolean hasExtendedBarrel(ItemStack gunStack) {
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
            if (attachmentStack != null && attachmentStack.getItem() instanceof ExtendedBarrelItem) {
                return true;
            }
        }
        return false;
    }
    public boolean isOneHandedCarbineCandidate(ItemStack gunStack) {
        return gunStack.is(ModTags.Items.ONE_HANDED_CARBINE);
    }

    public boolean isOneHandedCarbineActive(ItemStack gunStack) {
        return isOneHandedCarbineCandidate(gunStack) &&
                !Gun.hasExtendedBarrel(gunStack) &&
                !Gun.hasStock(gunStack);
    }

    public void onAttachmentChanged(ItemStack stack) {
        GunStackData.updateState(stack, tag -> tag.putBoolean("AttachmentChanged", true));
    }

    public int getBayonetBanzaiLevel(ItemStack gunStack) {
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
            if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
                int banzai = ModEnchantments.getLevel(attachmentStack, ModEnchantments.BANZAI); // Port note
                if (banzai > 0) {
                    return banzai;
                }
            }
        }
        return 0;
    }

    public Gun getGunProperties() {
        return this.gun;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (stack.is(ModTags.Items.MINING_GUN)) {
            return true;
        }
        return super.isBookEnchantable(stack, book);
    }

}