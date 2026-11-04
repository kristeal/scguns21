package top.kristeal.client.handler;

import net.minecraft.server.level.ServerLevel;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import top.kristeal.Config;
import top.kristeal.common.GunStackData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;
import top.kristeal.common.Gun;
import top.kristeal.event.GunEventBus;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.BayonetItem;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.item.attachment.IAttachment;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.S2CMessageMeleeAttack;
import top.kristeal.network.message.S2CMessageMeleeImpact;
import top.kristeal.util.GunModifierHelper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * The Reworked, Server-authoritative banzai mechanic. Bayonet stabs and the sprinting Banzai charge.
 *
 * <p>Stabs and charges share one cooldown, stored on the gun stack under {@value #MELEE_COOLDOWN_TAG}.
 * A stab writes the gun's configured melee cooldown; a charge writes {@link #banzaiCooldownTicks()}.
 * Neither can start while that cooldown runs.
 *
 * <p>Charge state is kept per player in {@link #BANZAI_STATES}.
 * It is mirrored to clients via the synced keys {@code BANZAI} and
 * {@code BANZAI_CHARGE}, so {@link #isBanzaiActive} reads correctly on both sides.
 *
 * <p>All damage, knockback, enchantment effects, sounds and particles are applied here on the server;
 * the client only plays animations when it receives the S2C packets sent from this class.
 *
 * <p>Banzai is fully configurable (see {@link Config.Banzai}): the charge fills over {@link #banzaiFullChargeDistance()} blocks of sprinting and is
 * triggered by the first target inside {@link #banzaiAoeRadius()}; targets within {@link #banzaiHitRadius()}
 * take damage, but only inside a cone ahead ({@link #banzaiConeCos()}). A fully charged hit adds
 * {@link #banzaiFullChargeDamage()} on top of the stab damage, and each Banzai enchantment level adds
 * {@link #banzaiDamagePerLevel()} of that total. Enchantment damage from bayonet attachments is scaled
 * by {@link #enchantmentDamageScalingFactor()}.
 */
public class MeleeAttackHandler {
    private static final String MELEE_COOLDOWN_TAG = "MeleeCooldown";

    private static float enchantmentDamageScalingFactor() { return Config.COMMON.banzai.banzaiEnchantFactor.get().floatValue(); }
    private static float banzaiFullChargeDamage() { return Config.COMMON.banzai.banzaiFullDamage.get().floatValue(); }
    private static float banzaiDamagePerLevel() { return Config.COMMON.banzai.banzaiDamagePerLevel.get().floatValue(); }
    public static int banzaiCooldownTicks() { return Config.COMMON.banzai.banzaiCooldownTicks.get(); }
    private static double banzaiFullChargeDistance() { return Config.COMMON.banzai.banzaiFullChargeDistance.get(); }
    private static double banzaiAoeRadius() { return Config.COMMON.banzai.banzaiAoe.get(); }
    private static double banzaiHitRadius() { return Config.COMMON.banzai.banzaiHitRadius.get(); }
    private static int banzaiSprintGraceTicks() { return Config.COMMON.banzai.banzaiGraceTicks.get(); }
    private static double banzaiConeCos() { return Config.COMMON.banzai.banzaiConeCos.get(); }

    private static final Map<UUID, BanzaiState> BANZAI_STATES = new HashMap<>();

    private static final class BanzaiState {
        private final int slot;
        private final Item item;
        private Vec3 lastPosition;
        private int ticksNotSprinting;
        private double charge; // 0 to 1, built up by sprinting, spent by a hit

        private BanzaiState(int slot, Item item, Vec3 position) {
            this.slot = slot;
            this.item = item;
            this.lastPosition = position;
        }
    }

    /** True while this player's Banzai charge is running. Synced, so valid on both client and server. */
    public static boolean isBanzaiActive(Player player) {
        return ModSyncedDataKeys.BANZAI.getValue(player);
    }

    public static void startBanzai(ServerPlayer player) {
        if (BANZAI_STATES.containsKey(player.getUUID()) || !player.isSprinting()) {
            return;
        }
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldItem.getItem() instanceof GunItem gunItem) || !gunItem.hasBayonet(heldItem)
                || isMeleeOnCooldown(player, heldItem)) {
            return;
        }
        BANZAI_STATES.put(player.getUUID(), new BanzaiState(player.getInventory().selected, heldItem.getItem(), player.position()));
        ModSyncedDataKeys.BANZAI.setValue(player, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.6F, 1.3F);
    }

    public static void stopBanzai(ServerPlayer player) {
        BanzaiState state = BANZAI_STATES.remove(player.getUUID());
        boolean wasRunning = state != null;
        resetSyncedBanzai(player);
        if (wasRunning) {
            ItemStack gunStack = player.getInventory().getItem(state.slot);
            if (gunStack.getItem() == state.item) {
                long cooldownEnd = player.level().getGameTime() + banzaiCooldownTicks();
                GunStackData.updateState(gunStack, tag -> tag.putLong(MELEE_COOLDOWN_TAG, cooldownEnd));
            }
        }
        if (wasRunning && player.isAlive()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    /** Drops any running charge, used on logout and respawn. */
    public static void clearBanzai(ServerPlayer player) {
        BANZAI_STATES.remove(player.getUUID());
        resetSyncedBanzai(player);
    }

    private static void resetSyncedBanzai(ServerPlayer player) {
        if (isBanzaiActive(player)) {
            ModSyncedDataKeys.BANZAI.setValue(player, false);
        }
        if (ModSyncedDataKeys.BANZAI_CHARGE.getValue(player) != 0) {
            ModSyncedDataKeys.BANZAI_CHARGE.setValue(player, 0);
        }
    }

    public static void performMeleeAttack(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldItem.getItem() instanceof GunItem gunItem)) {
            return;
        }
        if (isMeleeOnCooldown(player, heldItem)) {
            return;
        }

        setMeleeCooldown(player, heldItem, gunItem);

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        if (heldItem.getItem() instanceof AnimatedGunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);

            if (gun.getGeneral().usesCustomMeleeAnimation()) {
                CompoundTag tag = GunStackData.liveState(heldItem); // mutable state, drives the GeckoLib animation
                tag.putBoolean("scguns:IsMelee", true);
                tag.putLong("MeleeStartTime", System.currentTimeMillis());
                ModSyncedDataKeys.MELEE.setValue(player, true);
            }
        }

        PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeAttack(heldItem));
        LivingEntity target = findTargetWithinReach(player, heldItem);
        if (target != null && target != player) {
            performStab(player);
            damageGunAndAttachments(heldItem, player);
        } else {
            HitResult hitResult = rayTraceBlocks(player, heldItem);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                BlockPos pos = blockHitResult.getBlockPos();
                BlockState blockState = player.level().getBlockState(pos);
                ClientboundLevelParticlesPacket particlePacket = getClientboundLevelParticlesPacket(blockHitResult, blockState);
                player.connection.send(particlePacket);
            }
        }
    }

    @NotNull
    private static ClientboundLevelParticlesPacket getClientboundLevelParticlesPacket(BlockHitResult blockHitResult, BlockState blockState) {
        Vec3 hitVec = blockHitResult.getLocation();
        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
        return new ClientboundLevelParticlesPacket(
                particleData,
                true,
                hitVec.x,
                hitVec.y,
                hitVec.z,
                0.0F,
                0.0F,
                0.0F,
                0.1F,
                10
        );
    }
    private static HitResult rayTraceBlocks(Player player, ItemStack heldItem) {
        GunItem gunItem = (GunItem)heldItem.getItem();
        float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();

        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle();
        Vec3 reachVector = eyePosition.add(lookVector.scale(reach));
        return player.level().clip(new ClipContext(
                eyePosition,
                reachVector,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }
    private static LivingEntity findTargetWithinReach(Player player, ItemStack heldItem) {
        GunItem gunItem = (GunItem)heldItem.getItem();
        float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();

        AABB boundingBox = player.getBoundingBox().inflate(reach, reach, reach);
        return player.level().getEntitiesOfClass(LivingEntity.class, boundingBox,
                        entity -> entity != player && entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }


    public static boolean isMeleeOnCooldown(Player player, ItemStack heldItem) {
        CompoundTag tag = GunStackData.getState(heldItem); // read-only snapshot
        long currentTime = player.level().getGameTime();
        return tag.contains(MELEE_COOLDOWN_TAG) && currentTime < tag.getLong(MELEE_COOLDOWN_TAG);
    }

    public static void setMeleeCooldown(Player player, ItemStack heldItem, GunItem gunItem) {
        long currentTime = player.level().getGameTime();
        int cooldownTicks = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeCooldownTicks();
        GunStackData.updateState(heldItem, tag -> tag.putLong(MELEE_COOLDOWN_TAG, currentTime + cooldownTicks));
    }
    private static float getAttackDamage(ServerPlayer player, ItemStack heldItem, GunItem gunItem, LivingEntity target) {
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float additionalDamage = GunModifierHelper.getAdditionalDamage(heldItem, true);
        if (gunItem.isBuiltInBayonetGun(heldItem)) {
            additionalDamage += GunItem.BUILT_IN_BAYONET_DAMAGE;
        }
        float enchantmentDamage = getEnchantmentDamageFromBayonet(heldItem, target, gunItem);
        float meleeDamage = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeDamage();
        return baseDamage + additionalDamage + enchantmentDamage + meleeDamage;
    }

    private static void performStab(ServerPlayer player) {
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldItem.getItem() instanceof GunItem gunItem)) {
            return;
        }
        LivingEntity target = raycastForMeleeAttack(player, heldItem);
        if (target == null) {
            return;
        }
        DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
        if (target.hurt(damageSource, getAttackDamage(player, heldItem, gunItem, target))) {
            spawnSuccessfulHitParticles(player, target);
            applyKnockback(player, target, heldItem);
            applySpecialEnchantmentsFromBayonet(heldItem, target, player, gunItem);
            sendImpact(player, heldItem);
        }
    }

    private static void hitBanzaiTargets(ServerPlayer player, List<LivingEntity> targets, double charge) {
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldItem.getItem() instanceof GunItem gunItem)) {
            return;
        }
        float banzaiMultiplier = 1.0F + banzaiDamagePerLevel() * gunItem.getBayonetBanzaiLevel(heldItem);
        DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
        boolean hitAny = false;
        for (LivingEntity target : targets) {
            // Stab damage plus the built-up charge bonus, then the Banzai multiplier, rounded to 2 decimals.
            float attackDamage = (getAttackDamage(player, heldItem, gunItem, target)
                    + (float) charge * banzaiFullChargeDamage()) * banzaiMultiplier;
            attackDamage = (float) (Math.round(attackDamage * 100.0) / 100.0);
            if (target.hurt(damageSource, attackDamage)) {
                hitAny = true;
                spawnSuccessfulHitParticles(player, target);
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
                applyKnockback(player, target, heldItem);
                applySpecialEnchantmentsFromBayonet(heldItem, target, player, gunItem);
            }
        }
        if (hitAny) {
            sendImpact(player, heldItem);
        }
    }

    private static void spawnSuccessfulHitParticles(ServerPlayer player, LivingEntity target) {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        ClientboundLevelParticlesPacket sweepPacket = new ClientboundLevelParticlesPacket(
                ParticleTypes.SWEEP_ATTACK,
                true,
                targetPos.x,
                targetPos.y,
                targetPos.z,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                1
        );
        player.connection.send(sweepPacket);

        for (int i = 0; i < 5; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;

            ClientboundLevelParticlesPacket critPacket = new ClientboundLevelParticlesPacket(
                    ParticleTypes.CRIT,
                    true,
                    targetPos.x + offsetX,
                    targetPos.y + offsetY,
                    targetPos.z + offsetZ,
                    0.0F,
                    0.1F,
                    0.0F,
                    0.1F,
                    1
            );
            player.connection.send(critPacket);
        }
    }
    /** Tells the attacker's client that a bayonet hit landed, so it plays the screen impact kick. */
    private static void sendImpact(ServerPlayer player, ItemStack heldItem) {
        if (((GunItem) heldItem.getItem()).hasBayonet(heldItem)) {
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeImpact());
        }
    }

    private static void applySpecialEnchantmentsFromBayonet(ItemStack gunStack, LivingEntity target, Player player, GunItem gunItem) {
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = gunItem.getAttachment(gunStack, type);
            if (attachmentStack.getItem() instanceof BayonetItem) {
                // 1.21 stores enchantments as holders in the stack's ENCHANTMENTS component.
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : attachmentStack.getTagEnchantments().entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int level = entry.getIntValue();
                    applyEnchantmentEffects(enchantment, level, target, player);
                    if (enchantment.is(top.kristeal.init.ModEnchantments.CORRODED)) {
                        applyCorrodedEffects(player, target, level);
                    }
                }
            }
        }
    }
    private static void applyCorrodedEffects(Player player, LivingEntity target, int level) {
        if (isBotEntity(target)) {
            spawnCorrodedParticles(player, target, level);
        } else {
            if (player.level().getRandom().nextFloat() < 0.30F) {
                int poisonDuration = 60 + (level * 20);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, poisonDuration, 0));
            }
        }
    }
    private static void spawnCorrodedParticles(Player player, LivingEntity target, int level) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < level * 5; i++) {
                double offsetX = (random.nextDouble() - 0.5) * target.getBbWidth();
                double offsetY = random.nextDouble() * target.getBbHeight();
                double offsetZ = (random.nextDouble() - 0.5) * target.getBbWidth();

                ClientboundLevelParticlesPacket particlePacket = new ClientboundLevelParticlesPacket(
                        ParticleTypes.ELECTRIC_SPARK,
                        true,
                        target.getX() + offsetX,
                        target.getY() + offsetY,
                        target.getZ() + offsetZ,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.1F,
                        1
                );

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(particlePacket);
                }
            }
        }
    }
    /** Advances one player's charge by one server tick. Called every player tick from the server event bus. */
    public static void tickBanzai(ServerPlayer player) {
        BanzaiState state = BANZAI_STATES.get(player.getUUID());
        if (state == null) {
            resetSyncedBanzai(player);
            return;
        }

        // Movement is client-driven, so getDeltaMovement() is ~0 on the server — measure position delta instead.
        Vec3 position = player.position();
        double distance = position.subtract(state.lastPosition).horizontalDistance();
        state.lastPosition = position;

        // Match on slot + item, not the exact stack: the gun's NBT changes while it is held.
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!player.isAlive() || player.getInventory().selected != state.slot || heldItem.getItem() != state.item
                || !(heldItem.getItem() instanceof GunItem gunItem) || !gunItem.hasBayonet(heldItem)) {
            stopBanzai(player);
            return;
        }

        if (player.isSprinting()) {
            state.ticksNotSprinting = 0;
        } else if (++state.ticksNotSprinting > banzaiSprintGraceTicks()) {
            stopBanzai(player);
            return;
        }

        if (player.isSprinting()) {
            state.charge = Math.min(1.0, state.charge + distance / banzaiFullChargeDistance());
            syncCharge(player, state);
        }

        // A target in range spends the charge, ends it, and starts the shared cooldown.
        if (!findTargetsInCone(player, banzaiAoeRadius()).isEmpty()) {
            hitBanzaiTargets(player, findTargetsInCone(player, banzaiHitRadius()), state.charge);
            stopBanzai(player);
        }
    }

    /** Pushes the charge to the client as a whole percentage, but only when it actually changed. */
    private static void syncCharge(ServerPlayer player, BanzaiState state) {
        int percent = (int) Math.round(state.charge * 100.0);
        if (ModSyncedDataKeys.BANZAI_CHARGE.getValue(player) != percent) {
            ModSyncedDataKeys.BANZAI_CHARGE.setValue(player, percent);
        }
    }

    private static float getEnchantmentDamageFromBayonet(ItemStack gunStack, LivingEntity target, GunItem gunItem) {
        float enchantmentDamage = 0.0f;
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = gunItem.getAttachment(gunStack, type);
            if (attachmentStack.getItem() instanceof BayonetItem) {
                // 1.21 has no DamageEnchantment#getDamageBonus: modifyDamage with base 0 returns the summed
                // bonus of the bayonet's enchantments against this target (Sharpness, Smite, Bane, ...).
                if (target.level() instanceof ServerLevel serverLevel) {
                    float damageBonus = EnchantmentHelper.modifyDamage(serverLevel, attachmentStack, target, target.damageSources().generic(), 0.0F);
                    enchantmentDamage += damageBonus * enchantmentDamageScalingFactor();
                }
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : attachmentStack.getTagEnchantments().entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int level = entry.getIntValue();
                    if (enchantment.is(top.kristeal.init.ModEnchantments.CORRODED)) {
                        if (isBotEntity(target)) {
                            float corrodedBonus = top.kristeal.event.CorrodedEnchantmentHandler.getBotDamageBonus(level);
                            enchantmentDamage += corrodedBonus * enchantmentDamageScalingFactor();
                        }
                    }
                }
            }
        }
        return enchantmentDamage;
    }
    private static boolean isBotEntity(LivingEntity entity) {
        return entity.getType().is(top.kristeal.init.ModTags.Entities.BOT);
    }
    private static void applyEnchantmentEffects(Holder<Enchantment> enchantment, int level, LivingEntity target, Player player) {
        if (enchantment.is(Enchantments.FIRE_ASPECT)) {
            target.igniteForSeconds(level * 4);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        } else if (enchantment.is(Enchantments.KNOCKBACK)) {
            Vec3 direction = target.position().subtract(player.position()).normalize();
            target.knockback(level * 0.5F, -direction.x(), -direction.z());
        } else if (enchantment.is(Enchantments.SMITE) && target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {
            spawnEnchantmentHitParticles(player, target);
        } else if (enchantment.is(Enchantments.BANE_OF_ARTHROPODS) && target.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) {
            spawnEnchantmentHitParticles(player, target);
        } else if (enchantment.is(Enchantments.SHARPNESS)) {
            spawnEnchantmentHitParticles(player, target);
        }
    }
    private static void spawnEnchantmentHitParticles(Player player, LivingEntity target) {
        if (player instanceof ServerPlayer serverPlayer) {
            ClientboundLevelParticlesPacket particlePacket = new ClientboundLevelParticlesPacket(
                    ParticleTypes.ENCHANTED_HIT,
                    true,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    target.getBbWidth() * 0.5F,
                    target.getBbHeight() * 0.25F,
                    target.getBbWidth() * 0.5F,
                    0.02F,
                    8
            );
            serverPlayer.connection.send(particlePacket);
        }
    }
    private static void applyKnockback(Player player, LivingEntity target, ItemStack stack) {
        int knockbackLevel = EnchantmentHelper.getItemEnchantmentLevel(player.registryAccess().holderOrThrow(Enchantments.KNOCKBACK), stack);
        Vec3 direction = target.position().subtract(player.position()).normalize();
        target.knockback(0.4F + (knockbackLevel * 0.5F), -direction.x(), -direction.z());
    }
    private static LivingEntity raycastForMeleeAttack(Player player, ItemStack heldItem) {
        GunItem gunItem = (GunItem)heldItem.getItem();
        float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();

        Vec3 startVec = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endVec = startVec.add(lookVec.scale(reach));
        AABB boundingBox = new AABB(startVec, endVec);

        return player.level().getEntitiesOfClass(LivingEntity.class, boundingBox,
                        entity -> entity != player && entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }
    /** All living entities within {@code radius} of the player and inside the forward look cone. */
    private static List<LivingEntity> findTargetsInCone(Player player, double radius) {
        Vec3 position = player.position();
        AABB boundingBox = new AABB(position.subtract(radius, radius, radius), position.add(radius, radius, radius));
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();

        return player.level().getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> {
            if (entity == player || !entity.isAlive()) return false;
            Vec3 toTarget = entity.position().subtract(position).multiply(1, 0, 1);
            // A target overlapping the player has no meaningful direction, so count it as in front.
            return toTarget.lengthSqr() < 1.0E-4 || look.dot(toTarget.normalize()) >= banzaiConeCos();
        });
    }
    private static void damageGunAndAttachments(ItemStack stack, Player player) {
        Level level = player.level();
        GunEventBus.damageGun(stack, level, player);
        GunEventBus.damageAttachments(stack, level, player);
    }
}
