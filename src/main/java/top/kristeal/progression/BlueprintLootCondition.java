package top.kristeal.progression;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Config;
import top.kristeal.Reference;

/**
 * Loot condition {@code scguns:blueprint_chance}: passes with {@code chance} (times the server's
 * {@code research.blueprintLootChanceMultiplier}), and only in Research mode. It gates the blueprints added to structure
 * loot, which belong to the research progression; in Raid mode blueprints come from the reference's recipes.
 * Checked when the loot is rolled, so a mode switch needs no reload.
 */
public record BlueprintLootCondition(float chance) implements LootItemCondition
{
    public static final MapCodec<BlueprintLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.FLOAT.fieldOf("chance").forGetter(BlueprintLootCondition::chance)
    ).apply(instance, BlueprintLootCondition::new));

    public static final DeferredRegister<LootItemConditionType> REGISTER = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Reference.MOD_ID);
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TYPE = REGISTER.register("blueprint_chance", () -> new LootItemConditionType(CODEC));

    public static void register(IEventBus bus)
    {
        REGISTER.register(bus);
    }

    @Override
    public LootItemConditionType getType()
    {
        return TYPE.get();
    }

    @Override
    public boolean test(LootContext context)
    {
        if (!ProgressionMode.isResearch())
        {
            return false;
        }
        double multiplier = Config.isServerConfigLoaded() ? Config.SERVER.research.blueprintLootChanceMultiplier.get() : 1.0;
        return context.getRandom().nextFloat() < chance * multiplier;
    }
}
