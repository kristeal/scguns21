package top.kristeal.client.handler;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.init.ModEffects;

/**
 * Port note: replaces the reference's mixin/client/GameRendererMixin, which drew the white blind overlay
 * in GameRenderer#render right before the GUI. Registered below every vanilla GUI layer, so the HUD stays on top as before.
 * Unlike the mixin, GUI layers are skipped while the HUD is hidden.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class BlindOverlayHandler {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(Reference.id("blind_overlay"), BlindOverlayHandler::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null)
        {
            return;
        }

        MobEffectInstance effect = player.getEffect(ModEffects.BLINDED);
        if (effect != null)
        {
            // Render white screen-filling overlay at full alpha effect when duration is above threshold
            // When below threshold, fade to full transparency as duration approaches 0
            float percent = Math.min((effect.getDuration() / (float) Config.SERVER.alphaFadeThreshold.get()), 1);
            guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), ((int) (percent * Config.SERVER.alphaOverlay.get() + 0.5) << 24) | 16777215);
        }
    }
}
