package top.kristeal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class PlayerTurretTargetingBlock extends TurretTargetingBlock{
    public static final MapCodec<PlayerTurretTargetingBlock> CODEC = simpleCodec(PlayerTurretTargetingBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public PlayerTurretTargetingBlock(Properties properties) {
        super(properties);
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
