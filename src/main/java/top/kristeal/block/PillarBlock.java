package top.kristeal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PillarBlock extends RotatedPillarBlock {
    // 1.20.5+: every Block subclass must expose a MapCodec
    public static final MapCodec<PillarBlock> CODEC = simpleCodec(PillarBlock::new);


    public PillarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }
}
