package top.kristeal.entity.monster;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.Config;
import top.kristeal.config.EntityEquipmentConfig;
import top.kristeal.init.ModEntities;
import top.kristeal.init.ModTags;

public class CogMinionEntity extends Monster {
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(CogMinionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT =
            SynchedEntityData.defineId(CogMinionEntity.class, EntityDataSerializers.INT);

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();

    public CogMinionEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCanPickUpLoot(true);
    }


    private boolean isHoldingExplosiveBlock() {
        ItemStack mainHandItem = this.getMainHandItem();
        return mainHandItem.is(ModTags.Items.EXPLOSIVE_BLOCK);
    }
    private void explodeIfHoldingExplosive() {
        if (isHoldingExplosiveBlock() && !this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, false, Level.ExplosionInteraction.MOB);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
    // Port note: LivingEntity#canBreatheUnderwater is final in 1.21 (tag-driven). Forge 1.20.1's canDrownInFluidType default
    // returned !canBreatheUnderwater() for water, so this keeps the reference behaviour: no drowning in water, other fluids unchanged.
    @Override
    public boolean canDrownInFluidType(FluidType type) {
        if (type == NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance pPotionEffect) {
        // Port note: effects are registry holders in 1.21; MobEffects constants are the same Holder.Reference instances
        Holder<MobEffect> effect = pPotionEffect.getEffect();

        if (effect == MobEffects.POISON ||
                effect == MobEffects.WITHER ||
                effect == MobEffects.HUNGER ||
                effect == MobEffects.REGENERATION ||
                effect == MobEffects.SATURATION ||
                effect == MobEffects.CONFUSION ||
                effect == MobEffects.BLINDNESS ||
                effect == MobEffects.WEAKNESS ||
                effect == MobEffects.MOVEMENT_SLOWDOWN ||
                effect == MobEffects.DIG_SLOWDOWN ||
                effect == MobEffects.HARM ||
                effect == MobEffects.HEAL) {
            return false;
        }

        return super.canBeAffected(pPotionEffect);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.5D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            if (source.getEntity() instanceof Player) {
                float spawnChance = Config.COMMON.gameplay.cogBeaconSpawnChance.get().floatValue();
                if (spawnChance > 0 && this.random.nextFloat() < spawnChance) {
                    SignalBeaconEntity beacon = new SignalBeaconEntity(ModEntities.SIGNAL_BEACON.get(), this.level());
                    beacon.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                    this.level().addFreshEntity(beacon);
                }
            }
        }
    }
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty,
                                        MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        EntityEquipmentConfig.equipEntity(this, "scguns:cog_minion");
        return pSpawnData;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.isAttacking() && this.getAttackTimeout() > 0) {
                this.setAttackTimeout(this.getAttackTimeout() - 1);
                if (this.getAttackTimeout() == 6) {
                    LivingEntity target = this.getTarget();
                    if (target != null && this.distanceToSqr(target) <= this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth()) {
                        boolean didHurt = this.doHurtTarget(target);
                        if (didHurt) {
                            this.explodeIfHoldingExplosive();
                        }
                    }
                }

                if (this.getAttackTimeout() <= 0) {
                    this.setAttacking(false);
                }
            }
        }

        if (this.level().isClientSide()) {
            setupAnimationStates();
        }
    }

    private void setupAnimationStates() {
        if (this.isAttacking()) {
            this.idleAnimationState.stop();
            this.attackAnimationState.startIfStopped(this.tickCount);
        } else {
            this.attackAnimationState.stop();
            this.idleAnimationState.startIfStopped(this.tickCount);
        }
    }

    @Override
    public boolean wantsToPickUp(ItemStack pStack) {
        return this.canHoldItem(pStack);
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
        if (!this.getItemBySlot(slot).isEmpty()) {
            return false;
        }
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.HEAD;
    }

    @Override
    public boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing) {
        if (existing.isEmpty()) {
            EquipmentSlot slot = this.getEquipmentSlotForItem(candidate);
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.HEAD;
        }
        return false;
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
        if (attacking) {
            this.setAttackTimeout(12);
        }
    }

    public void setAttackTimeout(int timeout) {
        this.entityData.set(ATTACK_TIMEOUT, timeout);
    }

    public int getAttackTimeout() {
        return this.entityData.get(ATTACK_TIMEOUT);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
        builder.define(ATTACK_TIMEOUT, 0);
    }
    @Override
    protected void updateWalkAnimation(float pPartialTick) {
        float f;
        if (this.getPose() == Pose.STANDING) {
            f = Math.min(pPartialTick * 6F, 1f);
        } else {
            f = 0f;
        }
        this.walkAnimation.update(f, 0.2f);
    }
    @Override
    protected Vec3i getPickupReach() {
        return new Vec3i(3, 3, 3);
    }

    @Override
    protected void pickUpItem(ItemEntity pItemEntity) {
        super.pickUpItem(pItemEntity);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected void checkAndPerformAttack(LivingEntity pEnemy) {
                // Port note: 1.21 no longer passes the distance; 1.20.1 passed the squared distance to the target's position
                double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
                if (pDistToEnemySqr <= this.getAttackReachSqr(pEnemy) && this.getTicksUntilNextAttack() <= 0 && !CogMinionEntity.this.isAttacking()) {
                    CogMinionEntity.this.setAttacking(true);
                    this.resetAttackCooldown();
                    this.mob.swing(InteractionHand.MAIN_HAND);
                }
            }

            /** Port note: MeleeAttackGoal#getAttackReachSqr was removed in 1.20.5; this is the 1.20.1 formula. */
            private double getAttackReachSqr(LivingEntity pEnemy) {
                return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pEnemy.getBbWidth();
            }

            @Override
            protected void resetAttackCooldown() {
                this.adjustedTickDelay(25);
            }
        });
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CogMinionEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.0, 30));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
    }
    @Override
    public boolean canHoldItem(ItemStack stack) {
        return true;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

}