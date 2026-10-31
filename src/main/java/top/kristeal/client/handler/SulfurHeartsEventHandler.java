package top.kristeal.client.handler;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.init.ModEffects;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class SulfurHeartsEventHandler {

    private static final ResourceLocation SULFUR_FULL = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_full.png");
    private static final ResourceLocation SULFUR_HALF = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_half.png");
    private static final ResourceLocation SULFUR_FULL_BLINKING = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_full_blinking.png");
    private static final ResourceLocation SULFUR_HALF_BLINKING = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_half_blinking.png");
    private static final ResourceLocation SULFUR_FULL_HARDCORE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_full_hardcore.png");
    private static final ResourceLocation SULFUR_HALF_HARDCORE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_half_hardcore.png");
    private static final ResourceLocation SULFUR_FULL_BLINKING_HARDCORE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_full_blinking_hardcore.png");
    private static final ResourceLocation SULFUR_HALF_BLINKING_HARDCORE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/heart/sulfur_poisoned_half_blinking_hardcore.png");

    // Port note: the reference drew on RenderGuiEvent.Post. 1.21 GUI layers are drawn at increasing depth, so a Post draw at
    // depth 0 ended up hidden under the vanilla hearts. It is now its own layer directly above the vanilla health layer.
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Reference.id("sulfur_hearts"), SulfurHeartsEventHandler::onRenderGui);
    }

    private static void onRenderGui(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // Fix: the vanilla hearts are hidden in creative and spectator, so these must be too
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }

        MobEffectInstance sulfurEffect = player.getEffect(ModEffects.SULFUR_POISONING);
        if (sulfurEffect == null) {
            return;
        }

        int amplifier = sulfurEffect.getAmplifier();
        int duration = sulfurEffect.getDuration();

        float alpha = Math.min(1.0f, duration / 100.0f);
        alpha = Math.min(1.0f, alpha + (amplifier * 0.2f));

        if (alpha <= 0.1f) return;

        int health = Mth.ceil(player.getHealth());
        int maxHealth = Mth.ceil(player.getMaxHealth());

        if (health <= 0 || maxHealth <= 0) return;

        boolean isHardcore = player.level().getLevelData().isHardcore();
        long currentTime = System.currentTimeMillis();
        boolean isBlinking = (currentTime / 250) % 2 == 0;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int left = screenWidth / 2 - 91;
        int top = screenHeight - 39;

        guiGraphics.pose().pushPose();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);

        renderSulfurHeartOverlay(guiGraphics, left, top, health, maxHealth, isHardcore, isBlinking);

        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderSulfurHeartOverlay(GuiGraphics guiGraphics, int x, int y, int health, int maxHealth, boolean hardcore, boolean blinking) {
        int heartsPerRow = 10;
        int fullHearts = health / 2;
        boolean hasHalfHeart = (health % 2) == 1;

        for (int row = 0; row < (maxHealth + 19) / 20; row++) {
            int heartsInThisRow = Math.min(heartsPerRow, (maxHealth / 2) - row * heartsPerRow);
            if (heartsInThisRow <= 0) break;
            for (int col = 0; col < heartsInThisRow; col++) {
                int heartIndex = row * heartsPerRow + col;
                int heartX = x + col * 8;
                int heartY = y - row * 10;
                if (heartIndex >= fullHearts && !(heartIndex == fullHearts && hasHalfHeart)) {
                    continue;
                }
                ResourceLocation textureToUse = getSulfurHeartTexture(heartIndex, fullHearts, hasHalfHeart, hardcore, blinking);
                guiGraphics.blit(textureToUse, heartX, heartY, 0, 0, 9, 9, 9, 9);
            }
        }
    }

    private static ResourceLocation getSulfurHeartTexture(int heartIndex, int fullHearts, boolean hasHalfHeart, boolean hardcore, boolean blinking) {
        boolean isFullHeart = heartIndex < fullHearts;
        boolean isHalfHeart = heartIndex == fullHearts && hasHalfHeart;

        if (isFullHeart) {
            if (hardcore) {
                return blinking ? SULFUR_FULL_BLINKING_HARDCORE : SULFUR_FULL_HARDCORE;
            } else {
                return blinking ? SULFUR_FULL_BLINKING : SULFUR_FULL;
            }
        } else if (isHalfHeart) {
            if (hardcore) {
                return blinking ? SULFUR_HALF_BLINKING_HARDCORE : SULFUR_HALF_HARDCORE;
            } else {
                return blinking ? SULFUR_HALF_BLINKING : SULFUR_HALF;
            }
        }
        return SULFUR_FULL;
    }
}