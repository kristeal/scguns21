package top.kristeal.client.handler;

import top.kristeal.common.GunStackData;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import top.kristeal.Config;
import top.kristeal.ScorchedGuns;
import top.kristeal.client.KeyBinds;
import top.kristeal.client.util.PropertyHelper;
import top.kristeal.common.GripType;
import top.kristeal.common.Gun;
import top.kristeal.common.ReloadType;
import top.kristeal.compat.PlayerReviveHelper; // PlayerRevive compat
import top.kristeal.debug.Debug;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.C2SMessageAim;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Author: MrCrayfish
 */
public class AimingHandler
{
    private static AimingHandler instance;

    public static AimingHandler get()
    {
        if(instance == null)
        {
            instance = new AimingHandler();
        }
        return instance;
    }

    private static final double MAX_AIM_PROGRESS = 5;
    private final AimTracker localTracker = new AimTracker();
    private final Map<Player, AimTracker> aimingMap = new WeakHashMap<>();
    private double normalisedAdsProgress;
    public boolean aiming = false;
    private boolean wasKeyPressed = false;

    private AimingHandler() {}

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event)
    {
        Player player = event.getEntity();
        AimTracker tracker = getAimTracker(player);
        if(tracker != null)
        {
            tracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            if(!tracker.isAiming())
            {
                this.aimingMap.remove(player);
            }
        }
    }

    @Nullable
    private AimTracker getAimTracker(Player player)
    {
        if(ModSyncedDataKeys.AIMING.getValue(player) && !this.aimingMap.containsKey(player))
        {
            this.aimingMap.put(player, new AimTracker());
        }
        return this.aimingMap.get(player);
    }

    public float getAimProgress(Player player, float partialTicks)
    {
        if(player.isLocalPlayer())
        {
            return (float) this.localTracker.getNormalProgress(partialTicks);
        }

        AimTracker tracker = this.getAimTracker(player);
        if(tracker != null)
        {
            return (float) tracker.getNormalProgress(partialTicks);
        }
        return 0F;
    }


    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event)
    {

        Player player = Minecraft.getInstance().player;
        if(player == null)
            return;

        ItemStack heldItem = player.getMainHandItem();


        boolean isReloading = ModSyncedDataKeys.RELOADING.getValue(player);
        boolean inCriticalPhase = false;

        if (heldItem.getItem() instanceof GunItem) {
            CompoundTag tag = GunStackData.liveState(heldItem);
            inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");
            Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);

            if (!isReloading && !inCriticalPhase && heldItem.getItem() instanceof AnimatedGunItem animatedGun) {
                String reloadState = tag.getString("scguns:ReloadState");
                if (reloadState.equals("STOPPING") && !tag.getBoolean("scguns:IsPlayingReloadStop")) {
                    animatedGun.cleanupReloadState(tag);
                }

                if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                    if (tag.getBoolean("IsManualReload") ||
                            tag.getBoolean("InReloadLoop") ||
                            tag.contains("LastReloadStateChange") ||
                            tag.contains("ManualReloadInitialized")) {
                        tag.remove("IsManualReload");
                        tag.remove("InReloadLoop");
                        tag.remove("PendingStopTransition");
                        tag.remove("PendingStopTime");
                        tag.remove("LastReloadStateChange");
                        tag.remove("ManualReloadInitialized");
                        tag.remove("scguns:ReloadState");
                    }
                }
            }

            if (inCriticalPhase && gun.getReloads().getReloadType() != ReloadType.MANUAL) {
                this.aiming = false;
                wasKeyPressed = KeyBinds.getAimMapping().isDown();
                if(ModSyncedDataKeys.AIMING.getValue(player)) {
                    ModSyncedDataKeys.AIMING.setValue(player, false);
                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
                }
                this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
                return;
            }
        }

        if(isReloading || inCriticalPhase) {
            this.aiming = false;
            wasKeyPressed = KeyBinds.getAimMapping().isDown();
            if(ModSyncedDataKeys.AIMING.getValue(player)) {
                ModSyncedDataKeys.AIMING.setValue(player, false);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
            }
            this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            return;
        }

        boolean currentKeyPressed = KeyBinds.getAimMapping().isDown();


        boolean toggleAdsEnabled = Config.COMMON.gameplay.toggleADS.get();

        if(toggleAdsEnabled)
        {
            if(currentKeyPressed && !wasKeyPressed)
            {
                this.aiming = !this.aiming;
            }
        }
        else
        {
            this.aiming = currentKeyPressed;
        }
        wasKeyPressed = currentKeyPressed;

        boolean shouldBeAiming = this.aiming;

        if(shouldBeAiming) {
            if(heldItem.getItem() instanceof GunItem) {
                Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);
                GripType gripType = gun.getGeneral().getGripType(heldItem);

                if(!gun.canAimDownSight()) {
                    shouldBeAiming = false;
                    this.aiming = false;
                }
                else if(gripType == GripType.ONE_HANDED && !player.getOffhandItem().isEmpty()) {
                    shouldBeAiming = false;
                    this.aiming = false;
                }
            }
            else {
                shouldBeAiming = false;
                this.aiming = false;
            }
        }

        if(shouldBeAiming)
        {
            if(!ModSyncedDataKeys.AIMING.getValue(player))
            {
                ModSyncedDataKeys.AIMING.setValue(player, true);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(true));
            }
        }
        else if(ModSyncedDataKeys.AIMING.getValue(player))
        {
            ModSyncedDataKeys.AIMING.setValue(player, false);
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
        }

        this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }


    public boolean isAiming() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null)
            return false;

        if(mc.player.isSpectator())
            return false;

        if(ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
            return false;
        }

        ItemStack heldItem = mc.player.getMainHandItem();
        if(heldItem.getItem() instanceof GunItem) {
            CompoundTag tag = GunStackData.liveState(heldItem);

            if(tag.contains("OldScopeType")) {
                tag.remove("OldScopeType");
            }
            if(tag.contains("ScopeCacheTime")) {
                long cacheTime = tag.getLong("ScopeCacheTime");
                if(System.currentTimeMillis() - cacheTime > 5000) {
                    tag.remove("ScopeCacheTime");
                }
            }

            String reloadState = tag.getString("scguns:ReloadState");
            if(!reloadState.isEmpty() && !reloadState.equals("NONE")) {
                return false;
            }
        }

        if(Debug.isForceAim())
            return true;

        if(mc.screen != null) {
            this.aiming = false;
            return false;
        }

        if(PlayerReviveHelper.isBleeding(mc.player) /* Batch 31 */) {
            this.aiming = false;
            return false;
        }

        if(!(heldItem.getItem() instanceof GunItem)) {
            this.aiming = false;
            return false;
        }

        Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);
        if(!gun.canAimDownSight()) {
            this.aiming = false;
            return false;
        }
        if(heldItem.getItem() instanceof AnimatedGunItem) {
            CompoundTag tag = GunStackData.liveState(heldItem);
            if(tag.getBoolean("IsDrawing") && tag.getInt("DrawnTick") < 15) {
                this.aiming = false;
                return false;
            }
        }
        GripType gripType = gun.getGeneral().getGripType(heldItem);
        if(gripType == GripType.ONE_HANDED && !mc.player.getOffhandItem().isEmpty()) {
            this.aiming = false;
            return false;
        }

        if(!this.localTracker.isAiming() && this.isLookingAtInteractableBlock()) {
            this.aiming = false;
            return false;
        }

        return this.aiming;
    }

    @SubscribeEvent
    public void onFovUpdate(ViewportEvent.ComputeFov event)
    {
        if(!event.usedConfiguredFov())
            return;

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.player.getMainHandItem().isEmpty() || mc.options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        ItemStack heldItem = mc.player.getMainHandItem();
        if(!(heldItem.getItem() instanceof GunItem gunItem))
            return;

        if(AimingHandler.get().getNormalisedAdsProgress() == 0)
            return;

        if(ModSyncedDataKeys.RELOADING.getValue(mc.player))
            return;

        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
        if(modifiedGun.getModules().getZoom() == null)
            return;

        double time = PropertyHelper.getSightAnimations(heldItem, modifiedGun).getFovCurve().apply(this.normalisedAdsProgress);
        float modifier = Gun.getFovModifier(heldItem, modifiedGun);
        modifier = (1.0F - modifier) * (float) time;
        event.setFOV(event.getFOV() - event.getFOV() * modifier);
    }

    /**
     * Replaces the reference's mixin/client/MouseHandlerMixin. The mixin multiplied the
     * {@code sensitivity * 0.6 + 0.2} base in MouseHandler#turnPlayer (the third double local, ordinal 2) by the ADS
     * factor below. The event sets the raw sensitivity instead, so it is solved for the same base value.
     */
    @SubscribeEvent
    public void onCalculatePlayerTurn(CalculatePlayerTurnEvent event)
    {
        float additionalAdsSensitivity = 1.0F;
        Minecraft mc = Minecraft.getInstance();
        if(mc.player != null && !mc.player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON)
        {
            ItemStack heldItem = mc.player.getMainHandItem();
            if(heldItem.getItem() instanceof GunItem gunItem)
            {
                if(this.isAiming() && !ModSyncedDataKeys.RELOADING.getValue(mc.player))
                {
                    Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                    if(modifiedGun.getModules().getZoom() != null)
                    {
                        float modifier = Gun.getFovModifier(heldItem, modifiedGun);
                        modifier = Mth.clamp(modifier, 0.1F, 10.0F);

                        additionalAdsSensitivity = Mth.clamp((float) Math.pow(modifier, 0.25), 0.5F, 1.0F);
                    }
                }
            }
        }
        double adsSensitivity = Config.CLIENT.controls.aimDownSightSensitivity.get();
        double factor = (1.0 - (1.0 - adsSensitivity) * this.getNormalisedAdsProgress()) * additionalAdsSensitivity;
        double base = event.getMouseSensitivity() * 0.6F + 0.2F;
        event.setMouseSensitivity((base * factor - 0.2F) / 0.6F);
    }

    @SubscribeEvent
    public void onClientTick(ClientPlayerNetworkEvent.LoggingOut event)
    {
        this.aimingMap.clear();

    }

    /**
     * Prevents the crosshair from rendering when aiming down sight
     */
    @SubscribeEvent(receiveCanceled = true)
    public void onRenderOverlay(RenderGuiEvent.Pre event) // Port note: RenderGuiOverlayEvent (fired per overlay) -> once per frame
    {
        this.normalisedAdsProgress = this.localTracker.getNormalProgress(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    public boolean isZooming()
    {
        return this.aiming;
    }



    public boolean isLookingAtInteractableBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.hitResult != null && mc.level != null)
        {
            if(mc.hitResult instanceof BlockHitResult result)
            {
                BlockState state = mc.level.getBlockState(result.getBlockPos());
                Block block = state.getBlock();
                // Forge should add a tag for intractable blocks so modders can know which blocks can be interacted with :)
            }
            else if(mc.hitResult instanceof EntityHitResult result)
            {
                return result.getEntity() instanceof ItemFrame;
            }
        }
        return false;
    }

    public double getNormalisedAdsProgress()
    {
        return this.normalisedAdsProgress;
    }

    public class AimTracker
    {
        private double currentAim;
        private double previousAim;

        private void handleAiming(Player player, ItemStack heldItem)
        {
            this.previousAim = this.currentAim;
            if(ModSyncedDataKeys.AIMING.getValue(player) || (player.isLocalPlayer() && AimingHandler.this.isAiming()))
            {
                if(this.currentAim < MAX_AIM_PROGRESS)
                {
                    double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
                    speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
                    this.currentAim += speed;
                    if(this.currentAim > MAX_AIM_PROGRESS)
                    {
                        this.currentAim = (int) MAX_AIM_PROGRESS;
                    }
                }
            }
            else
            {
                if(this.currentAim > 0)
                {
                    double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
                    speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
                    this.currentAim -= speed;
                    if(this.currentAim < 0)
                    {
                        this.currentAim = 0;
                    }
                }
            }
        }

        public boolean isAiming()
        {
            return this.currentAim != 0 || this.previousAim != 0;
        }

        public double getNormalProgress(float partialTicks)
        {
            return Mth.clamp((this.previousAim + (this.currentAim - this.previousAim) * partialTicks) / MAX_AIM_PROGRESS, 0.0, 1.0);
        }
    }
}