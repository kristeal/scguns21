package top.kristeal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MemorialBlock extends Block {
    // 1.20.5+: every Block subclass must expose a MapCodec
    public static final MapCodec<MemorialBlock> CODEC = simpleCodec(MemorialBlock::new);

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    public MemorialBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // 1.20.5+: Block#use was split; this ignores the held item, so it maps to useWithoutItem
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            for (int i = 0; i < 5; i++) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
                double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.2;
                double motionX = (level.random.nextDouble() - 0.5) * 0.05;
                double motionZ = (level.random.nextDouble() - 0.5) * 0.05;
                level.addParticle(ParticleTypes.HEART, x, y, z, motionX, 0.15, motionZ);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape base = Block.box(0, 0, 0, 16, 6, 16);
        VoxelShape top = Block.box(2, 6, 2, 14, 24, 14);
        VoxelShape combinedShape = Shapes.or(base, top);

        return switch (state.getValue(FACING)) {
            case NORTH -> combinedShape;
            case SOUTH -> combinedShape;
            case EAST -> combinedShape;
            case WEST -> combinedShape;
            default -> combinedShape;
        };
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
