package top.kristeal.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import top.kristeal.Config;
import top.kristeal.common.Gun;
import top.kristeal.event.GunFireEvent;
import top.kristeal.item.GunItem;
import top.kristeal.util.GunEnchantmentHelper;
import top.kristeal.util.GunModifierHelper;

import java.util.Random;

public class GunRecoilHandler {
    private static GunRecoilHandler instance;
    private final Random random = new Random();
    private double gunRecoilNormal;
    private double gunRecoilAngle;
    private float gunRecoilRandom;
    private float cameraRecoil;
    private float progressCameraRecoil;
    private static int recoilRand;

    public static GunRecoilHandler get() {
        if (instance == null) {
            instance = new GunRecoilHandler();
        }
        return instance;
    }

    private GunRecoilHandler() {
    }

    @SubscribeEvent
    public void preShoot(GunFireEvent.Pre event) {
        if (event.isClient()) {
            if (Config.SERVER.enableCameraRecoil.get()) {
                recoilRand = (new Random()).nextInt(2);
            }
        }
    }

    @SubscribeEvent
    public void onGunFire(GunFireEvent.Post event) {
        if (event.isClient()) {
            if (Config.SERVER.enableCameraRecoil.get()) {
                ItemStack heldItem = event.getStack();
                GunItem gunItem = (GunItem) heldItem.getItem();
                Gun modifiedGun = gunItem.getModifiedGun(heldItem);

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;

                float recoilModifier = 1.0F - GunModifierHelper.getRecoilModifier(heldItem);
                if (mc.player != null) {
                    float enchantmentEffect = GunEnchantmentHelper.getRecoilModifier(mc.player, heldItem);
                    recoilModifier *= enchantmentEffect;
                }
                recoilModifier = (float) ((double) recoilModifier * this.getAdsRecoilReduction(modifiedGun));

                this.cameraRecoil = modifiedGun.getProjectile().getRecoilAngle() * recoilModifier;
                this.progressCameraRecoil = 0.0F;
                this.gunRecoilRandom = this.random.nextFloat();
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderFrameEvent.Post event) {
        if (!(this.cameraRecoil <= 0.0F)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (Config.SERVER.enableCameraRecoil.get()) {
                    float recoilAmount = this.cameraRecoil * mc.getTimer().getGameTimeDeltaTicks() * 0.15F;
                    float startProgress = this.progressCameraRecoil / this.cameraRecoil;
                    float endProgress = (this.progressCameraRecoil + recoilAmount) / this.cameraRecoil;
                    float pitch = mc.player.getXRot();
                    float yaw = mc.player.getYRot();

                    if (startProgress < 0.2F) {
                        mc.player.setXRot(pitch - (endProgress - startProgress) / 0.2F * this.cameraRecoil);
                        if (recoilRand == 1) {
                            mc.player.setYRot(yaw - (endProgress - startProgress) / 0.2F * this.cameraRecoil / 2.0F);
                        } else {
                            mc.player.setYRot(yaw + (endProgress - startProgress) / 0.2F * this.cameraRecoil / 2.0F);
                        }
                    } else {
                        mc.player.setXRot(pitch + (endProgress - startProgress) / 0.8F * this.cameraRecoil);
                        if (recoilRand == 1) {
                            mc.player.setYRot(yaw + (endProgress - startProgress) / 0.8F * this.cameraRecoil / 2.0F);
                        } else {
                            mc.player.setYRot(yaw - (endProgress - startProgress) / 0.8F * this.cameraRecoil / 2.0F);
                        }
                    }

                    this.progressCameraRecoil += recoilAmount;
                    if (this.progressCameraRecoil >= this.cameraRecoil) {
                        this.cameraRecoil = 0.0F;
                        this.progressCameraRecoil = 0.0F;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlay(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            ItemStack heldItem = event.getItemStack();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                assert Minecraft.getInstance().player != null;
                ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
                float cooldown = tracker.getCooldownPercent(gunItem, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
                cooldown = cooldown >= modifiedGun.getGeneral().getRecoilDurationOffset() ?
                        (cooldown - modifiedGun.getGeneral().getRecoilDurationOffset()) / (1.0F - modifiedGun.getGeneral().getRecoilDurationOffset()) : 0.0F;

                float amount;
                if ((double) cooldown >= 0.8) {
                    amount = ((1.0F - cooldown) / 0.2F);
                    this.gunRecoilNormal = 1.0F - --amount * amount * amount * amount;
                } else {
                    amount = cooldown / 0.8F;
                    this.gunRecoilNormal = (double) amount < 0.5 ? (double) (2.0F * amount * amount) : (double) (-1.0F + (4.0F - 2.0F * amount) * amount);
                }

                float baseRecoilAngle = modifiedGun.getProjectile().getRecoilAngle();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    float recoilModifier = 1.0F - GunModifierHelper.getRecoilModifier(heldItem);
                    float enchantmentMultiplier = GunEnchantmentHelper.getRecoilModifier(mc.player, heldItem);
                    recoilModifier *= enchantmentMultiplier;

                    this.gunRecoilAngle = baseRecoilAngle * recoilModifier;
                } else {
                    this.gunRecoilAngle = baseRecoilAngle;
                }
            }
        }
    }

    public double getAdsRecoilReduction(Gun gun) {
        return 1.0 - (double) gun.getGeneral().getRecoilAdsReduction() * AimingHandler.get().getNormalisedAdsProgress();
    }

    public double getGunRecoilNormal() {
        return this.gunRecoilNormal;
    }

    public double getGunRecoilAngle() {
        return this.gunRecoilAngle;
    }

    public float getGunRecoilRandom() {
        return this.gunRecoilRandom;
    }
}