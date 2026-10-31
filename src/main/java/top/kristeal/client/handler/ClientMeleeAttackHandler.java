package top.kristeal.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimationController;
import top.kristeal.common.GunStackData;
import top.kristeal.common.ReloadType;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.C2SMessageReload;

public class ClientMeleeAttackHandler {
    /** Handles S2CMessageMeleeAttack: the server accepted a stab. */
    public static void onMeleeAttackMessage() {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
            ItemStack heldItem = player.getMainHandItem();
            interruptForMelee(player, gunItem, heldItem);
            startMeleeAnimation(gunItem, heldItem);
        }
    }

    /**
     * A stab interrupts a reload and the inspect animation. The reference did this on the server thread, driving the
     * GeckoLib controllers there and sending a C2S packet from the server.
     */
    public static void interruptForMelee(Player player, GunItem gunItem, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof AnimatedGunItem animatedGunItem)) {
            return;
        }
        AnimationController<GeoAnimatable> controller = animatedGunItem.getAnimatableInstanceCache()
                .getManagerForId(GeoItem.getId(heldItem))
                .getAnimationControllers()
                .get("controller");
        CompoundTag tag = GunStackData.liveState(heldItem);
        if (tag.getBoolean("scguns:IsReloading")) {
            ReloadType reloadType = gunItem.getModifiedGun(heldItem).getReloads().getReloadType();
            if (reloadType == ReloadType.MAG_FED) {
                tag.remove("scguns:IsReloading");
                ModSyncedDataKeys.RELOADING.setValue(player, false);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                if (controller != null) {
                    controller.forceAnimationReset();
                }
            } else if (reloadType == ReloadType.MANUAL && controller != null &&
                    (animatedGunItem.isAnimationPlaying(controller, "reload_loop") ||
                            animatedGunItem.isAnimationPlaying(controller, "reload_start"))) {
                tag.putBoolean("scguns:ReloadComplete", true);
                controller.tryTriggerAnimation("reload_stop");
                tag.remove("scguns:IsReloading");
                ModSyncedDataKeys.RELOADING.setValue(player, false);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
            }
        }
        if (controller != null && animatedGunItem.isAnimationPlaying(controller, "inspect")) {
            controller.tryTriggerAnimation("idle");
        }
    }

    public static void startMeleeAnimation(GunItem gunItem, ItemStack heldItem) {
        if (Minecraft.getInstance().player != null) {
            boolean isOnCooldown = MeleeAttackHandler.isMeleeOnCooldown(Minecraft.getInstance().player, heldItem);
            if (isOnCooldown) {
                return;
            }
            GunRenderingHandler.get().startMeleeAnimation(heldItem);

            if (gunItem.hasBayonet(heldItem)) {
                GunRenderingHandler.get().startBayonetStabAnimation();
            }

            GunRenderingHandler.get().startThirdPersonMeleeAnimation();
            updateMeleeCooldownHUD(gunItem, heldItem);
        }
    }


    /** Shows a cooldown of the given length on the melee indicator (also used after a bayonet charge ends). */
    public static void showMeleeCooldown(int ticks) {
        HUDRenderHandler.isMeleeCooldownActive = true;
        HUDRenderHandler.maxMeleeCooldown = ticks;
        HUDRenderHandler.meleeCooldown = ticks;
    }

    public static void updateMeleeCooldownHUD(GunItem gunItem, ItemStack heldItem) {
        HUDRenderHandler.isMeleeCooldownActive = true;
        HUDRenderHandler.maxMeleeCooldown = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeCooldownTicks();
        HUDRenderHandler.meleeCooldown = HUDRenderHandler.maxMeleeCooldown;
    }


    public static void spawnParticleEffect(Player player, LivingEntity target, ParticleType<?> particleType) {
        if (player.level().isClientSide) {
            ClientLevel clientLevel = (ClientLevel) player.level();
            clientLevel.addParticle((ParticleOptions) particleType, target.getX(), target.getY(), target.getZ(), 0.1D, 0.1D, 0.1D);
        } else {
            ((ServerLevel) player.level()).sendParticles((SimpleParticleType) particleType, target.getX(), target.getY(), target.getZ(), 10, 0.5D, 0.5D, 0.5D, 0.0D);
        }
    }
}
