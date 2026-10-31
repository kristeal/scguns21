package top.kristeal.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.kristeal.Reference;

import javax.annotation.Nullable;

/**
 * A player's gun tier and raid level, tracked in both progression modes.
 * <p>
 * Port note: the reference kept this in the player's persistent NBT ({@code SCGunsProgression}) and copied it by hand
 * on death. Here it is a saved data attachment that is copied on death. The raid level is stored apart from the tier,
 * as in the reference, so it can be set on its own.
 */
public final class PlayerGunProgression
{
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Reference.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Data>> DATA = REGISTER.register("gun_progression",
            () -> AttachmentType.builder(() -> Data.NONE).serialize(Data.CODEC).copyOnDeath().build());

    private PlayerGunProgression() {}

    public static void register(IEventBus bus)
    {
        REGISTER.register(bus);
    }

    public record Data(String tierId, int raidLevel)
    {
        public static final Data NONE = new Data(GunTier.NONE.id(), 0);
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("tier").forGetter(Data::tierId),
                Codec.INT.fieldOf("raid_level").forGetter(Data::raidLevel)
        ).apply(instance, Data::new));
    }

    public static GunTier getTier(Player player)
    {
        return GunTierManager.getTier(player.getData(DATA).tierId());
    }

    public static int getRaidLevel(Player player)
    {
        return player.getData(DATA).raidLevel();
    }

    /** Sets the tier and the raid level that goes with it ({@code null} clears both). */
    public static void setTier(Player player, @Nullable GunTier tier)
    {
        GunTier value = tier == null ? GunTier.NONE : tier;
        player.setData(DATA, new Data(value.id(), value.raidLevel()));
        if (player instanceof ServerPlayer serverPlayer)
        {
            ProgressionTriggers.GUN_TIER.get().trigger(serverPlayer, value);
        }
    }

    public static void setRaidLevel(Player player, int raidLevel)
    {
        player.setData(DATA, new Data(player.getData(DATA).tierId(), Math.max(0, raidLevel)));
    }

    /** Moves the player to this tier if its level is higher than their current one. Returns whether it did. */
    public static boolean unlock(Player player, @Nullable GunTier tier)
    {
        if (tier == null || tier.level() <= getTier(player).level())
        {
            return false;
        }
        setTier(player, tier);
        return true;
    }

    /** The reference rule: the first tier whose tag holds the item. */
    @Nullable
    public static GunTier unlockFromItem(Player player, ItemStack stack)
    {
        GunTier tier = GunTierManager.getTierForItem(stack);
        return unlock(player, tier) ? tier : null;
    }

    /** Research mode: the tier a blueprint unlocks. */
    @Nullable
    public static GunTier unlockFromBlueprint(Player player, ItemStack stack)
    {
        GunTier tier = GunTierManager.getTierForBlueprint(stack);
        return unlock(player, tier) ? tier : null;
    }
}
