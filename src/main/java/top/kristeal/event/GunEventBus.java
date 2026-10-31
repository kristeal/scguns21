package top.kristeal.event;

import top.kristeal.item.NonUnderwaterGunItem;
import top.kristeal.item.UnderwaterGunItem;

import top.kristeal.common.GunStackData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.ScorchedGuns;
import top.kristeal.cache.HotBarrelCache;
import top.kristeal.client.handler.MeleeAttackHandler;
import top.kristeal.common.*;
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.common.exosuit.ExoSuitUpgrade;
import top.kristeal.common.exosuit.ExoSuitUpgradeManager;
import top.kristeal.common.network.ServerPlayHandler;
import top.kristeal.init.*;
import top.kristeal.interfaces.IAirGun;
import top.kristeal.interfaces.IEnergyGun;
import top.kristeal.item.*;
import top.kristeal.item.ammo_boxes.EmptyCasingPouchItem;
import top.kristeal.item.animated.*;
import top.kristeal.item.attachment.IAttachment;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.C2SMessageReload;
import top.kristeal.network.message.S2CMessageHotBarrelSync;
import top.kristeal.util.AirSourceHelper;
import top.kristeal.util.GunCompositeStatHelper;
import top.theillusivec4.curios.api.CuriosApi;


@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunEventBus {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerLevel world : server.getAllLevels()) {
                    BeamHandlerCommon.BeamMiningManager.tickMiningProgress(world);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel world : server.getAllLevels()) {
            TemporaryLightManager.emergencyCleanup(world);
        }
    }

    @SubscribeEvent
    public static void preShoot(GunFireEvent.Pre event) {
        Player player = event.getEntity();
        Level level = event.getEntity().level();
        ItemStack heldItem = player.getMainHandItem();
        CompoundTag tag = GunStackData.getState(heldItem); // Port note: CUSTOM_DATA copy

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) MeleeAttackHandler.stopBanzai(serverPlayer);

        if (level.isClientSide() && heldItem.getItem() instanceof AnimatedGunItem animatedGunItem) {
            try {
                long id = GeoItem.getId(heldItem);
                AnimationController<GeoAnimatable> animationController = animatedGunItem.getAnimatableInstanceCache()
                        .getManagerForId(id)
                        .getAnimationControllers()
                        .get("controller");

                if (animationController != null) {
                    if (heldItem.isDamageableItem() &&
                            heldItem.getDamageValue() >= (heldItem.getMaxDamage() - 1)) {
                        player.displayClientMessage(Component.translatable("message.scguns.gun_broken")
                                .withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        return;
                    }

                    if (animatedGunItem.isAnimationPlaying(animationController, "reload_stop")) {
                        event.setCanceled(true);
                        return;
                    }

                    if (tag != null && tag.getBoolean("scguns:IsReloading")) {
                        if (animatedGunItem.isAnimationPlaying(animationController, "reload_loop") ||
                                animatedGunItem.isAnimationPlaying(animationController, "reload_start")) {
                            tag.putBoolean("scguns:ReloadComplete", true);
                            animationController.tryTriggerAnimation("reload_stop");
                            tag.remove("scguns:IsReloading");
                            GunStackData.setState(heldItem, tag);
                            ModSyncedDataKeys.RELOADING.setValue(player, false);
                            PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                        }
                        event.setCanceled(true);
                        return;
                    }
                }
            } catch (Exception e) {
                ScorchedGuns.LOGGER.error("Error in preShoot animation handling: " + e.getMessage());
            }
        }
        if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            GripType gripType = gun.determineGripType(heldItem);

            if (player.isUsingItem() && player.getOffhandItem().getItem() == Items.SHIELD
                    && (gripType == GripType.ONE_HANDED || gripType == GripType.ONE_HANDED_2)) {
                event.setCanceled(true);
                return;
            }
            if (tag.contains("DrawnTick") &&
                    tag.getInt("DrawnTick") < 15) {
                event.setCanceled(true);
                return;
            }

            int energyUse = gun.getProjectile().getEnergyUse();
            if (!player.isCreative()) {
                // Energy capability check (Forge orElseThrow -> a null capability throws the same way)
                if (heldItem.getItem() instanceof IEnergyGun) {
                    IEnergyStorage energyStorage = heldItem.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (energyStorage == null) {
                        throw new IllegalStateException();
                    }
                    if (energyStorage.getEnergyStored() >= energyUse) {
                        energyStorage.extractEnergy(energyUse, false);
                    } else {
                        player.displayClientMessage(Component.translatable("message.energy_gun.no_energy")
                                .withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        return;
                    }
                }

                // Air source check (backtank path guarded by ModCompat.isCreateLoaded())
                if (heldItem.getItem() instanceof IAirGun) {
                    float airCostPerShot = calculateAirCostPerShot(gun);
                    if (!AirSourceHelper.consumeAir(player, airCostPerShot)) {
                        AirSourceHelper.AirSource airSource = AirSourceHelper.getBestAirSource(player);

                        if (airSource.getType() == AirSourceHelper.AirSource.Type.NONE) {
                            if (ModCompat.isCreateLoaded()) {
                                player.displayClientMessage(Component.translatable("message.airgun.no_air_source")
                                        .withStyle(ChatFormatting.RED), true);
                            } else {
                                player.displayClientMessage(Component.translatable("message.airgun.requires_canister")
                                        .withStyle(ChatFormatting.RED), true);
                            }
                        } else {
                            player.displayClientMessage(Component.translatable("message.airgun.no_air")
                                    .withStyle(ChatFormatting.RED), true);
                        }
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            // NonUnderwaterAnimatedGunItem, and the non-animated NonUnderwaterGunItem (no reference gun uses it)
            if ((heldItem.getItem() instanceof NonUnderwaterGunItem || heldItem.getItem() instanceof NonUnderwaterAnimatedGunItem) &&
                    player.isUnderWater()) {
                event.setCanceled(true);
                return;
            }

            if (heldItem.isDamageableItem() && tag != null) {
                if (heldItem.getDamageValue() == (heldItem.getMaxDamage() - 1)) {
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.scguns.gun_broken")
                            .withStyle(ChatFormatting.RED), true);
                    event.getEntity().getCooldowns().addCooldown(event.getStack().getItem(),
                            gun.getGeneral().getRate());
                    event.setCanceled(true);
                    return;
                }
                int maxDamage = heldItem.getMaxDamage();
                int currentDamage = heldItem.getDamageValue();
                int gunRustLevel = ModEnchantments.getLevel(heldItem, ModEnchantments.GUN_RUST);

                double jamChance = 0.0;

                if (gunRustLevel > 0) {
                    jamChance = 0.10 + (gunRustLevel * 0.05);
                }

                if (currentDamage >= maxDamage * 0.8) {
                    jamChance += 0.025;
                }

                if (jamChance > 0 && Math.random() < jamChance) {
                    event.getEntity().playSound(ModSounds.ITEM_PISTOL_COCK.get(), 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.scguns.gun_jammed")
                            .withStyle(ChatFormatting.YELLOW), true);

                    int coolDown = gun.getGeneral().getRate() * 10;
                    if (coolDown > 30) {
                        coolDown = 30;
                    }
                    event.getEntity().getCooldowns().addCooldown(event.getStack().getItem(), coolDown);
                    event.setCanceled(true);
                    return;
                }
                if (GunStackData.getAmmoCount(heldItem) >= 1) {
                    broken(heldItem, level, player);
                }
            }
        }
    }
    @SubscribeEvent
    public static void postShoot(GunFireEvent.Post event) {
        Player player = event.getEntity();
        Level level = event.getEntity().level();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof AnimatedGunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            if (gun.getGeneral().isRevolver()) {
                ((AnimatedGunItem)heldItem.getItem()).getRotationHandler().incrementCylinderRotation(30.0f);
            }
            if (heldItem.getItem().toString().contains("cogloader")) {
                ((AnimatedGunItem)heldItem.getItem()).getRotationHandler().incrementMagazineRotation(15.0f);
            }
            if (heldItem.getItem().toString().contains("scrapper")) {
                float maxAmmo = Gun.getMaxAmmo(heldItem);
                float currentAmmo = Gun.getAmmoCount(heldItem);
                float slidePosition = Math.min((maxAmmo - currentAmmo) / maxAmmo, 1.0f);
                GunStackData.updateState(heldItem, t -> t.putFloat("MagazinePosition", slidePosition));
            }
            long id = GeoItem.getId(heldItem);
            AnimationController<GeoAnimatable> controller = gunItem.getAnimatableInstanceCache()
                    .getManagerForId(id)
                    .getAnimationControllers()
                    .get("controller");

            controller.forceAnimationReset();
            boolean isCarbine = gunItem.isInCarbineMode(heldItem);

            if (gunItem instanceof AnimatedDualWieldGunItem /* Batch 24 */) {
                ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.incrementShotCount();
                boolean useAlternate = ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.shouldUseAlternateAnimation();

                if (ModSyncedDataKeys.AIMING.getValue(player)) {
                    controller.tryTriggerAnimation(useAlternate ? "aim_shoot1" : "aim_shoot");
                } else {
                    controller.tryTriggerAnimation(useAlternate ? "shoot1" : "shoot");
                }
            } else {
                if (ModSyncedDataKeys.AIMING.getValue(player)) {
                    controller.tryTriggerAnimation(isCarbine ? "carbine_aim_shoot" : "aim_shoot");
                } else {
                    controller.tryTriggerAnimation(isCarbine ? "carbine_shoot" : "shoot");
                }
            }
        }
        if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            int hotBarrelLevel = ModEnchantments.getLevel(heldItem, ModEnchantments.HOT_BARREL);

            if (gun.getProjectile().hasPlayerKnockBack()) {
                applyGunKnockback(player, gun, GunCompositeStatHelper.getCompositeRate(heldItem, gun));
            }

            if (hotBarrelLevel > 0) {
                int hotBarrelFillRate = gun.getGeneral().getHotBarrelRate();
                HotBarrelCache.increaseHotBarrel(player, heldItem, hotBarrelFillRate);
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    int newLevel = HotBarrelCache.getHotBarrelLevel(player, heldItem);
                    PacketHandler.getPlayChannel().sendToPlayer(
                            () -> serverPlayer,
                            new S2CMessageHotBarrelSync(newLevel, heldItem.getItem().getDescriptionId())
                    );
                }
            }
            if (gun.getGeneral().isEnableGunLight()) {
                Vec3 lookVec = player.getLookAngle();
                BlockPos lightPos = player.blockPosition()
                        .offset((int)(lookVec.x * 2), 2, (int)(lookVec.z * 2));
                boolean isBeamWeapon = gun.getGeneral().getFireMode() == FireMode.BEAM;
                TemporaryLightManager.addTemporaryLight(level, lightPos, isBeamWeapon);
            }

            int shotCount = ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.getShotCount();
            boolean mirror = (heldItem.getItem() instanceof AnimatedDualWieldGunItem /* Batch 24 */ && (shotCount % 2 == 1));

            if (Config.COMMON.gameplay.spawnCasings.get()) {
                if (gun.getProjectile().ejectsCasing() && !gun.getProjectile().ejectDuringReload()) {
                    if (GunStackData.getAmmoCount(heldItem) >= 1 || player.getAbilities().instabuild) {
                        ejectCasing(level, player, mirror);
                    }
                }

            }

            if (heldItem.isDamageableItem()) {
                if (heldItem.getDamageValue() >= (heldItem.getMaxDamage() / 1.5) && Math.random() < 0.15) {
                    level.playSound(player, player.blockPosition(), ModSounds.COPPER_GUN_JAM.get(), SoundSource.PLAYERS, 1.0F, 1.0f);
                }
            }
        }
    }

    /**
     * Fix: the airborne "recoil jump" lift is capped at {@link #MAX_AIRBORNE_LIFT_PER_TICK} per tick of fire
     * interval, below gravity (0.08/tick). Fast guns (e.g. Earth's Corpse with a Bump Stock, a shot every tick) can slow a
     * fall but no longer sustain flight; slow, strong guns keep their full reference lift.
     */
    private static final double MAX_AIRBORNE_LIFT_PER_TICK = 0.07;

    private static void applyGunKnockback(Player player, Gun gun, int fireRate) {
        Vec3 lookVec = player.getLookAngle();
        float baseStrength = gun.getProjectile().getPlayerKnockBackStrength();
        float totalKnockbackResistance = 0.0F;
        totalKnockbackResistance += (float) player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5f;

        for (ItemStack armorPiece : player.getArmorSlots()) {
            if (armorPiece.getItem() instanceof ArmorItem armor) {
                ArmorMaterial material = armor.getMaterial().value(); // Port note: 1.21 armour materials are registry holders (records)
                totalKnockbackResistance += material.knockbackResistance() * 0.25f;
            }
        }
        totalKnockbackResistance = Math.min(0.75F, totalKnockbackResistance);
        float effectiveStrength = baseStrength * (1.0F - totalKnockbackResistance);
        if (effectiveStrength > 0) {
            double verticalBoost;
            if (lookVec.y < -0.5 && !player.onGround() && player.getDeltaMovement().y > 0) {
                verticalBoost = Math.min(effectiveStrength * 1.25, MAX_AIRBORNE_LIFT_PER_TICK * Math.max(1, fireRate));
            } else {
                verticalBoost = 0.1 * effectiveStrength;
            }

            player.setDeltaMovement(player.getDeltaMovement().add(
                    -lookVec.x * effectiveStrength,
                    verticalBoost,
                    -lookVec.z * effectiveStrength
            ));

            if (verticalBoost > 0.5) {
                player.fallDistance = 0;
            }

            if (player instanceof ServerPlayer) {
                ((ServerPlayer) player).connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
    }
    // Port note: the reference handler had no phase check, so it ran at both START and END of every player tick.
    // 1.21 splits the phases into two events; both delegate here to keep that.
    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        onPlayerTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        onPlayerTick(event.getEntity());
    }

    private static void onPlayerTick(Player player) {
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof GunItem &&
                ModEnchantments.getLevel(heldItem, ModEnchantments.HOT_BARREL) > 0) {
            int levelBefore = HotBarrelCache.getHotBarrelLevel(player, heldItem);
            if (levelBefore > 0) {
            }
            HotBarrelCache.tickHotBarrel(player, heldItem);

        } else {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack itemStack = player.getInventory().getItem(i);
                if (itemStack.getItem() instanceof GunItem &&
                        ModEnchantments.getLevel(itemStack, ModEnchantments.HOT_BARREL) > 0) {

                    if (HotBarrelCache.getHotBarrelLevel(player, itemStack) > 0) {
                        HotBarrelCache.clearHotBarrel(player, itemStack);

                    }
                }
            }
        }
        if (player.tickCount % 1200 == 0) {
            HotBarrelCache.cleanupOldEntries();
        }
    }
    private static float calculateAirCostPerShot(Gun gun) {
        return gun.getProjectile().getEnergyUse();
    }

    public static void broken(ItemStack stack, Level level, Player player) {
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        if (currentDamage >= (maxDamage - 2)) {
            level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
    public static void damageGun(ItemStack stack, Level level, Player player) {
        if (!player.getAbilities().instabuild && Config.COMMON.gameplay.enableGunDamage.get()) {
            if (stack.isDamageableItem()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamageValue();
                boolean isUnderwater = player.isUnderWater();
                // The animated underwater guns, and the non-animated UnderwaterGunItem (unused by the reference guns)
                boolean isUnderwaterGun = stack.getItem() instanceof UnderwaterGunItem
                        || stack.getItem() instanceof AnimatedUnderWaterGunItem
                        || stack.getItem() instanceof AnimatedDiamondSteelUnderWaterGunItem;

                int damageAmount = 1;
                if (stack.getItem() instanceof GunItem gunItem) {
                    Gun gun = gunItem.getModifiedGun(stack);
                    damageAmount = gun.getProjectile(stack).getDurabilityDamage();
                }

                int waterProofLevel = ModEnchantments.getLevel(stack, ModEnchantments.WATER_PROOF);
                int acceleratorLevel = ModEnchantments.getLevel(stack, ModEnchantments.ACCELERATOR);

                if (isUnderwater) {
                    if (isUnderwaterGun) {
                        if (waterProofLevel > 0) {
                            if (Math.random() < 0.25) {
                                damageAmount = 0;
                            }
                        }
                    } else {
                        if (waterProofLevel > 0) {
                        } else {
                            damageAmount = 3;
                        }
                    }
                }

                if (acceleratorLevel > 0 && damageAmount > 0) {
                    float catastrophicChance = 0.07f * acceleratorLevel;
                    if (Math.random() < catastrophicChance) {
                        damageAmount *= 8;
                    }
                    else {
                        float extraWearChance = 0.25f * acceleratorLevel;
                        if (Math.random() < extraWearChance) {
                            damageAmount *= 2;
                        }
                    }
                }

                if (stack.getItem() instanceof GunItem gunItem) {
                    ItemStack stockStack = Gun.getAttachment(IAttachment.Type.STOCK, stack);
                    if (!stockStack.isEmpty() && stockStack.getItem() instanceof IAttachment<?> attachment) {
                        for (top.kristeal.interfaces.IGunModifier modifier : attachment.getProperties().getModifiers()) {
                            if (modifier == GunModifiers.BUMP_STOCK_MODIFIER && damageAmount > 0) {
                                if (Math.random() < 0.30) {
                                    damageAmount *= 2;
                                }
                                break;
                            }
                        }
                    }
                }

                if (currentDamage >= (maxDamage - damageAmount)) {
                    if (currentDamage >= (maxDamage - damageAmount - 1)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                } else {
                    stack.hurtAndBreak(damageAmount, player, null);
                }
            }
        }
    }


    /**
     * Wears every equipped attachment by one point (shots and melee hits). Attachments are stored in the immutable
     * attachments component, so each one is damaged as a copy and written back; one that would break is removed with
     * the item-break sound instead, as in the reference.
     */
    public static void damageAttachments(ItemStack stack, Level level, Player player) {
        if (player.getAbilities().instabuild || !Config.COMMON.gameplay.enableAttachmentDamage.get()) {
            return;
        }
        if (!(stack.getItem() instanceof GunItem) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (IAttachment.Type type : IAttachment.Type.values()) {
            if (!Gun.hasAttachmentEquipped(stack, type)) {
                continue;
            }
            ItemStack attachment = Gun.getAttachment(type, stack);
            if (!attachment.isDamageableItem()) {
                continue;
            }
            if (attachment.getDamageValue() >= attachment.getMaxDamage() - 1) {
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                Gun.removeAttachment(stack, type.getTagKey());
                continue;
            }
            // Unbreaking applies; the break callback never fires because the last point is handled above
            attachment.hurtAndBreak(1, serverLevel, player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null, item -> {});
            GunStackData.setAttachment(stack, type.getTagKey(), attachment);
        }
    }

    public static void ejectCasing(Level level, LivingEntity livingEntity, boolean mirror) {
        if (!level.isClientSide()) return;

        if (!(livingEntity instanceof Player playerEntity)) {
            return;
        }

        ItemStack heldItem = playerEntity.getMainHandItem();
        Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);

        Vec3 lookVec = playerEntity.getLookAngle();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 forwardVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        double offsetX = (mirror ? -rightVec.x : rightVec.x) * 0.5 + forwardVec.x * 0.5;
        double offsetY = playerEntity.getEyeHeight() - 0.4;
        double offsetZ = (mirror ? -rightVec.z : rightVec.z) * 0.5 + forwardVec.z * 0.5;

        Vec3 particlePos = playerEntity.getPosition(1).add(offsetX, offsetY, offsetZ);
        ResourceLocation particleLocation = gun.getProjectile().getCasingParticle();

        if (particleLocation != null) {
            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleLocation);
            if (particleType instanceof SimpleParticleType simpleParticleType) {
                level.addParticle(simpleParticleType,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0, 0);
            }
        }
    }



    public static void spawnCasingInWorld(Level level, Player player, ItemStack casingStack) {
        ItemEntity casingEntity = new ItemEntity(level, player.getX(), player.getY() + 1.5, player.getZ(), casingStack);
        casingEntity.setPickUpDelay(40);
        casingEntity.setDeltaMovement(0, 0.2, 0);
        level.addFreshEntity(casingEntity);
    }

    public static boolean addCasingToPouch(Player player, ItemStack casingStack) {
        ItemStack casingCopy = casingStack.copy();

        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof EmptyCasingPouchItem) {
                int insertedItems = EmptyCasingPouchItem.add(itemStack, casingCopy);
                if (insertedItems > 0) {
                    return true;
                }
            }
        }
        if (addCasingToExoSuitPouches(player, casingCopy)) {
            return true;
        }
        final boolean[] result = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof EmptyCasingPouchItem) {
                    int insertedItems = EmptyCasingPouchItem.add(stack, casingCopy);
                    if (insertedItems > 0) {
                        result[0] = true;
                        return;
                    }
                }
            }
        });

        return result[0];
    }

    public static boolean addCasingDirectly(Player player, ItemStack casingStack) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof EmptyCasingPouchItem) {
                int insertedItems = EmptyCasingPouchItem.add(itemStack, casingStack);
                if (insertedItems > 0) {
                    return true;
                }
            }
        }

        if (addCasingToExoSuitPouches(player, casingStack)) {
            return true;
        }

        final boolean[] result = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof EmptyCasingPouchItem) {
                    int insertedItems = EmptyCasingPouchItem.add(stack, casingStack);
                    if (insertedItems > 0) {
                        result[0] = true;
                        return;
                    }
                }
            }
        });

        if (result[0]) {
            return true;
        }

        return player.getInventory().add(casingStack);
    }
    private static boolean addCasingToExoSuitPouches(Player player, ItemStack casingStack) {
        ItemStack chestplate = getEquippedChestplate(player);
        if (chestplate.isEmpty()) {
            return false;
        }

        ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
        if (pouchUpgrade.isEmpty()) {
            return false;
        }

        ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
        if (upgrade == null) {
            return false;
        }

        String pouchId = getPouchId(pouchUpgrade);
        ItemStackHandler pouchInventory = getPouchInventory(chestplate, pouchId, upgrade.getDisplay().getStorageSize());

        for (int i = 0; i < pouchInventory.getSlots(); i++) {
            ItemStack stack = pouchInventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof EmptyCasingPouchItem) {
                int insertedItems = EmptyCasingPouchItem.add(stack, casingStack);
                if (insertedItems > 0) {
                    savePouchInventory(chestplate, pouchId, pouchInventory);
                    return true;
                }
            }
        }

        return false;
    }
    private static ItemStack getEquippedChestplate(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem exosuit &&
                    exosuit.getType() == net.minecraft.world.item.ArmorItem.Type.CHESTPLATE) {
                return armorStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findPouchUpgrade(ItemStack chestplate) {
        for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
            if (!upgradeItem.isEmpty()) {
                ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                if (upgrade != null && upgrade.getType().equals("pouches")) {
                    return upgradeItem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static String getPouchId(ItemStack pouchUpgrade) {
        return pouchUpgrade.getItem().toString();
    }

    private static ItemStackHandler getPouchInventory(ItemStack chestplate, String pouchId, int size) {
        // Port note: same 1.21 port as ExoSuitAmmoHelper (ExoSuitData custom tag, built-in registry access)
        CompoundTag pouchData = ExoSuitData.getCustomTag(chestplate).getCompound("PouchData");

        ItemStackHandler handler = new ItemStackHandler(size);
        if (pouchData.contains(pouchId)) {
            handler.deserializeNBT(ExoSuitData.REGISTRY_ACCESS, pouchData.getCompound(pouchId));
        }

        return handler;
    }

    private static void savePouchInventory(ItemStack chestplate, String pouchId, ItemStackHandler handler) {
        CompoundTag tag = ExoSuitData.getCustomTag(chestplate);
        CompoundTag pouchData = tag.getCompound("PouchData");
        pouchData.put(pouchId, handler.serializeNBT(ExoSuitData.REGISTRY_ACCESS));
        tag.put("PouchData", pouchData);
        ExoSuitData.setCustomTag(chestplate, tag);
    }
}