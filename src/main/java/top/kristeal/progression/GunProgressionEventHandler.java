package top.kristeal.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.kristeal.Config;
import top.kristeal.Reference;
import top.kristeal.config.RaidConfig;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Unlocks gun tiers the reference way: picking up, crafting or taking from the Gun Bench an item in a tier tag, and on
 * login, any such item in the inventory. In Research mode, getting a blueprint unlocks its tier too.
 * <p>
 * Port notes: the inventory is also scanned every five  seconds, so items from trades, chests, commands or the creative
 * menu count without a rejoin (the reference only scanned on login). The reference read the tier-message option from
 * the client config on the server (so it only worked in singleplayer); it is the server option
 * {@code progression.showTierMessages} here. Death is handled by the attachment's copy-on-death.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunProgressionEventHandler
{
    private static final int INVENTORY_SCAN_INTERVAL = 100;

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event)
    {
        if (event.getPlayer() instanceof ServerPlayer player)
        {
            check(player, event.getOriginalStack());
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            check(player, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        scanInventory(player);
        // Tier advancements for a tier reached before they existed (or before a mode switch)
        ProgressionTriggers.GUN_TIER.get().trigger(player, PlayerGunProgression.getTier(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % INVENTORY_SCAN_INTERVAL == 0)
        {
            scanInventory(player);
        }
    }

    /** A gun was taken from the Gun Bench (the reference unlocked the tier in its result slot). */
    public static void onGunObtained(ServerPlayer player, ItemStack stack)
    {
        check(player, stack);
    }

    private static void check(ServerPlayer player, ItemStack stack)
    {
        GunTier tier = PlayerGunProgression.unlockFromItem(player, stack);
        if (tier == null && blueprintsGrantTier())
        {
            tier = PlayerGunProgression.unlockFromBlueprint(player, stack);
        }
        if (tier != null)
        {
            sendTierUnlockedMessage(player, tier);
        }
    }

    private static void scanInventory(ServerPlayer player)
    {
        boolean blueprints = blueprintsGrantTier();
        GunTier unlocked = null;
        for (ItemStack stack : player.getInventory().items)
        {
            GunTier tier = PlayerGunProgression.unlockFromItem(player, stack);
            if (tier == null && blueprints)
            {
                tier = PlayerGunProgression.unlockFromBlueprint(player, stack);
            }
            if (tier != null)
            {
                unlocked = tier;
            }
        }
        if (unlocked != null)
        {
            sendTierUnlockedMessage(player, unlocked);
        }
    }

    private static boolean blueprintsGrantTier()
    {
        return ProgressionMode.isResearch() && Config.SERVER.progression.blueprintsGrantTier.get();
    }

    public static void sendTierUnlockedMessage(Player player, @Nullable GunTier tier)
    {
        if (tier == null || tier.level() == 0 || !Config.SERVER.progression.showTierMessages.get())
        {
            return;
        }

        player.sendSystemMessage(Component.translatable("progression.scguns.tier_unlocked",
                tier.displayName().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).withStyle(ChatFormatting.YELLOW));

        List<GunTier> mobTiers = tier.getAvailableMobTiers();
        if (!mobTiers.isEmpty())
        {
            player.sendSystemMessage(join(Component.translatable("progression.scguns.enemies_can_spawn").withStyle(ChatFormatting.GRAY),
                    mobTiers.stream().map(t -> t.displayName().withStyle(ChatFormatting.RED)).toList()));
        }

        if (tier.raidLevel() > 0 && RaidConfig.nightlyRaidsActive())
        {
            List<RaidConfig.RaidData> raids = RaidConfig.getRaidsForLevel(tier.raidLevel());
            if (!raids.isEmpty())
            {
                player.sendSystemMessage(join(Component.translatable("progression.scguns.raids_can_spawn").withStyle(ChatFormatting.DARK_GRAY),
                        raids.stream().map(raid -> Component.translatable("raid.scguns." + raid.raidId()).withStyle(ChatFormatting.DARK_RED)).toList()));
            }
        }
    }

    private static Component join(MutableComponent head, List<MutableComponent> parts)
    {
        MutableComponent message = head.append(" ");
        for (int i = 0; i < parts.size(); i++)
        {
            message.append(parts.get(i));
            if (i < parts.size() - 1)
            {
                message.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
        }
        return message;
    }
}
