package top.kristeal.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.kristeal.Reference;
import top.kristeal.common.GunAttachments;
import top.kristeal.research.BlueprintResearch;

/**
 * Typed item components for the gun state that other systems read. The reference kept all of
 * these as keys in the stack's NBT; the transient reload/animation keys stay in CUSTOM_DATA (see GunStackData).
 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Reference.MOD_ID);

    /** Reference NBT key "AmmoCount". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO_COUNT =
            REGISTER.registerComponentType("ammo_count", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Reference NBT compound "Attachments": attachment type key -> attachment item stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunAttachments>> ATTACHMENTS =
            REGISTER.registerComponentType("attachments", builder -> builder.persistent(GunAttachments.CODEC).networkSynchronized(GunAttachments.STREAM_CODEC).cacheEncoding());

    /** Reference NBT keys "Gun" (gun data override) and "Custom" (custom gun flag), kept together in one compound. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> GUN_OVERRIDE =
            REGISTER.registerComponentType("gun_override", builder -> builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    /** Reference NBT key "IgnoreAmmo". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IGNORE_AMMO =
            REGISTER.registerComponentType("ignore_ammo", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Reference NBT key "AdditionalDamage". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ADDITIONAL_DAMAGE =
            REGISTER.registerComponentType("additional_damage", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** Research progression: marks a gun as a prototype (built from a half-researched entry). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> PROTOTYPE =
            REGISTER.registerComponentType("prototype", builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    /** Research progression: a blueprint's research, entry id -> finished steps. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintResearch>> RESEARCH =
            REGISTER.registerComponentType("research", builder -> builder.persistent(BlueprintResearch.CODEC).networkSynchronized(BlueprintResearch.STREAM_CODEC).cacheEncoding());
}
