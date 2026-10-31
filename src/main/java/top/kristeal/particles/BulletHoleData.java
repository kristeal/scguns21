package top.kristeal.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import top.kristeal.init.ModParticleTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

/**
 * Author: MrCrayfish
 */
public class BulletHoleData implements ParticleOptions
{
    // 1.20.5+: ParticleOptions.Deserializer/writeToNetwork/writeToString replaced by MapCodec + StreamCodec
    public static final MapCodec<BulletHoleData> CODEC = RecordCodecBuilder.mapCodec((builder) -> {
        return builder.group(Codec.INT.fieldOf("dir").forGetter((data) -> {
            return data.direction.ordinal();
        }), Codec.LONG.fieldOf("pos").forGetter((p_239806_0_) -> {
            return p_239806_0_.pos.asLong();
        })).apply(builder, BulletHoleData::new);
    });

    public static final StreamCodec<ByteBuf, BulletHoleData> STREAM_CODEC = StreamCodec.composite(
            Direction.STREAM_CODEC, BulletHoleData::getDirection,
            BlockPos.STREAM_CODEC, BulletHoleData::getPos,
            BulletHoleData::new);

    private final Direction direction;
    private final BlockPos pos;

    public BulletHoleData(int dir, long pos)
    {
        this.direction = Direction.values()[dir];
        this.pos = BlockPos.of(pos);
    }

    public BulletHoleData(Direction dir, BlockPos pos)
    {
        this.direction = dir;
        this.pos = pos;
    }

    public Direction getDirection()
    {
        return this.direction;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    @Override
    public ParticleType<?> getType()
    {
        return ModParticleTypes.BULLET_HOLE.get();
    }
}
