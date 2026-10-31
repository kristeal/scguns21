package top.kristeal.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.attributes.SCAttributes;
import top.kristeal.client.handler.ReloadHandler;
import top.kristeal.common.exosuit.ExoSuitAmmoHelper;
import top.kristeal.event.GunEventBus;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.AmmoBoxItem;
import top.kristeal.item.GunItem;
import top.kristeal.item.ammo_boxes.CreativeAmmoBoxItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.S2CMessageGunSound;
import top.kristeal.network.message.S2CMessageStopReload;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import static top.kristeal.common.network.ServerPlayHandler.hasCreativeAmmoBoxInCurios;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ReloadTracker {
    private static final Map<Player, ReloadTracker> RELOAD_TRACKER_MAP = new WeakHashMap<>();

    private final int startTick;
    private final int slot;
    private final ItemStack stack;
    private final Gun gun;

    public ReloadTracker(Player player) {
        this.startTick = player.tickCount;
        this.slot = player.getInventory().selected;
        this.stack = player.getInventory().getSelected();
        this.gun = ((GunItem) stack.getItem()).getModifiedGun(stack);
    }


    private void handleReloadByproduct(Player player) {
        if (this.gun.getReloads().getReloadType() != ReloadType.SINGLE_ITEM) {
            return;
        }

        if (this.gun.getReloads().shouldGiveByproduct(player.level().getRandom(), this.stack)) {
            Item byproduct = this.gun.getReloads().getReloadByproduct();
            if (byproduct != null) {
                ItemStack byproductStack = new ItemStack(byproduct);

                if (GunEventBus.addCasingDirectly(player, byproductStack)) {
                    return;
                }

                boolean added = player.getInventory().add(byproductStack);
                if (!added) {
                    Level level = player.level();
                    double x = player.getX();
                    double y = player.getY();
                    double z = player.getZ();
                    ItemEntity itemEntity = new ItemEntity(level, x, y, z, byproductStack);
                    itemEntity.setDeltaMovement(
                            level.random.nextDouble() * 0.2 - 0.1,
                            0.2,
                            level.random.nextDouble() * 0.2 - 0.1
                    );
                    level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    public boolean isWeaponFull(Player player) {
        ItemStack currentStack = player.getMainHandItem();
        int currentAmmo = GunStackData.getAmmoCount(currentStack); // Port note: AMMO_COUNT component
        int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(currentStack, this.gun);
        return currentAmmo >= maxAmmo;
    }

    private boolean isWeaponEmpty() {
        return GunStackData.getAmmoCount(this.stack) == 0;
    }

    public boolean hasNoAmmo(Player player) {
        boolean result;
        if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
            result = Gun.findAmmo(player, this.gun.getReloads().getReloadItem()).stack().isEmpty();
        } else {
            result = Gun.findAmmo(player, this.gun.getProjectile().getItem()).stack().isEmpty();
        }
        return result;
    }

    private boolean canReload(Player player) {
        if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
            return true;
        }

        int deltaTicks = player.tickCount - this.startTick;
        double reloadSpeed = Objects.requireNonNull(player.getAttribute(SCAttributes.RELOAD_SPEED)).getValue();
        int interval = (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) ?
                (int) Math.ceil((double) GunEnchantmentHelper.getMagReloadSpeed(this.stack) / reloadSpeed) :
                (int) Math.ceil((double) GunEnchantmentHelper.getReloadInterval(this.stack) / reloadSpeed);
        return deltaTicks >= interval;
    }

    public static int ammoInInventory(ItemStack[] ammoStack) {
        int result = 0;
        for (ItemStack x : ammoStack) {
            result += x.getCount();
        }
        return result;
    }

    private void shrinkFromAmmoPool(ItemStack[] ammoStack, Player player, int shrinkAmount) {
        final int[] shrinkAmt = {shrinkAmount};

        int exoSuitShrinkAmount = Math.min(shrinkAmt[0], ExoSuitAmmoHelper.getAmmoCountInExoSuit(player, gun.getProjectile().getItem()));
        if (exoSuitShrinkAmount > 0) {
            ExoSuitAmmoHelper.shrinkAmmoInExoSuit(player, gun.getProjectile().getItem(), exoSuitShrinkAmount);
            shrinkAmt[0] -= exoSuitShrinkAmount;

            if (shrinkAmt[0] == 0) {
                return;
            }
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof AmmoBoxItem) {
                    List<ItemStack> contents = AmmoBoxItem.getContents(stack).collect(Collectors.toList());
                    for (ItemStack pouchAmmoStack : contents) {
                        if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == gun.getProjectile().getItem()) {
                            int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                            pouchAmmoStack.shrink(max);
                            shrinkAmt[0] -= max;
                            if (shrinkAmt[0] == 0) {
                                updateAmmoPouchContents(stack, contents);
                                return;
                            }
                        }
                    }
                    updateAmmoPouchContents(stack, contents);
                }
            }
        });

        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof AmmoBoxItem) {
                List<ItemStack> contents = AmmoBoxItem.getContents(itemStack).collect(Collectors.toList());
                for (ItemStack pouchAmmoStack : contents) {
                    if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == gun.getProjectile().getItem()) {
                        int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                        pouchAmmoStack.shrink(max);
                        shrinkAmt[0] -= max;
                        if (shrinkAmt[0] == 0) {
                            updateAmmoPouchContents(itemStack, contents);
                            return;
                        }
                    }
                }
                updateAmmoPouchContents(itemStack, contents);
            }
        }

        for (ItemStack itemStack : ammoStack) {
            if (shrinkAmt[0] > 0 && !itemStack.isEmpty() && itemStack.getItem() == gun.getProjectile().getItem()) {
                int max = Math.min(shrinkAmt[0], itemStack.getCount());
                itemStack.shrink(max);
                shrinkAmt[0] -= max;
            }
        }
    }

    private void shrinkFromAmmoPool(ItemStack ammoStack, Player player, int shrinkAmount) {
        final int[] shrinkAmt = {shrinkAmount};

        int exoSuitShrinkAmount = Math.min(shrinkAmt[0], ExoSuitAmmoHelper.getAmmoCountInExoSuit(player, gun.getProjectile().getItem()));
        if (exoSuitShrinkAmount > 0) {
            ExoSuitAmmoHelper.shrinkAmmoInExoSuit(player, gun.getProjectile().getItem(), exoSuitShrinkAmount);
            shrinkAmt[0] -= exoSuitShrinkAmount;

            if (shrinkAmt[0] == 0) {
                return;
            }
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof AmmoBoxItem) {
                    List<ItemStack> contents = AmmoBoxItem.getContents(stack).collect(Collectors.toList());
                    for (ItemStack pouchAmmoStack : contents) {
                        if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == gun.getProjectile().getItem()) {
                            int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                            pouchAmmoStack.shrink(max);
                            shrinkAmt[0] -= max;
                            if (shrinkAmt[0] == 0) {
                                updateAmmoPouchContents(stack, contents);
                                return;
                            }
                        }
                    }
                    updateAmmoPouchContents(stack, contents);
                }
            }
        });
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof AmmoBoxItem) {
                List<ItemStack> contents = AmmoBoxItem.getContents(itemStack).collect(Collectors.toList());
                for (ItemStack pouchAmmoStack : contents) {
                    if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == gun.getProjectile().getItem()) {
                        int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                        pouchAmmoStack.shrink(max);
                        shrinkAmt[0] -= max;
                        if (shrinkAmt[0] == 0) {
                            updateAmmoPouchContents(itemStack, contents);
                            return;
                        }
                    }
                }
                updateAmmoPouchContents(itemStack, contents);
            }
        }
        if (shrinkAmt[0] > 0 && !ammoStack.isEmpty() && ammoStack.getItem() == gun.getProjectile().getItem()) {
            int max = Math.min(shrinkAmt[0], ammoStack.getCount());
            ammoStack.shrink(max);
            shrinkAmt[0] -= max;
        }
    }

    // Port note: ammo box contents are the BUNDLE_CONTENTS component; empty stacks are dropped by setContents
    private void updateAmmoPouchContents(ItemStack ammoPouch, List<ItemStack> contents) {
        AmmoBoxItem.setContents(ammoPouch, contents);
    }

    public void increaseMagAmmo(Player player) {
        ItemStack[] ammoStack = Gun.findAmmoStack(player, this.gun.getProjectile().getItem());
        if (ammoStack.length > 0) {
            {
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
                boolean hasCreativeBox = player.getInventory().items.stream()
                        .anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem) ||
                        hasCreativeAmmoBoxInCurios((ServerPlayer) player);
                if (hasCreativeBox) {
                    GunStackData.setAmmoCount(this.stack, maxAmmo);
                    return;
                }
                int currentAmmo = GunStackData.getAmmoCount(this.stack);
                if (currentAmmo < 0 || currentAmmo > maxAmmo) {
                    currentAmmo = 0;
                }
                int ammoAmount = Math.min(ammoInInventory(ammoStack), maxAmmo);
                int amount = maxAmmo - currentAmmo;
                if (ammoAmount < amount) {
                    GunStackData.setAmmoCount(this.stack, currentAmmo + ammoAmount);
                    this.shrinkFromAmmoPool(ammoStack, player, ammoAmount);
                } else {
                    GunStackData.setAmmoCount(this.stack, maxAmmo);
                    this.shrinkFromAmmoPool(ammoStack, player, amount);
                }
            }
        }
        playReloadSound(player);
    }


    public void reloadItem(Player player) {
        Item reloadItem = this.gun.getReloads().getReloadItem();
        ItemStack[] ammoStacks = Gun.findAmmoStack(player, reloadItem);

        if (ammoStacks.length > 0) {
            {
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
                int currentAmmo = GunStackData.getAmmoCount(this.stack);

                if (currentAmmo < maxAmmo) {
                    GunStackData.setAmmoCount(this.stack, maxAmmo);
                    this.shrinkFromAmmoPool(ammoStacks, player, 1);
                    handleReloadByproduct(player);
                }
            }
            playReloadSound(player);
        }
    }

    public void increaseAmmo(Player player) {
        AmmoContext context = Gun.findAmmo(player, this.gun.getProjectile().getItem());
        ItemStack ammo = context.stack();

        if (!ammo.isEmpty()) {
            int amount = Math.min(ammo.getCount(), this.gun.getReloads().getReloadAmount());
            ItemStack currentStack = player.getMainHandItem();
            {
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(currentStack, this.gun);
                int currentAmmo = GunStackData.getAmmoCount(currentStack);
                amount = Math.min(amount, maxAmmo - currentAmmo);
                GunStackData.setAmmoCount(currentStack, currentAmmo + amount);

            }
            shrinkFromAmmoPool(Gun.findAmmoStack(player, this.gun.getProjectile().getItem()), player, amount);
        }

        playReloadSound(player);
    }

    private void playReloadSound(Player player) {
        if (stack.getItem() instanceof AnimatedGunItem) {
            return;
        }
        ResourceLocation reloadSound = this.gun.getSounds().getReload();
        if (reloadSound != null) {
            double radius = Config.SERVER.reloadMaxDistance.get();
            double soundX = player.getX();
            double soundY = player.getY() + 1.0;
            double soundZ = player.getZ();
            S2CMessageGunSound message = new S2CMessageGunSound(reloadSound, SoundSource.PLAYERS, (float) soundX, (float) soundY, (float) soundZ, 1.0F, 1.0F, player.getId(), false, true);
            PacketHandler.getPlayChannel().sendToNearbyPlayers((ServerLevel) player.level(), soundX, soundY, soundZ, radius, message);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        CompoundTag state = GunStackData.getState(held);
        CompoundTag before = state.copy();
        onPlayerTick(player, state);
        if (!state.equals(before) && player.getMainHandItem() == held) {
            GunStackData.setState(held, state);
        }
    }

    // Port note: the reference body, working on the held gun's state copy
    private static void onPlayerTick(Player player, CompoundTag state) {
        if (player.level().isClientSide) {
            return;
        }
        if (ModSyncedDataKeys.RELOADING.getValue(player)) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                Gun gun = gunItem.getModifiedGun(heldItem);

                CompoundTag tag = state;

                if (!(heldItem.getItem().getClass().getPackageName().startsWith("top.kristeal"))) {
                    return;
                }

                ReloadTracker tracker = RELOAD_TRACKER_MAP.get(player);

                boolean needsNewTracker = false;
                if (tracker == null) {
                    needsNewTracker = true;
                } else {
                    ItemStack currentWeapon = player.getInventory().getSelected();
                    int currentSlot = player.getInventory().selected;
                    boolean isActivelyReloading = ModSyncedDataKeys.RELOADING.getValue(player);

                    if (!isActivelyReloading && (tracker.slot != currentSlot ||
                            currentWeapon.isEmpty() ||
                            !currentWeapon.getItem().getClass().equals(tracker.stack.getItem().getClass()))) {
                        needsNewTracker = true;
                        RELOAD_TRACKER_MAP.remove(player);
                    }
                }

                if (needsNewTracker) {
                    if (!(player.getInventory().getSelected().getItem() instanceof GunItem)) {
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        return;
                    }

                    tracker = new ReloadTracker(player);
                    RELOAD_TRACKER_MAP.put(player, tracker);
                }
                boolean weaponFull = tracker.isWeaponFull(player);
                boolean hasNoAmmo = tracker.hasNoAmmo(player);
                if (weaponFull || hasNoAmmo) {

                    if (player.getMainHandItem().getItem() instanceof AnimatedGunItem &&
                            tracker.gun.getReloads().getReloadType() == ReloadType.MANUAL) {

                        if (tag.getBoolean("scguns:ShouldStopAfterLoop")) {
                            long stopTime = tag.getLong("scguns:StopAfterLoopTime");
                            if (stopTime == 0) {
                                tag.putLong("scguns:StopAfterLoopTime", System.currentTimeMillis());
                                return;
                            }
                            if (System.currentTimeMillis() - stopTime < 100) {
                                return;
                            }
                            tag.remove("scguns:ShouldStopAfterLoop");
                            tag.remove("scguns:StopAfterLoopTime");
                        } else {
                            tag.putBoolean("scguns:ShouldStopAfterLoop", true);
                            return;
                        }
                    }
                    RELOAD_TRACKER_MAP.remove(player);
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    tag.remove("IsReloading");
                    tag.remove("scguns:IsReloading");
                    if (player.getMainHandItem().getItem() instanceof AnimatedGunItem) {
                        tag.putString("scguns:ReloadState", "STOPPING");
                        tag.putBoolean("scguns:IsPlayingReloadStop", true);
                        PacketHandler.getPlayChannel().sendToPlayer(() -> (ServerPlayer) player, new S2CMessageStopReload());
                    }
                }
            }
        } else {
            RELOAD_TRACKER_MAP.remove(player);
            state.remove("IsReloading");
        }
    }


    public static void loaded(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem().getClass().getPackageName().startsWith("top.kristeal"))) {
            return;
        }

        if (!ModSyncedDataKeys.RELOADING.getValue(player)) {
            RELOAD_TRACKER_MAP.remove(player);
            GunStackData.updateState(player.getMainHandItem(), t -> t.remove("IsReloading")); // Port note
            return;
        }

        if (!RELOAD_TRACKER_MAP.containsKey(player)) {
            if (!(player.getInventory().getSelected().getItem() instanceof GunItem)) {
                ModSyncedDataKeys.RELOADING.setValue(player, false);
                return;
            }
            RELOAD_TRACKER_MAP.put(player, new ReloadTracker(player));
        }

        ReloadTracker tracker = RELOAD_TRACKER_MAP.get(player);

        boolean weaponChanged = false;
        ItemStack currentWeapon = player.getInventory().getSelected();
        int currentSlot = player.getInventory().selected;
        if (tracker.slot != currentSlot ||
                !currentWeapon.getItem().equals(tracker.stack.getItem()) ||
                currentWeapon.isEmpty()) {
            weaponChanged = true;
        }

        if (weaponChanged ||
                tracker.hasNoAmmo(player) ||
                (tracker.isWeaponFull(player) && tracker.gun.getReloads().getReloadType() != ReloadType.MANUAL)) {
            RELOAD_TRACKER_MAP.remove(player);
            ModSyncedDataKeys.RELOADING.setValue(player, false);
            GunStackData.updateState(player.getMainHandItem(), t -> t.remove("IsReloading")); // Port note
            return;
        }

        Item item = player.getMainHandItem().getItem();
        if (item instanceof GunItem) {
            Gun gun = tracker.gun;
            ReloadType reloadType = gun.getReloads().getReloadType();
            if (!(item instanceof AnimatedGunItem)) {
                if (reloadType == ReloadType.MAG_FED) {
                    tracker.increaseMagAmmo(player);
                } else if (reloadType == ReloadType.SINGLE_ITEM) {
                    tracker.reloadItem(player);
                } else if (reloadType == ReloadType.MANUAL) {
                    tracker.increaseAmmo(player);
                }

                RELOAD_TRACKER_MAP.remove(player);
                ModSyncedDataKeys.RELOADING.setValue(player, false);
                GunStackData.updateState(player.getMainHandItem(), t -> t.remove("IsReloading")); // Port note
                return;
            }
            if (item instanceof AnimatedGunItem gunItem &&
                    item.getClass().getPackageName().startsWith("top.kristeal")) {
                if (reloadType == ReloadType.MANUAL) {
                    tracker.increaseAmmo(player);
                }

                if (tracker.isWeaponFull(player) || tracker.hasNoAmmo(player)) {
                    long id = GeoItem.getId(player.getMainHandItem());
                    AnimationController<GeoAnimatable> animationController = gunItem.getAnimatableInstanceCache()
                            .getManagerForId(id)
                            .getAnimationControllers()
                            .get("controller");

                    animationController.setAnimationSpeed(1.0);
                    animationController.forceAnimationReset();

                    if (reloadType == ReloadType.MANUAL) {
                        ReloadHandler.loaded(player);
                    }
                    tracker.handleReloadByproduct(player);
                    RELOAD_TRACKER_MAP.remove(player);
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    GunStackData.updateState(player.getMainHandItem(), t -> t.remove("IsReloading")); // Port note
                }
            }
        }
    }
}