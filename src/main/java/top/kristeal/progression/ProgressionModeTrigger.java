package top.kristeal.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;

import java.util.Optional;

/**
 * Advancement trigger {@code scguns:progression_mode}: granted while the world runs in the given mode. The root of
 * each progression's advancement tab uses it, so the Arsenal tab appears in Research mode and the Raids tab in Raid
 * mode. Fired on login, after datapack reloads and when the mode changes.
 */
public class ProgressionModeTrigger extends SimpleCriterionTrigger<ProgressionModeTrigger.Instance>
{
    public static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(Registries.TRIGGER_TYPE, Reference.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, ProgressionModeTrigger> INSTANCE = REGISTER.register("progression_mode", ProgressionModeTrigger::new);

    public static void register(IEventBus bus)
    {
        REGISTER.register(bus);
    }

    @Override
    public Codec<Instance> codec()
    {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player)
    {
        ProgressionMode mode = ProgressionMode.get();
        this.trigger(player, instance -> instance.mode().mode == mode);
    }

    public enum Mode implements StringRepresentable
    {
        RAID(ProgressionMode.RAID), RESEARCH(ProgressionMode.RESEARCH);

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final ProgressionMode mode;

        Mode(ProgressionMode mode)
        {
            this.mode = mode;
        }

        @Override
        public String getSerializedName()
        {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Instance(Optional<ContextAwarePredicate> player, Mode mode) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Mode.CODEC.fieldOf("mode").forGetter(Instance::mode)
        ).apply(instance, Instance::new));
    }
}
