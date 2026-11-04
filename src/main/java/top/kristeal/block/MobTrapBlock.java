package top.kristeal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.blockentity.MobTrapBlockEntity;
import top.kristeal.init.ModBlockEntities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobTrapBlock extends BaseEntityBlock {
    public static final MapCodec<MobTrapBlock> CODEC = simpleCodec(MobTrapBlock::new);

    public MobTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** A spawn egg loads one mob into the trap. */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                      @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!(stack.getItem() instanceof SpawnEggItem eggItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MobTrapBlockEntity mobTrap) {
            if (mobTrap.addMob(eggItem.getType(stack))) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            } else {
                player.sendSystemMessage(Component.translatable("mobtrap.too_many_mobs").withStyle(ChatFormatting.RED));
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand lists what the trap holds. */
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                       @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MobTrapBlockEntity mobTrap) {
            List<EntityType<?>> storedMobs = mobTrap.getStoredMobs();
            if (storedMobs.isEmpty()) {
                player.sendSystemMessage(Component.translatable("mobtrap.empty").withStyle(ChatFormatting.GRAY));
            } else {
                player.sendSystemMessage(Component.translatable("mobtrap.stored_mobs").withStyle(ChatFormatting.GRAY));
                Map<EntityType<?>, Integer> mobCounts = new LinkedHashMap<>();
                for (EntityType<?> mob : storedMobs) {
                    mobCounts.merge(mob, 1, Integer::sum);
                }
                for (Map.Entry<EntityType<?>, Integer> entry : mobCounts.entrySet()) {
                    player.sendSystemMessage(Component.literal("- " + BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()) + " x" + entry.getValue()).withStyle(ChatFormatting.GREEN));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (!level.isClientSide && entity instanceof Player && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof MobTrapBlockEntity mobTrap) {
            mobTrap.releaseMobs(serverLevel, pos);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MOB_TRAP.get(), (lvl, pos, blockState, blockEntity) -> blockEntity.serverTick(lvl, pos));
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MobTrapBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}
