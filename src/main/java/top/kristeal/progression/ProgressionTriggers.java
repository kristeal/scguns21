package top.kristeal.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;

import java.util.Optional;

/**
 * Advancement triggers of the Raid progression:
 * <ul>
 *     <li>{@code scguns:gun_tier} ({@code tier}, {@code min_level}, both optional): the player's gun tier changed;</li>
 *     <li>{@code scguns:raid_won} ({@code raid}, optional): the player was near a raid whose boss was defeated.</li>
 * </ul>
 */
public final class ProgressionTriggers
{
    public static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(Registries.TRIGGER_TYPE, Reference.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, GunTierTrigger> GUN_TIER = REGISTER.register("gun_tier", GunTierTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, RaidWonTrigger> RAID_WON = REGISTER.register("raid_won", RaidWonTrigger::new);

    private ProgressionTriggers() {}

    public static void register(IEventBus bus)
    {
        REGISTER.register(bus);
    }

    public static class GunTierTrigger extends SimpleCriterionTrigger<GunTierTrigger.Instance>
    {
        @Override
        public Codec<Instance> codec()
        {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, GunTier tier)
        {
            this.trigger(player, instance -> instance.tier().map(id -> id.equals(tier.id())).orElse(true)
                    && instance.minLevel().map(level -> tier.level() >= level).orElse(true));
        }

        public record Instance(Optional<ContextAwarePredicate> player, Optional<String> tier, Optional<Integer> minLevel) implements SimpleInstance
        {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    Codec.STRING.optionalFieldOf("tier").forGetter(Instance::tier),
                    Codec.INT.optionalFieldOf("min_level").forGetter(Instance::minLevel)
            ).apply(instance, Instance::new));
        }
    }

    public static class RaidWonTrigger extends SimpleCriterionTrigger<RaidWonTrigger.Instance>
    {
        @Override
        public Codec<Instance> codec()
        {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, String raidId)
        {
            this.trigger(player, instance -> instance.raid().map(raidId::equals).orElse(true));
        }

        public record Instance(Optional<ContextAwarePredicate> player, Optional<String> raid) implements SimpleInstance
        {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    Codec.STRING.optionalFieldOf("raid").forGetter(Instance::raid)
            ).apply(instance, Instance::new));
        }
    }
}
