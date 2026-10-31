package top.kristeal.research;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.HashMap;
import java.util.Map;

/**
 * Research progression: the research state stored on a blueprint stack, as the
 * {@code scguns:research} component. Maps an entry (the researched item's id, e.g. {@code scguns:lockewood}) to the
 * number of research steps finished. Entries at 0 steps are not stored.
 */
public record BlueprintResearch(Map<ResourceLocation, Integer> steps) {
    public static final BlueprintResearch EMPTY = new BlueprintResearch(Map.of());

    public static final Codec<BlueprintResearch> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, ExtraCodecs.POSITIVE_INT)
            .xmap(BlueprintResearch::new, BlueprintResearch::steps);

    public static final StreamCodec<ByteBuf, BlueprintResearch> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, Integer, Map<ResourceLocation, Integer>>map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(BlueprintResearch::new, BlueprintResearch::steps);

    public BlueprintResearch {
        steps = Map.copyOf(steps);
    }

    public int getSteps(ResourceLocation entry) {
        return steps.getOrDefault(entry, 0);
    }

    /** Returns a copy with the entry set to the given step count (0 removes it). */
    public BlueprintResearch withSteps(ResourceLocation entry, int stepCount) {
        Map<ResourceLocation, Integer> copy = new HashMap<>(steps);
        if (stepCount <= 0) {
            copy.remove(entry);
        } else {
            copy.put(entry, stepCount);
        }
        return new BlueprintResearch(copy);
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }
}
