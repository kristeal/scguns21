package top.kristeal.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import top.kristeal.Reference;
import top.kristeal.item.GunItem;

import java.util.List;

/**
 * Research progression: an item sub-predicate on a blueprint's research, used in
 * {@code inventory_changed} advancements as {@code "predicates": {"scguns:research": {"progress": "..."}}}.
 * <ul>
 *     <li>{@code prototype}: some gun entry has reached the prototype threshold;</li>
 *     <li>{@code researched}: some entry is fully researched;</li>
 *     <li>{@code complete}: every entry of the blueprint is fully researched.</li>
 * </ul>
 * Research is written to the blueprint inside the Research Station, so these fire when the player takes it out.
 */
public record ResearchItemPredicate(Progress progress) implements ItemSubPredicate {
    public static final Codec<ResearchItemPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Progress.CODEC.fieldOf("progress").forGetter(ResearchItemPredicate::progress)
    ).apply(instance, ResearchItemPredicate::new));

    public static final DeferredRegister<ItemSubPredicate.Type<?>> REGISTER =
            DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, Reference.MOD_ID);
    public static final DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<ResearchItemPredicate>> TYPE =
            REGISTER.register("research", () -> new ItemSubPredicate.Type<>(CODEC));

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }

    @Override
    public boolean matches(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || stack.isEmpty()) {
            return false;
        }
        List<ResearchRecipe> entries = ResearchHelper.getRecipesFor(server.getRecipeManager(), stack.getItem());
        if (entries.isEmpty()) {
            return false;
        }
        return switch (progress) {
            case PROTOTYPE -> entries.stream().anyMatch(entry -> entry.getEntry() instanceof GunItem
                    && ResearchHelper.getState(stack, entry) != ResearchHelper.State.LOCKED);
            case RESEARCHED -> entries.stream().anyMatch(entry -> ResearchHelper.getState(stack, entry) == ResearchHelper.State.RESEARCHED);
            case COMPLETE -> entries.stream().allMatch(entry -> ResearchHelper.getState(stack, entry) == ResearchHelper.State.RESEARCHED);
        };
    }

    public enum Progress implements StringRepresentable {
        PROTOTYPE("prototype"),
        RESEARCHED("researched"),
        COMPLETE("complete");

        public static final Codec<Progress> CODEC = StringRepresentable.fromEnum(Progress::values);

        private final String name;

        Progress(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
