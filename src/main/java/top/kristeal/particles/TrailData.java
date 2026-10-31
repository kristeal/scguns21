package top.kristeal.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import top.kristeal.init.ModParticleTypes;

/**
 * Author: MrCrayfish
 */
public class TrailData implements ParticleOptions
{
    // 1.20.5+: ParticleOptions.Deserializer/writeToNetwork/writeToString replaced by MapCodec + StreamCodec
    public static final MapCodec<TrailData> CODEC = RecordCodecBuilder.mapCodec((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("enchanted").forGetter((data) -> {
            return data.enchanted;
        })).apply(builder, TrailData::new);
    });

    public static final StreamCodec<ByteBuf, TrailData> STREAM_CODEC = ByteBufCodecs.BOOL.map(TrailData::new, TrailData::isEnchanted);

    private final boolean enchanted;

    public TrailData(boolean enchanted)
    {
        this.enchanted = enchanted;
    }

    public boolean isEnchanted()
    {
        return this.enchanted;
    }

    @Override
    public ParticleType<?> getType()
    {
        return ModParticleTypes.TRAIL.get();
    }
}
