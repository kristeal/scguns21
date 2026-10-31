package top.kristeal.common;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The gun's attachments, keyed by IAttachment.Type#getTagKey() ("Scope", "Barrel", "Stock",
 * "Under_Barrel", "Magazine") as in the reference's "Attachments" NBT compound. Immutable; stacks are copied in and out.
 * The codecs carry registry access, so attachments with data-driven components (e.g. enchantments) are saved correctly.
 */
public final class GunAttachments {
    public static final GunAttachments EMPTY = new GunAttachments(Map.of());

    public static final Codec<GunAttachments> CODEC = Codec.unboundedMap(Codec.STRING, ItemStack.CODEC)
            .xmap(GunAttachments::new, a -> a.stacks);
    public static final StreamCodec<RegistryFriendlyByteBuf, GunAttachments> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, String, ItemStack, Map<String, ItemStack>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ItemStack.STREAM_CODEC)
                    .map(GunAttachments::new, a -> a.stacks);

    private final Map<String, ItemStack> stacks;

    private GunAttachments(Map<String, ItemStack> stacks) {
        Map<String, ItemStack> copy = new HashMap<>();
        stacks.forEach((key, stack) -> {
            if (!stack.isEmpty()) copy.put(key, stack.copy());
        });
        this.stacks = Map.copyOf(copy);
    }

    public boolean has(String key) {
        return this.stacks.containsKey(key);
    }

    public ItemStack get(String key) {
        ItemStack stack = this.stacks.get(key);
        return stack != null ? stack.copy() : ItemStack.EMPTY;
    }

    public GunAttachments with(String key, ItemStack stack) {
        Map<String, ItemStack> copy = new HashMap<>(this.stacks);
        if (stack.isEmpty()) copy.remove(key);
        else copy.put(key, stack);
        return new GunAttachments(copy);
    }

    public GunAttachments without(String key) {
        return this.with(key, ItemStack.EMPTY);
    }

    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GunAttachments other) || other.stacks.size() != this.stacks.size()) return false;
        for (Map.Entry<String, ItemStack> e : this.stacks.entrySet()) {
            ItemStack theirs = other.stacks.get(e.getKey());
            if (theirs == null || !ItemStack.matches(e.getValue(), theirs)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<String, ItemStack> e : this.stacks.entrySet()) {
            hash += Objects.hash(e.getKey(), ItemStack.hashItemAndComponents(e.getValue()), e.getValue().getCount());
        }
        return hash;
    }
}
