package top.kristeal.init;

import net.minecraft.core.registries.Registries;
import top.kristeal.Reference;
import top.kristeal.effect.IncurableEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.effect.LaceratedEffect;
import top.kristeal.effect.SulfurPoisoningEffect;

/**
 * Author: MrCrayfish
 */
public class    ModEffects
{
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, Reference.MOD_ID);

    public static final DeferredHolder<MobEffect, IncurableEffect> BLINDED = REGISTER.register("blinded", () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0));
    public static final DeferredHolder<MobEffect, IncurableEffect> DEAFENED = REGISTER.register("deafened", () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0));
    public static final DeferredHolder<MobEffect, SulfurPoisoningEffect> SULFUR_POISONING = REGISTER.register("sulfur_poisoning", () -> new SulfurPoisoningEffect(MobEffectCategory.HARMFUL, 0xFFE135));
    public static final DeferredHolder<MobEffect, LaceratedEffect> LACERATED = REGISTER.register("lacerated", () -> new LaceratedEffect(MobEffectCategory.HARMFUL, 0));
}
