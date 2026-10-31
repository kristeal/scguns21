package top.kristeal.entity.raid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.ChatFormatting;
import top.kristeal.Config;
import top.kristeal.config.RaidConfig;
import top.kristeal.progression.PlayerGunProgression;

import java.util.List;

import java.util.stream.Collectors;

/**
 * Operator commands for testing raids:
 * <ul>
 *     <li>{@code /scguns raid list}: the configured raid ids</li>
 *     <li>{@code /scguns raid start <id>}: start that raid around the player who runs it</li>
 *     <li>{@code /scguns raid stop}: remove the running raid in this dimension</li>
 *     <li>{@code /scguns raid status}: the running raid and tonight's scheduled raid in this dimension</li>
 *     <li>{@code /scguns raid listall}: the nightly raids the player's raid level unlocks</li>
 *     <li>{@code /scguns raid startnext}: start a raid of the player's raid level now (as a nightly raid would pick it)</li>
 *     <li>{@code /scguns raid schedule}: schedule tonight's raid for the player now (skips the nightly chance)</li>
 *     <li>{@code /scguns raid cancel}: drop tonight's scheduled raid</li>
 * </ul>
 */
public final class RaidCommand
{
    private static final SuggestionProvider<CommandSourceStack> RAID_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(RaidConfig.getAllRaids().stream().map(RaidConfig.RaidData::raidId), builder);

    private RaidCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("scguns")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("raid")
                        .then(Commands.literal("list").executes(RaidCommand::list))
                        .then(Commands.literal("start")
                                .then(Commands.argument("raid", StringArgumentType.word())
                                        .suggests(RAID_SUGGESTIONS)
                                        .executes(RaidCommand::start)))
                        .then(Commands.literal("stop").executes(RaidCommand::stop))
                        .then(Commands.literal("status").executes(RaidCommand::status))
                        .then(Commands.literal("listall").executes(RaidCommand::listAll))
                        .then(Commands.literal("startnext").executes(RaidCommand::startNext))
                        .then(Commands.literal("schedule").executes(RaidCommand::schedule))
                        .then(Commands.literal("cancel").executes(RaidCommand::cancel))));
    }

    private static int list(CommandContext<CommandSourceStack> context)
    {
        String ids = RaidConfig.getAllRaids().stream().map(RaidConfig.RaidData::raidId).collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.available", ids), false);
        return RaidConfig.getAllRaids().size();
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        String raidId = StringArgumentType.getString(context, "raid");
        RaidConfig.RaidData config = RaidConfig.getRaidById(raidId);
        if (config == null)
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.unknown", raidId)).create();
        }

        String reason = reasonKey(RaidManager.get(level).startRaidFromPlayer(config, level, player));

        if (reason != null)
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.cannot_start", Component.translatable(reason))).create();
        }

        source.sendSuccess(() -> Component.translatable("commands.scguns.raid.started", raidId), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerLevel level = context.getSource().getLevel();
        if (!RaidManager.stopRaid(level))
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.none_active")).create();
        }

        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.stopped", 1), true);
        return 1;
    }

    private static int listAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int raidLevel = PlayerGunProgression.getRaidLevel(player);
        List<RaidConfig.RaidData> raids = RaidConfig.getRaidsForLevel(raidLevel);
        String ids = raids.isEmpty() ? "-" : raids.stream().map(raid -> raid.raidId() + " (" + raid.raidLevel() + ")").collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.available_for_level", raidLevel, ids), false);
        return raids.size();
    }

    private static int startNext(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        RaidConfig.RaidData config = RaidManager.selectRaidForLevel(PlayerGunProgression.getRaidLevel(player), level.dimension().location(), level.random);
        if (config == null)
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.none_for_level")).create();
        }
        String reason = reasonKey(RaidManager.get(level).startRaidFromPlayer(config, level, player));
        if (reason != null)
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.cannot_start", Component.translatable(reason))).create();
        }
        source.sendSuccess(() -> Component.translatable("commands.scguns.raid.started", config.raidId()), true);
        return 1;
    }

    private static int schedule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        RaidConfig.RaidData config = RaidManager.selectRaidForLevel(PlayerGunProgression.getRaidLevel(player), level.dimension().location(), level.random);
        if (config == null)
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.none_for_level")).create();
        }
        long day = level.getDayTime() / 24000;
        if (level.getDayTime() % 24000 >= Config.COMMON.raids.raidSpawnTime.get())
        {
            day++; // tonight's raid time has passed
        }
        RaidSaveData.get(level).scheduleRaid(level.dimension().location(), player.getUUID(), config.raidId(), day);
        player.sendSystemMessage(Component.translatable("raid.scguns.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        source.sendSuccess(() -> Component.translatable("commands.scguns.raid.scheduled", config.raidId(), player.getDisplayName()), true);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (!RaidManager.cancelScheduledRaid(context.getSource().getLevel()))
        {
            throw new SimpleCommandExceptionType(Component.translatable("commands.scguns.raid.none_scheduled")).create();
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.cancelled"), true);
        return 1;
    }

    private static String reasonKey(RaidManager.StartResult result)
    {
        return switch (result)
        {
            case STARTED -> null;
            case ALREADY_ACTIVE -> "commands.scguns.raid.already_active";
            case NO_LOCATION -> "commands.scguns.raid.no_location";
            case DISABLED -> "commands.scguns.raid.disabled";
            case FAILED -> "commands.scguns.raid.failed";
        };
    }

    private static int status(CommandContext<CommandSourceStack> context)
    {
        ServerLevel statusLevel = context.getSource().getLevel();
        RaidSaveData.ScheduledRaidData scheduled = RaidSaveData.get(statusLevel).getScheduledRaid(statusLevel.dimension().location());
        if (scheduled != null)
        {
            context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.scheduled_status", scheduled.raidId(), scheduled.scheduledDay()), false);
        }

        ActiveRaid raid = RaidManager.get(context.getSource().getLevel()).getCurrentActiveRaid();
        if (raid == null)
        {
            context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.list_none"), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.list_header"), false);
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.raid.list_entry",
                raid.getConfig().raidId(), raid.getAliveHenchmenCount(), raid.getRaidDuration() / 20), false);
        return 1;
    }
}
