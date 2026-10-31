package top.kristeal.entity.projectile;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import top.kristeal.common.GunStackData;
import net.minecraft.core.registries.BuiltInRegistries;
import top.kristeal.ScorchedGuns;
import top.kristeal.attributes.SCAttributes;
import top.kristeal.block.NitroKegBlock;
import top.kristeal.block.PowderKegBlock;
import top.kristeal.cache.HotBarrelCache;
import top.kristeal.common.*;
import top.kristeal.common.Gun.Projectile;
import top.kristeal.Config;
import top.kristeal.effect.RocketExplosion;
import top.kristeal.compat.SableCompat;
import top.kristeal.config.ProjectileAdvantageConfig;
import top.kristeal.init.*;
import top.kristeal.event.GunProjectileHitEvent;
import top.kristeal.interfaces.IDamageable;
import top.kristeal.interfaces.IExplosionDamageable;
import top.kristeal.interfaces.IHeadshotBox;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedDiamondSteelAirGunItem;
import top.kristeal.item.animated.AnimatedDiamondSteelGunItem;
import top.kristeal.item.animated.AnimatedDiamondSteelUnderWaterGunItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.S2CMessageBlood;
import top.kristeal.network.message.S2CMessageProjectileHitBlock;
import top.kristeal.network.message.S2CMessageProjectileHitEntity;
import top.kristeal.network.message.S2CMessageRemoveProjectile;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;
import top.kristeal.util.ReflectionUtil;
import top.kristeal.util.math.ExtendedEntityRayTraceResult;
import top.kristeal.world.ProjectileExplosion;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ProjectileEntity extends Entity implements IEntityWithComplexSpawn { // Port note: IEntityAdditionalSpawnData -> IEntityWithComplexSpawn
    static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
    public static final Predicate<BlockState> IGNORE_LEAVES = input -> input != null && Config.COMMON.gameplay.ignoreLeaves.get() && input.getBlock() instanceof LeavesBlock;

    protected int shooterId;
    protected LivingEntity shooter;
    protected Gun modifiedGun;
    protected Gun.General general;
    protected Gun.Projectile projectile;
    private ItemStack weapon = ItemStack.EMPTY;
    private ItemStack item = ItemStack.EMPTY;
    protected float additionalDamage = 0.0F;
    protected float attributeAdditionalDamage = 0.0F;
    protected double attributeDamageMultiplier = 0.0;
    protected EntityDimensions entitySize;
    protected double modifiedGravity;
    protected int life;
    private int soundTime = 0;
    private float chargeProgress;
    protected float armorBypassAmount = 2.0F;
    private float modifiedKnockback;
    private double distanceTraveled = 0.0;
    private long fireTime = -1L;
    public ProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
        super(entityType, worldIn);
    }

    public ProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
        this(entityType, worldIn);
        this.shooterId = shooter.getId();
        this.shooter = shooter;
        this.fireTime = worldIn.getGameTime();
        this.modifiedGun = modifiedGun;
        this.general = modifiedGun.getGeneral();
        this.projectile = modifiedGun.getProjectile(weapon);
        if (shooter instanceof ServerPlayer player) {
            this.chargeProgress = player.getPersistentData().getFloat("ChargeProgress");
        } else if (shooter instanceof Player player) {
            this.chargeProgress = ChargeHandler.getChargeProgress(player, weapon);
        } else {
            this.chargeProgress = 0f;
        }
        if (shooter instanceof Player player) ChargeHandler.clearLastChargeProgress(player.getUUID());

        float baseArmorBypass = this.projectile.getArmorPen();
        float puncturingBypass = GunEnchantmentHelper.getPuncturingArmorBypass(weapon);
        this.setArmorBypassAmount(baseArmorBypass + puncturingBypass);
        float baseKnockback = this.projectile.getKnockbackStrength();
        this.modifiedKnockback = GunEnchantmentHelper.getHeavyShotKnockback(weapon, baseKnockback);
        AttributeInstance additionalDamageAttr = shooter.getAttribute(SCAttributes.ADDITIONAL_BULLET_DAMAGE);
        this.attributeAdditionalDamage = additionalDamageAttr != null ? (float) additionalDamageAttr.getValue() : 0.0F;

        AttributeInstance damageMultAttr = shooter.getAttribute(SCAttributes.BULLET_DAMAGE_MULTIPLIER);
        this.attributeDamageMultiplier = damageMultAttr != null ? damageMultAttr.getValue() : 1.0;

        this.entitySize = EntityDimensions.scalable(this.projectile.getSize(), this.projectile.getSize());
        this.modifiedGravity = modifiedGun.getProjectile().isGravity() ? GunModifierHelper.getModifiedProjectileGravity(weapon, -0.04) : 0.0;
        this.life = GunModifierHelper.getModifiedProjectileLife(weapon, this.projectile.getLife());

        /* Get speed and set motion */
        Vec3 dir = this.getDirection(shooter, weapon, item, modifiedGun);
        double speedModifier = GunEnchantmentHelper.getProjectileSpeedModifier(weapon);
        double speed = GunModifierHelper.getModifiedProjectileSpeed(weapon, this.projectile.getSpeed() * speedModifier);
        if (modifiedGun.getGeneral().getFireMode() == FireMode.PULSE) {
            float chargeSpeedMultiplier = calculateChargeSpeedMultiplier(this.chargeProgress);
            speed *= chargeSpeedMultiplier;
        }

        AttributeInstance speedAttr = shooter.getAttribute(SCAttributes.PROJECTILE_SPEED);
        speed *= speedAttr != null ? speedAttr.getValue() : 1.0;
        this.setDeltaMovement(dir.x * speed, dir.y * speed, dir.z * speed);
        this.updateHeading();

        /* Spawn the projectile half way between the previous and current position */
        double posX = shooter.xOld + (shooter.getX() - shooter.xOld) / 2.0;
        double posY = shooter.yOld + (shooter.getY() - shooter.yOld) / 2.0 + shooter.getEyeHeight();
        double posZ = shooter.zOld + (shooter.getZ() - shooter.zOld) / 2.0;
        this.setPos(posX, posY, posZ);

        Item ammo = this.projectile.getItem();
        if (ammo != null) {
            int customModelData = -1;
            // Port note: a custom gun's "Model" is in the GUN_OVERRIDE compound; 1.21 custom model data is a component
            net.minecraft.world.item.component.CustomData override = GunStackData.getGunOverride(weapon);
            if (override != null && override.copyTag().contains("Model", Tag.TAG_COMPOUND)) {
                ItemStack model = ItemStack.parseOptional(this.registryAccess(), override.copyTag().getCompound("Model"));
                if (model.has(DataComponents.CUSTOM_MODEL_DATA)) {
                    customModelData = model.get(DataComponents.CUSTOM_MODEL_DATA).value();
                }
            }
            ItemStack ammoStack = new ItemStack(ammo);
            if (customModelData != -1) {
                ammoStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
            }
            this.item = ammoStack;
        }
    }
    public float getModifiedKnockback() {
        return this.modifiedKnockback;
    }
    private float calculateChargeSpeedMultiplier(float chargeProgress) {
        chargeProgress = Mth.clamp(chargeProgress, 0.0f, 1.0f);

        float minChargeSpeedMultiplier = 0.4f;
        float maxChargeSpeedMultiplier = 1.0f;

        return minChargeSpeedMultiplier + (maxChargeSpeedMultiplier - minChargeSpeedMultiplier) * chargeProgress;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
    public void setArmorBypassAmount(float amount) {
        this.armorBypassAmount = amount;
    }
    protected float calculateArmorBypassDamage(LivingEntity target, float damage) {
        int armorValue = target.getArmorValue();

        float baseReduction = Math.min(0.75f, armorValue * 0.004f);

        if (armorBypassAmount <= 0) {
            return damage * (1.0f - baseReduction);
        }

        float bypassPercent = armorBypassAmount / 10.0f;
        float effectiveArmor = armorValue * (1.0f - bypassPercent);
        float finalReduction = Math.min(0.75f, effectiveArmor * 0.004f);

        return damage * (1.0f - finalReduction);
    }
    public float getDamage() {
        float damage = getaFloat();
        damage = GunModifierHelper.getModifiedDamage(this.weapon, this.modifiedGun, damage);
        damage = GunEnchantmentHelper.getAcceleratorDamage(this.weapon, damage);
        damage = GunEnchantmentHelper.getHeavyShotDamage(this.weapon, damage);

        if (this.shooter instanceof Player player) {
            damage = GunEnchantmentHelper.getHotBarrelDamage(player, this.weapon, damage);
        }

        damage = GunEnchantmentHelper.getChargeDamage(this.weapon, damage, this.chargeProgress);

        damage *= Config.COMMON.gameplay.globalDamageMultiplier.get().floatValue();
        if (this.getPersistentData().contains("AIDamageScale")) {
            float scale = this.getPersistentData().getFloat("AIDamageScale");
            damage *= scale;
        }
        return Math.max(0F, damage);
    }
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.entitySize;
    }

    private Vec3 getDirection(LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {

        float baseSpread = modifiedGun.getProjectile().getSpread();

        float gunSpread;
        if (shooter instanceof Player player) {
            gunSpread = GunModifierHelper.getModifiedSpread(player, weapon, baseSpread);
        } else {
            gunSpread = GunModifierHelper.getModifiedSpread(weapon, baseSpread);
        }

        if (modifiedGun.getGeneral().getFireMode() == FireMode.PULSE) {
            float chargeSpreadMultiplier = calculateChargeSpreadMultiplier(this.chargeProgress);
            gunSpread *= chargeSpreadMultiplier;
        }

        if (gunSpread == 0F) {
            return getVectorFromRotation(shooter.getXRot(), shooter.getYRot());
        }

        if (shooter instanceof Player player) {
            if (!modifiedGun.getProjectile().isAlwaysSpread()) {
                float spreadTrackerMultiplier = SpreadTracker.get(player).getSpread(item);
                gunSpread *= spreadTrackerMultiplier;
            }
            if (ModSyncedDataKeys.AIMING.getValue(player)) {
                gunSpread *= 0.7F;
            }
        }

        AttributeInstance spreadAttr = shooter.getAttribute(SCAttributes.SPREAD_MULTIPLIER);
        float attrMultiplier = spreadAttr != null ? (float) spreadAttr.getValue() : 1.0F;
        gunSpread *= attrMultiplier;
        float spreadRadians = gunSpread * 0.017453292F;
        float angleY = random.nextFloat() * 2 * (float)Math.PI;
        float angleX = random.nextFloat() * spreadRadians;

        Vec3 forward = getVectorFromRotation(shooter.getXRot(), shooter.getYRot());
        Vec3 right;
        if (Math.abs(forward.y) < 0.999) {
            right = new Vec3(0, 1, 0).cross(forward).normalize();
        } else {
            right = new Vec3(1, 0, 0);
        }

        Vec3 up = forward.cross(right).normalize();
        Vec3 spreadVector = forward;
        spreadVector = rotateVector(spreadVector, right, angleX);
        spreadVector = rotateVector(spreadVector, forward, angleY);

        return spreadVector.normalize();
    }

    private float calculateChargeSpreadMultiplier(float chargeProgress) {
        chargeProgress = Mth.clamp(chargeProgress, 0.0f, 1.0f);

        float minChargeSpreadMultiplier = 2.0f;
        float maxChargeSpreadMultiplier = 0.3f;

        return minChargeSpreadMultiplier - (minChargeSpreadMultiplier - maxChargeSpreadMultiplier) * (chargeProgress * chargeProgress);
    }

    private Vec3 rotateVector(Vec3 vector, Vec3 axis, float angle) {
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        float dot = (float) vector.dot(axis);

        return new Vec3(
                vector.x * cos + (axis.y * vector.z - axis.z * vector.y) * sin + axis.x * dot * (1 - cos),
                vector.y * cos + (axis.z * vector.x - axis.x * vector.z) * sin + axis.y * dot * (1 - cos),
                vector.z * cos + (axis.x * vector.y - axis.y * vector.x) * sin + axis.z * dot * (1 - cos)
        );
    }
    public float getaFloat() {
        float initialDamage = (this.projectile.getDamage() + this.additionalDamage + this.attributeAdditionalDamage);
        initialDamage *= (float) this.attributeDamageMultiplier;

        if (this.projectile.isDamageReduceOverLife()) {
            float modifier = ((float) this.projectile.getLife() - (float) (this.tickCount - 1)) / (float) this.projectile.getLife();
            initialDamage *= modifier;
        }

        if (this.projectile.getDamageFalloffStart() > 0 && this.projectile.getDamageFalloffEnd() > this.projectile.getDamageFalloffStart()) {
            float falloffStart = this.projectile.getDamageFalloffStart();
            float falloffEnd = this.projectile.getDamageFalloffEnd();
            float minMultiplier = this.projectile.getDamageFalloffMinMultiplier();

            if (!this.weapon.isEmpty()) {
                falloffStart = GunModifierHelper.getModifiedDamageFalloffStart(this.weapon, falloffStart);
                falloffEnd = GunModifierHelper.getModifiedDamageFalloffEnd(this.weapon, falloffEnd);
            }

            if (this.distanceTraveled > falloffStart) {
                if (this.distanceTraveled >= falloffEnd) {
                    initialDamage *= minMultiplier;
                } else {
                    float falloffProgress = (float) ((this.distanceTraveled - falloffStart) / (falloffEnd - falloffStart));
                    float damageMultiplier = 1.0F - (falloffProgress * (1.0F - minMultiplier));
                    initialDamage *= damageMultiplier;
                }
            }
        }

        return initialDamage / this.projectile.getProjectileAmount();
    }

    public void setWeapon(ItemStack weapon) {
        this.weapon = weapon.copy();
    }

    public ItemStack getWeapon() {
        return this.weapon;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setAdditionalDamage(float additionalDamage) {
        this.additionalDamage = additionalDamage;
    }

    public double getModifiedGravity() {
        return this.modifiedGravity;
    }

    public void tick() {
        super.tick();
        this.updateHeading();
        this.onProjectileTick();

        // Track distance traveled for falloff calculation
        if (this.tickCount > 0) {
            Vec3 deltaMovement = this.getDeltaMovement();
            this.distanceTraveled += deltaMovement.length();
        }

        if (!this.level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            BlockHitResult fluidResult = this.level().clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));

            AABB range = new AABB(startVec.x-5, startVec.y-5, startVec.z-5, startVec.x+5, startVec.y+5, startVec.z+5);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, range);

            ResourceLocation flybySound = modifiedGun.getSounds().getFlybySound();

            if (!players.isEmpty() && flybySound != null && modifiedGun.getProjectile().getProjectileAmount() == 1 && this.tickCount > 3 && soundTime < this.tickCount - 3) {
                this.level().playSound(null, startVec.x,startVec.y,startVec.z, Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.get(flybySound)), SoundSource.NEUTRAL, (float) 0.5F + this.level().getRandom().nextFloat() * 0.4F, 0.8F + this.level().getRandom().nextFloat() * 0.4F);
                this.soundTime = this.tickCount;
            }

            if (fluidResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = fluidResult.getBlockPos();
                BlockState blockState = this.level().getBlockState(blockPos);
                FluidState fluidState = blockState.getFluidState();

                Vec3 impactPos = SableCompat.toWorld(this.level(), fluidResult.getLocation());
                if (fluidState.is(FluidTags.WATER)) {
                    this.onWaterImpact(impactPos);
                } else if (fluidState.is(FluidTags.LAVA)) {
                    this.onLavaImpact(impactPos);
                }
            }

            HitResult result = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), IGNORE_LEAVES);
            if (result.getType() != HitResult.Type.MISS) {
                endVec = result.getLocation();
            }

            List<EntityResult> hitEntities = null;
            int level = ModEnchantments.getLevel(this.weapon, ModEnchantments.COLLATERAL);
            if (level == 0) {
                EntityResult entityResult = this.findEntityOnPath(startVec, endVec);
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = this.findEntitiesOnPath(startVec, endVec);
            }

            if (hitEntities != null && !hitEntities.isEmpty()) {
                for (EntityResult entityResult : hitEntities) {
                    result = new ExtendedEntityRayTraceResult(entityResult);
                    if (((EntityHitResult) result).getEntity() instanceof Player player) {
                        if (this.shooter instanceof Player && !((Player) this.shooter).canHarmPlayer(player)) {
                            result = null;
                        }
                    }
                    if (result != null) {
                        this.onHit(result, startVec, endVec);
                    }
                }
            } else {
                this.onHit(result, startVec, endVec);
            }
        }

        double nextPosX = this.getX() + this.getDeltaMovement().x();
        double nextPosY = this.getY() + this.getDeltaMovement().y();
        double nextPosZ = this.getZ() + this.getDeltaMovement().z();
        this.setPos(nextPosX, nextPosY, nextPosZ);

        if (this.projectile.isGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, this.modifiedGravity, 0));
        }

        if (this.tickCount >= this.life) {
            if (this.isAlive()) {
                this.onExpired();
            }
            this.remove(RemovalReason.KILLED);
        }
    }

    /**
     * A simple method to perform logic on each tick of the projectile. This method is appropriate
     * for spawning particles. Override {@link #tick()} to make changes to physics
     */
    protected void onProjectileTick() {
    }

    /**
     * Called when the projectile has run out of its life. In other words, the projectile managed
     * to not hit any blocks and instead aged. The grenade uses this to explode in the air.
     */
    protected void onExpired() {
    }

    @Nullable
    protected EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
        Vec3 hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!entity.equals(this.shooter)) {
                EntityResult result = this.getHitResult(entity, startVec, endVec);
                if (result == null)
                    continue;
                Vec3 hitPos = result.getHitPos();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (distanceToHit < closestDistance) {
                    hitVec = hitPos;
                    hitEntity = entity;
                    closestDistance = distanceToHit;
                    headshot = result.isHeadshot();
                }
            }
        }
        return hitEntity != null ? new EntityResult(hitEntity, hitVec, headshot) : null;
    }

    protected List<EntityResult> findEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
        List<EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);

        for (Entity entity : entities) {

            EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result == null) {
                continue;
            }
            hitEntities.add(result);
        }
        return hitEntities;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public EntityResult getHitResult(Entity entity, Vec3 startVec, Vec3 endVec) {
        double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
        AABB boundingBox = entity.getBoundingBox();
        if (Config.COMMON.gameplay.improvedHitboxes.get() &&
                entity instanceof ServerPlayer &&
                this.shooter instanceof ServerPlayer) {
            int ping = (int) Math.floor((((ServerPlayer) this.shooter).connection.latency() / 1000.0) * 20.0 + 0.5);
            boundingBox = BoundingBoxManager.getBoundingBox((Player) entity, ping);
        }
        boundingBox = boundingBox.expandTowards(0, expandHeight, 0);

        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        Vec3 grownHitPos = boundingBox.inflate(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).clip(startVec, endVec).orElse(null);
        if (hitPos == null && grownHitPos != null) {
            HitResult raytraceresult = rayTraceBlocks(this.level(), new ClipContext(startVec, grownHitPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), IGNORE_LEAVES);
            if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                return null;
            }
            hitPos = grownHitPos;
        }

        /* Check for headshot */
        boolean headshot = false;
        if (Config.COMMON.gameplay.enableHeadShots.get() && entity instanceof LivingEntity) {
            IHeadshotBox<LivingEntity> headshotBox = (IHeadshotBox<LivingEntity>) BoundingBoxManager.getHeadshotBoxes(entity.getType());
            if (headshotBox != null) {
                AABB box = headshotBox.getHeadshotBox((LivingEntity) entity);
                if (box != null) {
                    box = box.move(boundingBox.getCenter().x, boundingBox.minY, boundingBox.getCenter().z);
                    Optional<Vec3> headshotHitPos = box.clip(startVec, endVec);
                    if (!headshotHitPos.isPresent()) {
                        box = box.inflate(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get());
                        headshotHitPos = box.clip(startVec, endVec);
                    }
                    if (headshotHitPos.isPresent() && (hitPos == null || headshotHitPos.get().distanceTo(hitPos) < 0.5)) {
                        hitPos = headshotHitPos.get();
                        headshot = true;
                    }
                }
            }
        }

        if (hitPos == null) {
            return null;
        }

        return new EntityResult(entity, hitPos, headshot);
    }

    public void onHit(HitResult result, Vec3 startVec, Vec3 endVec) {
        if (NeoForge.EVENT_BUS.post(new GunProjectileHitEvent(result, this)).isCanceled()) {
            return;
        }

        if (result instanceof BlockHitResult blockHitResult) {
            if (blockHitResult.getType() == HitResult.Type.MISS) {
                return;
            }

            Vec3 hitVec = result.getLocation();
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = this.level().getBlockState(pos);
            Block block = state.getBlock();

            if (Config.COMMON.gameplay.griefing.enableGlassBreaking.get() && state.is(ModTags.Blocks.FRAGILE)) {
                float destroySpeed = state.getDestroySpeed(this.level(), pos);
                if (destroySpeed >= 0) {
                    float chance = Config.COMMON.gameplay.griefing.fragileBaseBreakChance.get().floatValue() / (destroySpeed + 1);
                    if (this.random.nextFloat() < chance) {
                        this.level().destroyBlock(pos, Config.COMMON.gameplay.griefing.fragileBlockDrops.get());
                    }
                }
            }

            if (!state.canBeReplaced()) {
                this.remove(RemovalReason.KILLED);
            }

            if (block instanceof IDamageable) {
                ((IDamageable) block).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int) Math.ceil(this.getDamage() / 2.0) + 1);
            }

            if (this.fireTime >= 0 && this.shooter instanceof Player && this.level() instanceof ServerLevel serverLevel
                    && ShotgunBreachHandler.canBreach(this.projectile.getProjectileAmount())) {
                ShotgunBreachHandler.onPelletHit(serverLevel, this.shooterId, this.fireTime, pos, hitVec.x, hitVec.y, hitVec.z);
            }

            this.onHitBlock(state, pos, blockHitResult.getDirection(), hitVec.x, hitVec.y, hitVec.z);

            if (block instanceof TargetBlock targetBlock) {
                int power = ReflectionUtil.updateTargetBlock(targetBlock, this.level(), state, blockHitResult, this);
                if (this.shooter instanceof ServerPlayer serverPlayer) {
                    serverPlayer.awardStat(Stats.TARGET_HIT);
                    CriteriaTriggers.TARGET_BLOCK_HIT.trigger(serverPlayer, this, blockHitResult.getLocation(), power);
                }
            }

            if (block instanceof BellBlock bell) {
                bell.attemptToRing(this.level(), pos, blockHitResult.getDirection());
            }

            return;
        }

        if (result instanceof ExtendedEntityRayTraceResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (entity.getId() == this.shooterId) {
                return;
            }

            if (this.shooter instanceof Player player) {
                if (entity.hasIndirectPassenger(player)) {
                    return;
                }
                HotBarrelCache.getHotBarrelLevel(player, this.weapon);
                boolean shouldFire = GunEnchantmentHelper.shouldSetOnFire(player, this.weapon);

                if (shouldFire) {
                    entity.igniteForSeconds(5);
                }
            } else {
                if (ModEnchantments.getLevel(this.weapon, ModEnchantments.HOT_BARREL) > 0) {
                    entity.igniteForSeconds(5);
                }
            }

            this.onHitEntity(entity, result.getLocation(), startVec, endVec, entityHitResult.isHeadshot());

            int collateralLevel = ModEnchantments.getLevel(weapon, ModEnchantments.COLLATERAL);
            ResourceLocation advantage = this.getProjectile().getAdvantage();

            if (!(entity.getType().is(ModTags.Entities.GHOST) &&
                    advantage.equals(ModTags.Entities.UNDEAD.location())) ||
                    collateralLevel == 0) {
                this.remove(RemovalReason.KILLED);
            }

            entity.invulnerableTime = 0;
        }
    }

    protected void onLavaImpact(Vec3 impactPos) {
        if (!this.level().isClientSide() && Config.CLIENT.particle.enableLavaImpactParticles.get()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            for (int i = 0; i < 5; i++) {
                double ySpeed = 0.2 + (random.nextDouble() * 0.3);
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        impactPos.x, impactPos.y, impactPos.z,
                        1,
                        0.02, 0, 0.02,
                        ySpeed
                );
            }

            // Smoke particles
            for (int i = 0; i < 3; i++) {
                double xSpeed = (random.nextDouble() - 0.5) * 0.1;
                double ySpeed = 0.2 + (random.nextDouble() * 0.2);
                double zSpeed = (random.nextDouble() - 0.5) * 0.1;
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        impactPos.x, impactPos.y, impactPos.z,
                        1,
                        xSpeed, ySpeed, zSpeed,
                        0.05
                );
            }

            serverLevel.sendParticles(ParticleTypes.LAVA,
                    impactPos.x, impactPos.y + 0.05, impactPos.z,
                    3,
                    0.1, 0.1, 0.1,
                    0.2
            );
        }
        this.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.LAVA_POP, SoundSource.NEUTRAL,
                0.6F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
    }

    protected void onWaterImpact(Vec3 impactPos) {
        if (!this.level().isClientSide()) {
            boolean enableParticles = true;
            try {
                enableParticles = Config.CLIENT.particle.enableWaterImpactParticles.get();
            } catch (IllegalStateException e) {
                enableParticles = true;
            }

            if (enableParticles) {
                ServerLevel serverLevel = (ServerLevel) this.level();
                boolean isSubmerged = this.isInWater();
                int splashParticles = isSubmerged ? 10 : 40;
                int bubbleParticles = isSubmerged ? 10 : 30;
                int snowflakeParticles = isSubmerged ? 5 : 15;
                int fallingWaterParticles = isSubmerged ? 5 : 20;

                for (int i = 0; i < fallingWaterParticles; i++) {
                    double ySpeed = 0.5 + (random.nextDouble() * 0.5);
                    serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                            impactPos.x, impactPos.y, impactPos.z,
                            1, 0.05, 0, 0.05, ySpeed);
                }

                for (int i = 0; i < snowflakeParticles; i++) {
                    double xSpeed = (random.nextDouble() - 0.5) * 0.2;
                    double ySpeed = 0.3 + (random.nextDouble() * 0.3);
                    double zSpeed = (random.nextDouble() - 0.5) * 0.2;
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                            impactPos.x, impactPos.y, impactPos.z,
                            1, xSpeed, ySpeed, zSpeed, 0.1);
                }

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        impactPos.x, impactPos.y + 0.1, impactPos.z,
                        splashParticles, 0.2, 0.2, 0.2, 0.4);

                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                        impactPos.x, impactPos.y, impactPos.z,
                        bubbleParticles, 0.5, 0.3, 0.5, 0.2);
            }

            this.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL,
                    1.2F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }
    }

    public float advantageMultiplier(Entity entity) {
        ResourceLocation advantage = this.getProjectile().getAdvantage();
        if (advantage.equals(ModTags.Entities.NONE.location())) {
            return 1.0f;
        }

        ProjectileAdvantageConfig.AdvantageData advantageData =
                ProjectileAdvantageConfig.getAdvantageData(advantage.toString());

        if (advantageData == null) {
            return 1.0f;
        }
        boolean hasMatchingTag = false;
        for (String targetTag : advantageData.targetTags()) {
            ResourceLocation tagLocation = ResourceLocation.parse(targetTag);
            TagKey<EntityType<?>> entityTag = TagKey.create(Registries.ENTITY_TYPE, tagLocation);
            if (entity.getType().is(entityTag)) {
                hasMatchingTag = true;
                break;
            }
        }

        if (hasMatchingTag) {
            if (advantageData.causesFire() && advantageData.fireDuration() > 0) {
                entity.igniteForSeconds(advantageData.fireDuration());
            }

            return advantageData.multiplier();
        }

        return 1.0f;
    }

    protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
        float damage = this.getDamage();
        float newDamage = this.getCriticalDamage(this.weapon, this.random, damage);
        boolean critical = damage != newDamage;
        damage = newDamage;
        damage *= advantageMultiplier(entity);
        boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
        if (this.shooter instanceof Player player) {
            damage = GunEnchantmentHelper.getWaterProofDamage(this.weapon, player, damage);
        }
        if (headshot) {
            damage *= Config.COMMON.gameplay.headShotDamageMultiplier.get();
        }
        if (entity instanceof LivingEntity livingTarget) {
            damage = GunEnchantmentHelper.getPuncturingDamageReduction(this.weapon, livingTarget, damage);
            damage = applyProjectileProtection(livingTarget, damage);
            damage = calculateArmorBypassDamage(livingTarget, damage);
        }

        DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
        boolean blocked = ProjectileHelper.handleShieldHit(entity, this, damage);

        if (!blocked) {
            if (!(entity.getType().is(ModTags.Entities.GHOST) &&
                    !this.getProjectile().getAdvantage().equals(ModTags.Entities.UNDEAD.location()))) {
                if (damage > 0) {
                    entity.hurt(source, damage);
                }

                if (entity instanceof LivingEntity livingEntity) {
                    ResourceLocation effectLocation = this.projectile.getImpactEffect();
                    if (effectLocation != null) {
                        float effectChance = this.projectile.getImpactEffectChance();
                        if (this.random.nextFloat() < effectChance) {
                            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectLocation).orElse(null); // Port note: 1.21 effects are holders
                            if (effect != null) {
                                livingEntity.addEffect(new MobEffectInstance(
                                        effect,
                                        this.projectile.getImpactEffectDuration(),
                                        this.projectile.getImpactEffectAmplifier()
                                ));
                            }
                        }
                    }
                }
            }
        }

        if (entity instanceof LivingEntity) {
            GunEnchantmentHelper.applyElementalPopEffect(this.weapon, (LivingEntity) entity);
        }

        if (this.shooter instanceof Player) {
            int hitType = critical ? S2CMessageProjectileHitEntity.HitType.CRITICAL : headshot ? S2CMessageProjectileHitEntity.HitType.HEADSHOT : S2CMessageProjectileHitEntity.HitType.NORMAL;
            PacketHandler.getPlayChannel().sendToPlayer(() -> (ServerPlayer) this.shooter, new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, hitType, entity instanceof Player));
        }
        if (wasAlive && entity instanceof LivingEntity livingEntity && !livingEntity.isAlive()) {
            checkForDiamondSteelBonus(livingEntity, hitVec);
        }
        PacketHandler.getPlayChannel().sendToTracking(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));

    }
    void checkForDiamondSteelBonus(LivingEntity killedEntity, Vec3 position) {
        if (!this.level().isClientSide && this.getShooter() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();

            if (weapon.getItem() instanceof AnimatedDiamondSteelGunItem || weapon.getItem() instanceof AnimatedDiamondSteelUnderWaterGunItem || weapon.getItem() instanceof AnimatedDiamondSteelAirGunItem /* Batch 23/24 */) {
                int baseXP = killedEntity.getExperienceReward((ServerLevel) this.level(), player);
                int bonusXP = Math.round(baseXP * 0.2f);

                if (bonusXP > 0) {
                    ExperienceOrb xpOrb = new ExperienceOrb(this.level(), position.x, position.y, position.z, bonusXP);
                    this.level().addFreshEntity(xpOrb);
                }
            }
        }
    }
    public float applyProjectileProtection(LivingEntity target, float damage) {
        int protectionLevel = EnchantmentHelper.getEnchantmentLevel(target.registryAccess().holderOrThrow(Enchantments.PROJECTILE_PROTECTION), target); // Port note: enchantments are holders

        if (protectionLevel > 0) {
            float reduction = protectionLevel * 0.10f;
            reduction = Math.min(reduction, 0.8f);
            damage *= (1.0f - reduction);
        }

        return damage;
    }

    protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
        PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
        Block block = state.getBlock();

        if (primeTNT(state, pos)) {
            return;
        }
        if (block instanceof DoorBlock) {
            boolean isOpen = state.getValue(DoorBlock.OPEN);
            if (!isOpen) {
                this.level().setBlock(pos, state.setValue(DoorBlock.OPEN, true), 10);
                this.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        if (!state.canBeReplaced()) {
            this.remove(RemovalReason.KILLED);
        }
        if (block instanceof IDamageable) {
            ((IDamageable) block).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int) Math.ceil(this.getDamage() / 2.0) + 1);
        }
    }

    boolean primeTNT(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block == Blocks.TNT) {
            if (!this.level().isClientSide()) {
                TntBlock.explode(this.level(), pos);
                this.level().removeBlock(pos, false);
            }
            return true;
        }
        if (block == ModBlocks.POWDER_KEG.get()) {
            if (!this.level().isClientSide()) {
                PowderKegBlock.explode(this.level(), pos);
                this.level().removeBlock(pos, false);
            }
            return true;
        }
        if (block == ModBlocks.NITRO_KEG.get()) {
            if (!this.level().isClientSide()) {
                NitroKegBlock.explode(this.level(), pos);
                this.level().removeBlock(pos, false);
            }
            return true;
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.projectile = new Gun.Projectile();
        this.projectile.deserializeNBT(compound.getCompound("Projectile"));
        this.general = new Gun.General();
        this.general.deserializeNBT(compound.getCompound("General"));
        this.modifiedGravity = compound.getDouble("ModifiedGravity");
        this.life = compound.getInt("MaxLife");
        this.distanceTraveled = compound.getDouble("DistanceTraveled");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.put("Projectile", this.projectile.serializeNBT());
        compound.put("General", this.general.serializeNBT());
        compound.putDouble("ModifiedGravity", this.modifiedGravity);
        compound.putInt("MaxLife", this.life);
        compound.putDouble("DistanceTraveled", this.distanceTraveled);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeNbt(this.projectile.serializeNBT());
        buffer.writeNbt(this.general.serializeNBT());
        buffer.writeInt(this.shooterId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.item); // Port note: was BufferUtil.writeItemStackToBufIgnoreTag
        buffer.writeDouble(this.modifiedGravity);
        buffer.writeVarInt(this.life);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.projectile = new Gun.Projectile();
        this.projectile.deserializeNBT(buffer.readNbt());
        this.general = new Gun.General();
        this.general.deserializeNBT(buffer.readNbt());
        this.shooterId = buffer.readInt();
        this.item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        this.modifiedGravity = buffer.readDouble();
        this.life = buffer.readVarInt();
        this.entitySize = EntityDimensions.scalable(this.projectile.getSize(), this.projectile.getSize());
    }

    public void updateHeading() {
        double horizontalDistance = this.getDeltaMovement().horizontalDistance();
        this.setYRot((float) (Mth.atan2(this.getDeltaMovement().x(), this.getDeltaMovement().z()) * (180D / Math.PI)));
        this.setXRot((float) (Mth.atan2(this.getDeltaMovement().y(), horizontalDistance) * (180D / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public Projectile getProjectile() {
        return this.projectile;
    }

    static Vec3 getVectorFromRotation(float pitch, float yaw) {
        float f = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -Mth.cos(-pitch * 0.017453292F);
        float f3 = Mth.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    /**
     * Gets the entity who spawned the projectile
     */
    public LivingEntity getShooter() {
        return this.shooter;
    }

    /**
     * Gets the id of the entity who spawned the projectile
     */
    public int getShooterId() {
        return this.shooterId;
    }

    float getCriticalDamage(ItemStack weapon, RandomSource rand, float damage) {
        float chance = GunModifierHelper.getCriticalChance(weapon);
        if (rand.nextFloat() < chance) {
            float critMultiplier = this.modifiedGun.getProjectile().getCritDamageMultiplier();
            return damage * critMultiplier;
        }
        return damage;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void onRemovedFromLevel() { // Port note: renamed from onRemovedFromWorld in NeoForge 1.21
        if (!this.level().isClientSide) {
            // Port note: Framework LevelLocation -> the PacketHandler shim (players within 256 blocks)
            PacketHandler.getPlayChannel().sendToNearbyPlayers((ServerLevel) this.level(), this.getX(), this.getY(), this.getZ(), 256, new S2CMessageRemoveProjectile(this.getId()));
        }
    }

    // Port note: getDeathTargetPoint (Framework LevelLocation) and getAddEntityPacket (NetworkHooks) are gone; NeoForge sends
    // the complex spawn data itself.

    /**
     * A custom implementation of ray tracing that allows you to pass a predicate to ignore certain
     * blocks when checking for collisions.
     *
     * @param world           the world to perform the ray trace
     * @param context         the ray trace context
     * @param ignorePredicate the block state predicate
     * @return a result of the raytrace
     */
    static BlockHitResult rayTraceBlocks(Level world, ClipContext context, Predicate<BlockState> ignorePredicate) {
        // Sable sub-levels are traced in their own plot space; their hits keep the plot block position
        return SableCompat.clipIncludingSubLevels(world, context.getFrom(), context.getTo(),
                (startVec, endVec) -> rayTraceLevelBlocks(world, context, startVec, endVec, ignorePredicate));
    }

    private static BlockHitResult rayTraceLevelBlocks(Level world, ClipContext context, Vec3 startVec, Vec3 endVec, Predicate<BlockState> ignorePredicate) {
        return performRayTrace(startVec, endVec, blockPos -> {
            BlockState blockState = world.getBlockState(blockPos);
            if (ignorePredicate.test(blockState)) return null;
            FluidState fluidState = world.getFluidState(blockPos);
            VoxelShape blockShape = context.getBlockShape(blockState, world, blockPos);
            BlockHitResult blockResult = world.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState);
            VoxelShape fluidShape = context.getFluidShape(fluidState, world, blockPos);
            BlockHitResult fluidResult = fluidShape.clip(startVec, endVec, blockPos);
            double blockDistance = blockResult == null ? Double.MAX_VALUE : startVec.distanceToSqr(blockResult.getLocation());
            double fluidDistance = fluidResult == null ? Double.MAX_VALUE : startVec.distanceToSqr(fluidResult.getLocation());
            return blockDistance <= fluidDistance ? blockResult : fluidResult;
        }, () -> {
            Vec3 Vector3d = startVec.subtract(endVec);
            return BlockHitResult.miss(endVec, Direction.getNearest(Vector3d.x, Vector3d.y, Vector3d.z), BlockPos.containing(endVec));
        });
    }

    private static <T> T performRayTrace(Vec3 startVec, Vec3 endVec, Function<BlockPos, T> hitFunction, Supplier<T> p_217300_2_) {
        if (startVec.equals(endVec)) {
            return p_217300_2_.get();
        } else {
            double startX = Mth.lerp(-0.0000001, endVec.x, startVec.x);
            double startY = Mth.lerp(-0.0000001, endVec.y, startVec.y);
            double startZ = Mth.lerp(-0.0000001, endVec.z, startVec.z);
            double endX = Mth.lerp(-0.0000001, startVec.x, endVec.x);
            double endY = Mth.lerp(-0.0000001, startVec.y, endVec.y);
            double endZ = Mth.lerp(-0.0000001, startVec.z, endVec.z);
            int blockX = Mth.floor(endX);
            int blockY = Mth.floor(endY);
            int blockZ = Mth.floor(endZ);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);
            T t = hitFunction.apply(mutablePos);
            if (t != null) {
                return t;
            }

            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = Mth.sign(deltaX);
            int signY = Mth.sign(deltaY);
            int signZ = Mth.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0D - Mth.frac(endX) : Mth.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0D - Mth.frac(endY) : Mth.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0D - Mth.frac(endZ) : Mth.frac(endZ));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                if (d12 < d13) {
                    if (d12 < d14) {
                        blockX += signX;
                        d12 += d9;
                    } else {
                        blockZ += signZ;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    blockY += signY;
                    d13 += d10;
                } else {
                    blockZ += signZ;
                    d14 += d11;
                }

                T t1 = hitFunction.apply(mutablePos.set(blockX, blockY, blockZ));
                if (t1 != null) {
                    return t1;
                }
            }

            return p_217300_2_.get();
        }
    }

    /**
     * Creates a projectile explosion for the specified entity.
     *
     * @param entity    The entity to explode
     * @param radius    The amount of radius the entity should deal
     * @param forceNone If true, forces the explosion mode to be NONE instead of config value
     */
    public static void createExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;

        boolean allowBlockRemoval = Config.COMMON.gameplay.griefing.enableBlockRemovalOnExplosions.get() && !forceNone;
        if (allowBlockRemoval && entity instanceof ProjectileEntity projectile) {
            LivingEntity shooter = projectile.getShooter();
            if (shooter != null && !(shooter instanceof Player)) {
                allowBlockRemoval = Config.COMMON.gameplay.griefing.enableMobExplosionBlockRemoval.get();
            }
        }

        Explosion.BlockInteraction mode = allowBlockRemoval ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP;
        Explosion explosion = new ProjectileExplosion(world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius, false, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);

        explosion.getToBlow().forEach(pos ->
        {
            if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                ((IExplosionDamageable) world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
            }
        });

        if (!explosion.interactsWithBlocks()) {
            explosion.clearToBlow();
        }

        for (ServerPlayer player : ((ServerLevel) world).players()) {
            if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4096) {
                player.connection.send(new ClientboundExplodePacket(entity.getX(), entity.getY(), entity.getZ(), radius, explosion.getToBlow(), explosion.getHitPlayers().get(player),
                        explosion.getBlockInteraction(), explosion.getSmallExplosionParticles(), explosion.getLargeExplosionParticles(), explosion.getExplosionSound()));
            }
        }
    }

    public static void createRocketExplosion(Entity entity, float radius, float damage, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ?
                entity.damageSources().explosion(entity, projectile.getShooter()) : null;

        boolean allowBlockRemoval = Config.COMMON.gameplay.griefing.enableBlockRemovalOnExplosions.get() && !forceNone;
        if (allowBlockRemoval && entity instanceof ProjectileEntity projectile) {
            LivingEntity shooter = projectile.getShooter();
            if (shooter != null && !(shooter instanceof Player)) {
                allowBlockRemoval = Config.COMMON.gameplay.griefing.enableMobExplosionBlockRemoval.get();
            }
        }

        Explosion.BlockInteraction mode = allowBlockRemoval ?
                Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP;

        Explosion explosion = new RocketExplosion(world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius, damage, false, mode);

        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);

        explosion.getToBlow().forEach(pos -> {
            if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                ((IExplosionDamageable) world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
            }
        });

        if (!explosion.interactsWithBlocks()) {
            explosion.clearToBlow();
        }
    }

    public static void createFireExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ?
                entity.damageSources().explosion(entity, projectile.getShooter()) : null;
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;

        Explosion explosion = new ProjectileExplosion(world, entity, source, null,
                entity.getX(), entity.getY(), entity.getZ(), radius * 0.5f, true, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);

        BlockPos centerPos = entity.blockPosition();
        AABB effectArea = new AABB(centerPos).inflate(radius);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, effectArea);

        for (LivingEntity livingEntity : nearbyEntities) {
            double distance = livingEntity.distanceTo(entity);
            if (distance <= radius) {
                livingEntity.igniteForSeconds(8);
                float damage = (float) (4.0F * (1.0 - distance / radius));
                livingEntity.hurt(world.damageSources().inFire(), damage);
            }
        }
    }
    public static void createSoulFireExplosion(Entity entity, float radius, boolean forceNone) {
        Level world = entity.level();
        if (world.isClientSide())
            return;

        DamageSource source = entity instanceof ProjectileEntity projectile ?
                entity.damageSources().explosion(entity, projectile.getShooter()) : null;
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;

        Explosion explosion = new ProjectileExplosion(world, entity, source, null,
                entity.getX(), entity.getY(), entity.getZ(), radius * 0.5f, false, mode) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
                return 0;
            }
        };

        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(world, explosion))
            return;

        explosion.explode();
        explosion.finalizeExplosion(true);
        BlockPos centerPos = entity.blockPosition();
        AABB effectArea = new AABB(centerPos).inflate(radius);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, effectArea);

        for (LivingEntity livingEntity : nearbyEntities) {
            double distance = livingEntity.distanceTo(entity);
            if (distance <= radius) {
                livingEntity.igniteForSeconds(8);

                float damage = (float) (4.0F * (1.0 - distance / radius));
                livingEntity.hurt(world.damageSources().inFire(), damage);
            }
        }

        int radiusInt = (int) Math.ceil(radius);
        int radiusSquared = radiusInt * radiusInt;

        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int z = -radiusInt; z <= radiusInt; z++) {
                BlockPos columnPos = centerPos.offset(x, 0, z);

                if (centerPos.distSqr(columnPos) <= radiusSquared) {
                    for (int y = -radiusInt; y <= radiusInt; y++) {
                        BlockPos pos = centerPos.offset(x, y, z);
                        BlockState stateAtPos = world.getBlockState(pos);
                        BlockState stateBelow = world.getBlockState(pos.below());

                        if (stateAtPos.isAir() &&
                                (stateBelow.isFaceSturdy(world, pos.below(), net.minecraft.core.Direction.UP) ||
                                        !stateBelow.isAir())) {
                            world.setBlock(pos, ModBlocks.FAKE_SOUL_FIRE.get().defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }

    public Entity getOwner() {
        return this.shooter;
    }

    /**
     * Author: MrCrayfish
     */
    public static class EntityResult {
        private final Entity entity;
        private final Vec3 hitVec;
        private final boolean headshot;

        public EntityResult(Entity entity, Vec3 hitVec, boolean headshot) {
            this.entity = entity;
            this.hitVec = hitVec;
            this.headshot = headshot;
        }

        /**
         * Gets the entity that was hit by the projectile
         */
        public Entity getEntity() {
            return this.entity;
        }

        /**
         * Gets the position the projectile hit
         */
        public Vec3 getHitPos() {
            return this.hitVec;
        }

        /**
         * Gets if this was a headshot
         */
        public boolean isHeadshot() {
            return this.headshot;
        }
    }

    public static class ProjectileHelper {
        public static final float DEFAULT_SHIELD_DISABLE_CHANCE = 0.30f;

        public static boolean handleShieldHit(Entity target, Entity projectile, float damage, float shieldDisableChance) {
            if (!(target instanceof Player player)) {
                return false;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            boolean isBlockingMainHand = player.isBlocking() && mainHandItem.getItem() instanceof ShieldItem;
            boolean isBlockingOffHand = player.isBlocking() && offHandItem.getItem() instanceof ShieldItem;

            if (!isBlockingMainHand && !isBlockingOffHand) {
                return false;
            }

            ItemStack shield = isBlockingMainHand ? mainHandItem : offHandItem;
            InteractionHand hand = isBlockingMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            if (projectile.level().getRandom().nextFloat() < shieldDisableChance) {
                player.getCooldowns().addCooldown(shield.getItem(), 100);
                player.stopUsingItem();
                player.level().broadcastEntityEvent(player, (byte) 30);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
                return false;
            }

            player.hurt(player.damageSources().generic(), 0.5f);
            shield.hurtAndBreak(12, player, LivingEntity.getSlotForHand(hand));

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);

            return true;
        }

        public static boolean handleShieldHit(Entity target, Entity projectile, float damage) {
            return handleShieldHit(target, projectile, damage, DEFAULT_SHIELD_DISABLE_CHANCE);
        }
    }
}