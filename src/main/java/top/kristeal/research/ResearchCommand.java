package top.kristeal.research;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.kristeal.item.BlueprintItem;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Research progression: an operator command that edits the research of the blueprint in the main
 * hand, for testing.
 * <ul>
 *     <li>{@code /scguns research complete [entry]}: finish one entry, or all of them</li>
 *     <li>{@code /scguns research reset [entry]}: clear one entry, or all of them</li>
 *     <li>{@code /scguns research set <entry> <percent>}: set an entry's progress (rounded down to whole steps)</li>
 * </ul>
 */
public final class ResearchCommand {
    private static final SimpleCommandExceptionType NOT_A_BLUEPRINT =
            new SimpleCommandExceptionType(Component.translatable("commands.scguns.research.not_a_blueprint"));
    private static final SimpleCommandExceptionType UNKNOWN_ENTRY =
            new SimpleCommandExceptionType(Component.translatable("commands.scguns.research.unknown_entry"));

    private static final SuggestionProvider<CommandSourceStack> ENTRY_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null || !(player.getMainHandItem().getItem() instanceof BlueprintItem)) {
            return builder.buildFuture();
        }
        RecipeManager recipes = context.getSource().getServer().getRecipeManager();
        return SharedSuggestionProvider.suggestResource(ResearchHelper.getRecipesFor(recipes, player.getMainHandItem().getItem())
                .stream().map(ResearchRecipe::getEntryId), builder);
    };

    private ResearchCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scguns")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("research")
                        .then(Commands.literal("complete")
                                .executes(context -> apply(context, null, ResearchRecipe::getSteps))
                                .then(Commands.argument("entry", ResourceLocationArgument.id()).suggests(ENTRY_SUGGESTIONS)
                                        .executes(context -> apply(context, ResourceLocationArgument.getId(context, "entry"), ResearchRecipe::getSteps))))
                        .then(Commands.literal("reset")
                                .executes(context -> apply(context, null, recipe -> 0))
                                .then(Commands.argument("entry", ResourceLocationArgument.id()).suggests(ENTRY_SUGGESTIONS)
                                        .executes(context -> apply(context, ResourceLocationArgument.getId(context, "entry"), recipe -> 0))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("entry", ResourceLocationArgument.id()).suggests(ENTRY_SUGGESTIONS)
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> {
                                                    int percent = IntegerArgumentType.getInteger(context, "percent");
                                                    return apply(context, ResourceLocationArgument.getId(context, "entry"),
                                                            recipe -> recipe.getSteps() * percent / 100);
                                                }))))));
    }

    /** Sets the step count of one entry (or every entry when {@code entryId} is null) on the held blueprint. */
    private static int apply(CommandContext<CommandSourceStack> context, ResourceLocation entryId, ToIntFunction<ResearchRecipe> steps) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack blueprint = player.getMainHandItem();
        if (!(blueprint.getItem() instanceof BlueprintItem)) {
            throw NOT_A_BLUEPRINT.create();
        }

        List<ResearchRecipe> targets = ResearchHelper.getRecipesFor(context.getSource().getServer().getRecipeManager(), blueprint.getItem())
                .stream().filter(recipe -> entryId == null || recipe.getEntryId().equals(entryId)).toList();
        if (targets.isEmpty()) {
            throw UNKNOWN_ENTRY.create();
        }

        for (ResearchRecipe recipe : targets) {
            ResearchHelper.setSteps(blueprint, recipe, steps.applyAsInt(recipe));
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.scguns.research.success", targets.size()), true);
        return targets.size();
    }
}
