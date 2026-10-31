package top.kristeal.client.handler;

import net.minecraft.server.level.ServerLevel;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
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


public class MeleeAttackHandler {
    private static final float ENCHANTMENT_DAMAGE_SCALING_FACTOR = 0.70f;
    /** Damage a fully built-up charge adds on top of the stab's damage, before the Banzai enchantment. */
    private static final float BANZAI_FULL_CHARGE_DAMAGE = 10.0F;
    /** Each Banzai level adds this share of the charge hit's damage. */
    private static final float BANZAI_DAMAGE_PER_LEVEL = 0.15F;
    /**
     * Cooldown after a charge ends (a hit, or the player stopping it). Stabs and charges share one cooldown, the gun's
     * melee cooldown: a stab sets the gun's own melee cooldown, a charge sets this; neither can start while it runs.
     */
    public static final int BANZAI_COOLDOWN_TICKS = 40;
    private static final String MELEE_COOLDOWN_TAG = "MeleeCooldown";
    /** Blocks of running that fill the charge from empty. */
    private static final double BANZAI_FULL_CHARGE_DISTANCE = 8.0;
    private static final double BANZAI_AOE_RADIUS = 1.5;
    private static final double BANZAI_HIT_RADIUS = 2.5;
    /** Sprinting can drop for a tick or two (a step, a bump); the charge only ends after this many ticks without it. */
    private static final int BANZAI_SPRINT_GRACE_TICKS = 5;
    /** Charge hits only reach targets within 60 degrees of where the player looks. */
    private static final double BANZAI_CONE_COS = 0.5;

    /**
     * Server-side state of each running charge. The reference kept one static flag and item for the whole game, so
     * every player shared one charge, and the client read the server's copy.
     */
    private static final Map<UUID, BanzaiState> BANZAI_STATES = new HashMap<>();

    private static final class BanzaiState {
        private final int slot;
        private final Item item;
        private Vec3 lastPosition;
        private int ticksNotSprinting;
        /** Damage built up by running, 0 to 1; spent by a hit. */
        private double charge;

        private BanzaiState(int slot, Item item, Vec3 position) {
            this.slot = slot;
            this.item = item;
            this.lastPosition = position;
        }
    }

    /** Whether this player's bayonet charge is running. Synced, so it is valid on both sides. */
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
            // On the gun that charged, also if the player has switched away from it
            ItemStack gunStack = player.getInventory().getItem(state.slot);
            if (gunStack.getItem() == state.item) {
                long cooldownEnd = player.level().getGameTime() + BANZAI_COOLDOWN_TICKS;
                GunStackData.updateState(gunStack, tag -> tag.putLong(MELEE_COOLDOWN_TAG, cooldownEnd));
            }
        }
        if (wasRunning && player.isAlive()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    /** Forgets a charge without effects (logout, respawn). */
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

        // Interrupting a reload and leaving the inspect animation happen on the client (S2CMessageMeleeAttack); the
        // reference drove GeckoLib controllers and sent a C2S packet from here, on the server thread.
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
                CompoundTag tag = GunStackData.liveState(heldItem); // Port note
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
        CompoundTag tag = GunStackData.getState(heldItem); // Port note: read-only copy
        long currentTime = player.level().getGameTime();
        return tag.contains(MELEE_COOLDOWN_TAG) && currentTime < tag.getLong(MELEE_COOLDOWN_TAG);
    }

    public static void setMeleeCooldown(Player player, ItemStack heldItem, GunItem gunItem) {
        long currentTime = player.level().getGameTime();
        int cooldownTicks = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeCooldownTicks();
        GunStackData.updateState(heldItem, tag -> tag.putLong(MELEE_COOLDOWN_TAG, currentTime + cooldownTicks)); // Port note
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
        float banzaiMultiplier = 1.0F + BANZAI_DAMAGE_PER_LEVEL * gunItem.getBayonetBanzaiLevel(heldItem);
        DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
        boolean hitAny = false;
        for (LivingEntity target : targets) {
            // The full stab damage plus the built-up damage, then +15% per Banzai level.
            // The reference used the stab's damage times a speed factor that only the Banzai enchantment raised.
            float attackDamage = (getAttackDamage(player, heldItem, gunItem, target)
                    + (float) charge * BANZAI_FULL_CHARGE_DAMAGE) * banzaiMultiplier;
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
    /** The impact kick on the attacker's screen; the reference called the client renderer from here. */
    private static void sendImpact(ServerPlayer player, ItemStack heldItem) {
        if (((GunItem) heldItem.getItem()).hasBayonet(heldItem)) {
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeImpact());
        }
    }

    private static void applySpecialEnchantmentsFromBayonet(ItemStack gunStack, LivingEntity target, Player player, GunItem gunItem) {
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = gunItem.getAttachment(gunStack, type);
            if (attachmentStack.getItem() instanceof BayonetItem) {
                // Port note: 1.21 enchantments are holders in the stack's ENCHANTMENTS component
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
    /**
     * Runs a player's charge for one server tick (called from the player tick event). The reference ran this from a
     * timer thread every 50 ms.
     */
    public static void tickBanzai(ServerPlayer player) {
        BanzaiState state = BANZAI_STATES.get(player.getUUID());
        if (state == null) {
            resetSyncedBanzai(player);
            return;
        }

        // Player movement is client-driven, so getDeltaMovement() is about zero on the server; use the position change.
        Vec3 position = player.position();
        double distance = position.subtract(state.lastPosition).horizontalDistance();
        state.lastPosition = position;

        // Same slot and item, not an exact stack match: the gun's state data changes while it is held.
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!player.isAlive() || player.getInventory().selected != state.slot || heldItem.getItem() != state.item
                || !(heldItem.getItem() instanceof GunItem gunItem) || !gunItem.hasBayonet(heldItem)) {
            stopBanzai(player);
            return;
        }

        if (player.isSprinting()) {
            state.ticksNotSprinting = 0;
        } else if (++state.ticksNotSprinting > BANZAI_SPRINT_GRACE_TICKS) {
            stopBanzai(player);
            return;
        }

        if (player.isSprinting()) {
            state.charge = Math.min(1.0, state.charge + distance / BANZAI_FULL_CHARGE_DISTANCE);
            syncCharge(player, state);
        }

        // A hit spends the charge and ends it, which starts the shared cooldown
        if (!findTargetsInCone(player, BANZAI_AOE_RADIUS).isEmpty()) {
            hitBanzaiTargets(player, findTargetsInCone(player, BANZAI_HIT_RADIUS), state.charge);
            stopBanzai(player);
        }
    }

    /** The HUD bar reads the charge as a whole percentage; only changes are sent. */
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
                // Port note: 1.21 has no DamageEnchantment#getDamageBonus. modifyDamage with a base of 0 gives the summed
                // damage bonus of the bayonet's enchantments against this target (Sharpness, Smite, Bane, ...), scaled once.
                if (target.level() instanceof ServerLevel serverLevel) {
                    float damageBonus = EnchantmentHelper.modifyDamage(serverLevel, attachmentStack, target, target.damageSources().generic(), 0.0F);
                    enchantmentDamage += damageBonus * ENCHANTMENT_DAMAGE_SCALING_FACTOR;
                }
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : attachmentStack.getTagEnchantments().entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int level = entry.getIntValue();
                    if (enchantment.is(top.kristeal.init.ModEnchantments.CORRODED)) {
                        if (isBotEntity(target)) {
                            float corrodedBonus = top.kristeal.event.CorrodedEnchantmentHandler.getBotDamageBonus(level);
                            enchantmentDamage += corrodedBonus * ENCHANTMENT_DAMAGE_SCALING_FACTOR;
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
    /** Living targets within the radius and in front of the player (the reference took every direction). */
    private static List<LivingEntity> findTargetsInCone(Player player, double radius) {
        Vec3 position = player.position();
        AABB boundingBox = new AABB(position.subtract(radius, radius, radius), position.add(radius, radius, radius));
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();

        return player.level().getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> {
            if (entity == player || !entity.isAlive()) return false;
            Vec3 toTarget = entity.position().subtract(position).multiply(1, 0, 1);
            // A target standing inside the player counts as in front
            return toTarget.lengthSqr() < 1.0E-4 || look.dot(toTarget.normalize()) >= BANZAI_CONE_COS;
        });
    }
    private static void damageGunAndAttachments(ItemStack stack, Player player) {
        Level level = player.level();
        GunEventBus.damageGun(stack, level, player);
        GunEventBus.damageAttachments(stack, level, player);
    }
}


