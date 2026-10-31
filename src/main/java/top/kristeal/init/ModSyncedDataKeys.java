package top.kristeal.init;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.kristeal.Reference;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: the reference used Framework SyncedDataKeys. These are NeoForge data attachments
 * instead: synced to the player and to everyone tracking them (setData syncs automatically on the server), not saved,
 * and not copied on death, which matches Framework's resetOnDeath(). {@link Key} keeps the reference's
 * getValue(player) / setValue(player, value) call shape.
 */
public class ModSyncedDataKeys
{
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Reference.MOD_ID);

    public static final Key<Boolean> AIMING = register("aiming", () -> false, ByteBufCodecs.BOOL);
    public static final Key<Boolean> SHOOTING = register("shooting", () -> false, ByteBufCodecs.BOOL);
    public static final Key<Integer> BURSTCOUNT = register("burstcount", () -> 0, ByteBufCodecs.VAR_INT);
    public static final Key<Boolean> ONBURSTCOOLDOWN = register("onburstcooldown", () -> false, ByteBufCodecs.BOOL);
    public static final Key<Boolean> RELOADING = register("reloading", () -> false, ByteBufCodecs.BOOL);
    public static final Key<Boolean> MELEE = register("melee", () -> false, ByteBufCodecs.BOOL);
    /** The bayonet charge (Banzai) is running; synced so the player's own client and everyone tracking them can show it. */
    public static final Key<Boolean> BANZAI = register("banzai", () -> false, ByteBufCodecs.BOOL);
    /** Damage the bayonet charge has built up by running, as a percentage (0-100), for the HUD bar. */
    public static final Key<Integer> BANZAI_CHARGE = register("banzai_charge", () -> 0, ByteBufCodecs.VAR_INT);

    private static <T> Key<T> register(String name, Supplier<T> defaultValue, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new Key<>(REGISTER.register(name, () -> AttachmentType.builder(defaultValue).sync(streamCodec).build()));
    }

    public record Key<T>(DeferredHolder<AttachmentType<?>, AttachmentType<T>> type) {
        public T getValue(Player player) {
            return player.getData(this.type);
        }

        public void setValue(Player player, T value) {
            player.setData(this.type, value);
        }
    }
}
