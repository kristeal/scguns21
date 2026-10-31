package top.kristeal.common.network;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Config;
import top.kristeal.common.*;
import top.kristeal.entity.projectile.ProjectileEntity;
import top.kristeal.event.GunEventBus;
import top.kristeal.event.GunFireEvent;
import top.kristeal.init.*;
import top.kristeal.interfaces.IProjectileFactory;
import top.kristeal.item.GunItem;
import top.kristeal.item.ammo_boxes.CreativeAmmoBoxItem;
import top.kristeal.item.animated.ExoSuitItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.*;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPlayHandler {
    public static void handleShoot(C2SMessageShoot message, ServerPlayer player) {
        if (player.isSpectator() || player.getUseItem().getItem() == Items.SHIELD)
            return;

        Level world = player.level();
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(heldItem.getItem() instanceof GunItem item)) {
            return;
        }

        if (!Gun.hasAmmo(heldItem) && !player.isCreative()) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.8F);
            return;
        }

        Gun modifiedGun = item.getModifiedGun(heldItem);
        if (modifiedGun == null) return;
        if (NeoForge.EVENT_BUS.post(new GunFireEvent.Pre(player, heldItem)).isCanceled())
            return;

        player.setYRot(Mth.wrapDegrees(message.getRotationYaw()));
        player.setXRot(Mth.clamp(message.getRotationPitch(), -90F, 90F));

        ShootTracker tracker = ShootTracker.getShootTracker(player);
        if (tracker.hasCooldown(item) && tracker.getRemaining(item) > Config.SERVER.cooldownThreshold.get()) {
            return;
        }
        tracker.putCooldown(heldItem, item, modifiedGun);

        if(ModSyncedDataKeys.RELOADING.getValue(player)) {
            ModSyncedDataKeys.RELOADING.setValue(player, false);
        }

        if (!modifiedGun.getProjectile().isAlwaysSpread() && modifiedGun.getProjectile().getSpread() > 0.0F) {
            SpreadTracker.get(player).update(player, item);
        }
        if (FireMode.BEAM.equals(modifiedGun.getGeneral().getFireMode()) ||
                FireMode.SEMI_BEAM.equals(modifiedGun.getGeneral().getFireMode())) {
            BeamWeaponHandler.handleBeamWeapon(player, heldItem, modifiedGun);
        }
        else if (modifiedGun.getProjectile(heldItem).firesArrows()) {
            int count = modifiedGun.getProjectile().getProjectileAmount();
            for (int i = 0; i < count; i++) {
                Arrow arrow = getArrow(player, world, heldItem, modifiedGun);
                arrow.pickup = Arrow.Pickup.ALLOWED;
                world.addFreshEntity(arrow);
            }
        }
        else {
            fireProjectiles(world, player, heldItem, item, modifiedGun);
        }
        if (!FireMode.BEAM.equals(modifiedGun.getGeneral().getFireMode()) &&
                !FireMode.SEMI_BEAM.equals(modifiedGun.getGeneral().getFireMode())) {
            consumeAmmo(player, heldItem);
        }

        handleCasingEjection(player, heldItem, modifiedGun, world);

        handleWeaponDamage(player, heldItem, world);

        ResourceLocation fireSound = getFireSound(heldItem, modifiedGun);
        if (fireSound != null) {
            playFireSound(player, world, heldItem, modifiedGun, fireSound);
        }
        GunEffectsHandler.handleGunEffects(player, heldItem, modifiedGun);

        NeoForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem));
        player.awardStat(Stats.ITEM_USED.get(item));
    }

    private static void consumeAmmo(ServerPlayer player, ItemStack heldItem) {
        if (!player.isCreative()) {
            if (!GunStackData.isIgnoreAmmo(heldItem)) { // Port note: IGNORE_AMMO / AMMO_COUNT components
                int level = ModEnchantments.getLevel(heldItem, ModEnchantments.RECLAIMED);
                if (level == 0 || player.level().random.nextInt(4 - Mth.clamp(level, 1, 2)) != 0) {
                    int currentAmmo = GunStackData.getAmmoCount(heldItem);
                    GunStackData.setAmmoCount(heldItem, Math.max(0, currentAmmo - 1));
                }
            }
        }
    }

    private static void handleCasingEjection(ServerPlayer player, ItemStack heldItem, Gun modifiedGun, Level world) {
        if (Config.COMMON.gameplay.spawnCasings.get()) {
            if (modifiedGun.getProjectile(heldItem).casingType != null && !player.getAbilities().instabuild &&
                    !modifiedGun.getProjectile(heldItem).ejectDuringReload()) {
                ItemStack casingStack = new ItemStack(Objects.requireNonNull(
                        BuiltInRegistries.ITEM.get(modifiedGun.getProjectile(heldItem).casingType)));

                double baseChance = 0.4;
                int enchantmentLevel = ModEnchantments.getLevel(heldItem, ModEnchantments.SHELL_CATCHER);
                double finalChance = baseChance + (enchantmentLevel * 0.15);

                if (Math.random() < finalChance) {
                    if (enchantmentLevel > 0) {
                        if (!GunEventBus.addCasingDirectly(player, casingStack)) {
                            GunEventBus.spawnCasingInWorld(world, player, casingStack);
                        }
                    } else {
                        if (!GunEventBus.addCasingToPouch(player, casingStack)) {
                            GunEventBus.spawnCasingInWorld(world, player, casingStack);
                        }
                    }
                }
            }
        }
    }

    private static void handleWeaponDamage(ServerPlayer player, ItemStack heldItem, Level world) {
        if (!player.isCreative()) {
            if (Config.COMMON.gameplay.enableGunDamage.get()) {
                GunEventBus.damageGun(heldItem, world, player);
            }
            if (Config.COMMON.gameplay.enableAttachmentDamage.get()) {
                GunEventBus.damageAttachments(heldItem, world, player);
            }
        }
    }

    public static class RatKingAndQueenModel {
        public static class GunFireEventRatHandler {
            private static int shotCount = 0;

            public static int getShotCount() {
                return shotCount;
            }

            public static void incrementShotCount() {
                shotCount++;
            }

            public static boolean shouldUseAlternateAnimation() {
                return shotCount % 2 == 1;
            }
        }
    }

    @NotNull
    private static Arrow getArrow(ServerPlayer player, Level world, ItemStack heldItem, Gun modifiedGun) {
        Arrow arrow = new Arrow(world, player, new ItemStack(Items.ARROW), heldItem); // Port note: 1.21 Arrow takes the pickup and weapon stacks

        float speed = (float) modifiedGun.getProjectile(heldItem).getSpeed() * 0.35f;
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float f = -Mth.sin(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
        float f1 = -Mth.sin(pitch * ((float)Math.PI / 180F));
        float f2 = Mth.cos(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
        Vec3 motion = new Vec3(f, f1, f2);
        Vec3 spawnPos = player.getEyePosition().add(
                motion.x * 0.5,
                -0.1,
                motion.z * 0.5
        );
        arrow.setPos(spawnPos);
        arrow.setDeltaMovement(motion.x * speed, motion.y * speed, motion.z * speed);

        float horizontalDistance = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        arrow.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180F / Math.PI)));
        arrow.setXRot((float) (Mth.atan2(motion.y, horizontalDistance) * (180F / Math.PI)));
        arrow.yRotO = arrow.getYRot();
        arrow.xRotO = arrow.getXRot();

        arrow.setBaseDamage(modifiedGun.getProjectile(heldItem).getDamage() * 0.15);
        return arrow;
    }

    private static void fireProjectiles(Level world, ServerPlayer player, ItemStack heldItem, GunItem item, Gun modifiedGun) {
        int count = modifiedGun.getProjectile().getProjectileAmount();
        Gun.Projectile projectileProps = modifiedGun.getProjectile(heldItem);
        ProjectileEntity[] spawnedProjectiles = new ProjectileEntity[count];

        for (int i = 0; i < count; i++) {
            IProjectileFactory factory = ProjectileManager.getInstance().getFactory(BuiltInRegistries.ITEM.getKey(projectileProps.getItem()));
            ProjectileEntity projectileEntity = factory.create(world, player, heldItem, item, modifiedGun);
            projectileEntity.setWeapon(heldItem);
            projectileEntity.setAdditionalDamage(Gun.getAdditionalDamage(heldItem));
            world.addFreshEntity(projectileEntity);
            spawnedProjectiles[i] = projectileEntity;
            projectileEntity.tick();
        }

        if (!projectileProps.shouldHideProjectile()) {
            sendProjectileTrail(player, spawnedProjectiles, projectileProps, false);
        }
    }
    private static void sendProjectileTrail(ServerPlayer player, ProjectileEntity[] projectiles, Gun.Projectile projectileProps, boolean b) {
        if(projectileProps.shouldHideTrail()) {
            return;
        }

        double spawnX = player.getX();
        double spawnY = player.getY() + 1.0;
        double spawnZ = player.getZ();
        double radius = Config.COMMON.network.projectileTrackingRange.get();
        ParticleOptions data = GunEnchantmentHelper.getParticle(player.getMainHandItem());

        S2CMessageBulletTrail messageBulletTrail = new S2CMessageBulletTrail(
                projectiles,
                projectileProps,
                player.getId(),
                data,
                true);

        PacketHandler.getPlayChannel().sendToNearbyPlayers((ServerLevel) player.level(), spawnX, spawnY, spawnZ, radius, messageBulletTrail);
    }

    private static void playFireSound(ServerPlayer player, Level world, ItemStack heldItem, Gun modifiedGun, ResourceLocation fireSound) {
        double posX = player.getX();
        double posY = player.getY() + player.getEyeHeight();
        double posZ = player.getZ();
        float volume = GunModifierHelper.getFireSoundVolume(heldItem);
        float pitch = 0.9F + world.random.nextFloat() * 0.2F;
        double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, Config.SERVER.gunShotMaxDistance.get());
        boolean muzzle = modifiedGun.getDisplay().getFlash() != null;

        S2CMessageGunSound messageSound = new S2CMessageGunSound(
                fireSound, SoundSource.PLAYERS, (float) posX, (float) posY, (float) posZ,
                volume, pitch, player.getId(), muzzle, false);

        PacketHandler.getPlayChannel().sendToNearbyPlayers((ServerLevel) player.level(), posX, posY, posZ, radius, messageSound);
    }

    public static void handleStopBeam(ServerPlayer player) {
        BeamWeaponHandler.stopBeam(player);
    }

    public static EntityHitResult rayTraceEntities(Level world, Entity shooter, Vec3 startVec, Vec3 endVec) {
        endVec.subtract(startVec).normalize();
        double maxDistance = startVec.distanceTo(endVec);
        AABB searchArea = new AABB(startVec, endVec).inflate(1.0D);

        Entity closestEntity = null;
        Vec3 hitVec = null;
        double minDistance = maxDistance;

        List<Entity> entities = world.getEntities(shooter, searchArea, entity -> !entity.isSpectator() && entity.isPickable() && entity.isAlive());

        for (Entity entity : entities) {
            AABB entityBB = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> optionalHit = entityBB.clip(startVec, endVec);

            if (optionalHit.isPresent()) {
                double distance = startVec.distanceTo(optionalHit.get());

                if (distance < minDistance) {
                    minDistance = distance;
                    closestEntity = entity;
                    hitVec = optionalHit.get();
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, hitVec);
        }

        return null;
    }

    public static void handlePreFireSound(ServerPlayer player) {
        Level world = player.level();
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(heldItem.getItem() instanceof GunItem item && (Gun.hasAmmo(heldItem) || player.isCreative()))
        {
            Gun modifiedGun = item.getModifiedGun(heldItem);
            ResourceLocation fireSound = getPreFireSound(heldItem, modifiedGun);
            if(fireSound != null)
            {
                double posX = player.getX();
                double posY = player.getY() + player.getEyeHeight();
                double posZ = player.getZ();
                float volume = GunModifierHelper.getFireSoundVolume(heldItem);
                float pitch = 0.9F + world.random.nextFloat() * 0.2F;
                double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, Config.SERVER.gunShotMaxDistance.get());
                S2CMessageGunSound messageSound = new S2CMessageGunSound(fireSound, SoundSource.PLAYERS, (float) posX, (float) posY, (float) posZ, volume, pitch, player.getId(), false, false);
                PacketHandler.getPlayChannel().sendToNearbyPlayers((ServerLevel) player.level(), posX, posY, posZ, radius, messageSound);
            }
        }
    }

    private static ResourceLocation getFireSound(ItemStack stack, Gun modifiedGun)
    {
        ResourceLocation fireSound = null;
        if(GunModifierHelper.isSilencedFire(stack))
        {
            fireSound = modifiedGun.getSounds().getSilencedFire();
        }
        else if(stack.isEnchanted())
        {
            fireSound = modifiedGun.getSounds().getEnchantedFire();
        }
        if(fireSound != null)
        {
            return fireSound;
        }
        return modifiedGun.getSounds().getFire();
    }

    private static ResourceLocation getPreFireSound(ItemStack stack, Gun modifiedGun)
    {
        return modifiedGun.getSounds().getPreFire();
    }

    public static void handleUnload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(stack);
            if (player.getInventory().items.stream().anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem) ||
                    hasCreativeAmmoBoxInCurios(player)) {
                return;
            }

            if (gun.getReloads().getReloadType() != ReloadType.SINGLE_ITEM) {
                if (GunStackData.hasAmmoCount(stack)) { // Port note: AMMO_COUNT component
                    int count = GunStackData.getAmmoCount(stack);
                    GunStackData.setAmmoCount(stack, 0);

                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(gun.getCurrentAmmoItem(stack));
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == null) return;

                    int maxStackSize = item.getDefaultMaxStackSize(); // Port note: Item#getMaxStackSize() became getDefaultMaxStackSize()
                    int stacks = count / maxStackSize;
                    for (int i = 0; i < stacks; i++) {
                        spawnAmmo(player, new ItemStack(item, maxStackSize));
                    }

                    int remaining = count % maxStackSize;
                    if (remaining > 0) {
                        spawnAmmo(player, new ItemStack(item, remaining));
                    }
                }
            }
        }
    }

    public static boolean hasCreativeAmmoBoxInCurios(ServerPlayer player) {
        AtomicBoolean found = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                if (curios.getStackInSlot(i).getItem() instanceof CreativeAmmoBoxItem) {
                    found.set(true);
                    return;
                }
            }
        });
        return found.get();
    }

    public static void handleExtraAmmo(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem) {
            boolean hasCreativeBox = player.getInventory().items.stream().anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem) ||
                    hasCreativeAmmoBoxInCurios(player);

            Gun gun = gunItem.getModifiedGun(stack);
            if (GunStackData.hasAmmoCount(stack)) { // Port note: AMMO_COUNT component
                int currentAmmo = GunStackData.getAmmoCount(stack);
                int modifiedCapacity = GunModifierHelper.getModifiedAmmoCapacity(stack, gun);

                if (currentAmmo > modifiedCapacity) {
                    GunStackData.setAmmoCount(stack, modifiedCapacity);
                    if (!hasCreativeBox) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(gun.getCurrentAmmoItem(stack));
                        Item item = BuiltInRegistries.ITEM.get(id);
                        if (item != null) {
                            int residue = currentAmmo - modifiedCapacity;
                            spawnAmmo(player, new ItemStack(item, residue));
                        }
                    }
                }
            }
        }
    }
    private static void spawnAmmo(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (stack.getCount() > 0) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
        }
    }

}