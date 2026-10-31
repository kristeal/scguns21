package top.kristeal.progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Operator commands for gun tiers and the progression mode (the reference's {@code /scguns progression} subcommands,
 * plus {@code mode} and {@code raidlevel}):
 * <ul>
 *     <li>{@code /scguns progression set <player> <tier>}</li>
 *     <li>{@code /scguns progression clear <player>}</li>
 *     <li>{@code /scguns progression check [player]}</li>
 *     <li>{@code /scguns progression raidlevel <player> <level>}</li>
 *     <li>{@code /scguns progression mode [raid|research]}: show or switch the world's progression</li>
 * </ul>
 */
public final class ProgressionCommand
{
    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(GunTierManager.getAllTiers().stream().map(GunTier::id), builder);
    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ProgressionMode.values()).map(m -> m.name().toLowerCase(Locale.ROOT)), builder);

    private ProgressionCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("scguns")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("progression")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("tier", StringArgumentType.string())
                                                .suggests(TIER_SUGGESTIONS)
                                                .executes(ProgressionCommand::set))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ProgressionCommand::clear)))
                        .then(Commands.literal("check")
                                .executes(context -> check(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> check(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("raidlevel")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(ProgressionCommand::raidLevel))))
                        .then(Commands.literal("mode")
                                .executes(ProgressionCommand::showMode)
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(MODE_SUGGESTIONS)
                                        .executes(ProgressionCommand::setMode)))));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String tierId = StringArgumentType.getString(context, "tier");
        GunTier tier = GunTierManager.getTier(tierId);
        if (tier == GunTier.NONE && !tierId.equals(GunTier.NONE.id()))
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.progression.invalid_tier", tierId)).create();
        }

        PlayerGunProgression.setTier(player, tier);
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.progression.set",
                player.getDisplayName(), tier.displayName().withStyle(ChatFormatting.GOLD)), true);
        GunProgressionEventHandler.sendTierUnlockedMessage(player, tier);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PlayerGunProgression.setTier(player, null);
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.progression.cleared", player.getDisplayName()), true);
        player.sendSystemMessage(Component.translatable("commands.scguns.progression.reset").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int check(CommandSourceStack source, ServerPlayer player)
    {
        GunTier tier = PlayerGunProgression.getTier(player);
        List<GunTier> mobTiers = tier.getAvailableMobTiers();
        MutableComponent tierName = tier.displayName().withStyle(ChatFormatting.GOLD);
        if (mobTiers.isEmpty())
        {
            source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check_none", player.getDisplayName(), tierName), false);
        }
        else
        {
            String names = mobTiers.stream().map(t -> t.displayName().getString()).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check", player.getDisplayName(), tierName, names), false);
        }
        int raidLevel = PlayerGunProgression.getRaidLevel(player);
        source.sendSuccess(() -> Component.translatable("commands.scguns.progression.raid_level", raidLevel).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int raidLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int level = IntegerArgumentType.getInteger(context, "level");
        PlayerGunProgression.setRaidLevel(player, level);
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.progression.raid_level_set", player.getDisplayName(), level), true);
        return 1;
    }

    private static int showMode(CommandContext<CommandSourceStack> context)
    {
        ProgressionMode mode = ProgressionMode.get();
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.progression.mode",
                Component.translatable("progression.scguns.mode." + mode.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD)), false);
        return mode.ordinal();
    }

    private static int setMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(context, "mode");
        ProgressionMode mode = Arrays.stream(ProgressionMode.values())
                .filter(m -> m.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("commands.scguns.progression.invalid_mode", name)).create());

        ProgressionMode.set(context.getSource().getServer(), mode);
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.progression.mode_set",
                Component.translatable("progression.scguns.mode." + mode.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD)), true);
        return 1;
    }
}
