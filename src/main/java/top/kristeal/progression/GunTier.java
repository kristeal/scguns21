package top.kristeal.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A gun tier, loaded from {@code data/<ns>/gun_tiers/<id>.json} by {@link GunTierManager}.
 * <ul>
 *     <li>{@code level}: a player only moves to a tier with a higher level (tiers of equal level don't replace each other);</li>
 *     <li>{@code order}: the order tiers are checked in when an item is in several tier tags;</li>
 *     <li>{@code tag}: picking up or crafting an item in it unlocks the tier;</li>
 *     <li>{@code raid_level}: the nightly raids the tier unlocks (every raid up to this level);</li>
 *     <li>{@code mob_tiers}: the tiers whose guns and elite gear mobs may spawn with, most advanced first;</li>
 *     <li>{@code blueprints}: in Research mode, getting one of these unlocks the tier.</li>
 * </ul>
 * Port note: the reference hard-coded these in {@code GunTiers}; the bundled JSONs hold the same values.
 */
public record GunTier(String id, int level, int order, @Nullable TagKey<Item> tag, int raidLevel, List<String> mobTiers, List<Item> blueprints)
{
    public static final GunTier NONE = new GunTier("none", 0, -1, null, 0, List.of(), List.of());

    public boolean matches(ItemStack stack)
    {
        return tag != null && stack.is(tag);
    }

    public boolean isUnlockedBy(ItemStack blueprint)
    {
        return blueprints.contains(blueprint.getItem());
    }

    public MutableComponent displayName()
    {
        return Component.translatable("gun_tier.scguns." + id);
    }

    /** The tiers mobs may use while a player is at this tier, most advanced first (unknown ids are skipped). */
    public List<GunTier> getAvailableMobTiers()
    {
        return mobTiers.stream().map(GunTierManager::getTier).filter(tier -> tier != NONE).toList();
    }
}
