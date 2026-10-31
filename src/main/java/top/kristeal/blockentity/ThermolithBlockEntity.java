package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.kristeal.block.ThermolithBlock;
import top.kristeal.client.screen.ThermolithMenu;
import top.kristeal.init.ModBlockEntities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ThermolithBlockEntity extends BlockEntity implements MenuProvider {
    private static final ResourceLocation THERMOLITH_INGREDIENT_TAG = ResourceLocation.fromNamespaceAndPath("scguns", "thermolith_ingredient");
    private static final ResourceLocation MELTABLE_BLOCKS_TAG = ResourceLocation.fromNamespaceAndPath("scguns", "meltable_blocks");
    private static final int MELT_INTERVAL = 40;
    private static final double MELT_RADIUS = 6.0;
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateLitState();
        }
    };
    private final IItemHandler handler = itemHandler;
    private int tickCounter = 0;
    private boolean isLit = false;
    public ThermolithBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMOLITH.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        updateLitState();

        if (isLit) {
            tickCounter++;
            if (tickCounter >= MELT_INTERVAL) {
                tickCounter = 0;
                meltRandomBlock();
            }
        } else {
            tickCounter = 0;
        }
    }

    private void meltRandomBlock() {
        RandomSource rand = RANDOM.get();
        BlockPos startPos = findNearestLava();
        if (startPos == null) {
            startPos = worldPosition;
        }

        for (int radius = 1; radius <= MELT_RADIUS; radius++) {
            List<BlockPos> candidatesAtRadius = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                            BlockPos targetPos = startPos.offset(dx, dy, dz);

                            if (worldPosition.distSqr(targetPos) <= MELT_RADIUS * MELT_RADIUS
                                    && level.getBlockState(targetPos).is(BlockTags.create(MELTABLE_BLOCKS_TAG))) {
                                candidatesAtRadius.add(targetPos);
                            }
                        }
                    }
                }
            }
            if (!candidatesAtRadius.isEmpty()) {
                BlockPos targetPos = candidatesAtRadius.get(rand.nextInt(candidatesAtRadius.size()));

                level.setBlockAndUpdate(targetPos, Blocks.LAVA.defaultBlockState());

                if (rand.nextFloat() < 0.15f) {
                    itemHandler.extractItem(0, 1, false);
                }

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                            10, 0.5, 0.5, 0.5, 0.1);
                }
                return;
            }
        }
    }

    private BlockPos findNearestLava() {
        for (int radius = 1; radius <= MELT_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                            BlockPos checkPos = worldPosition.offset(dx, dy, dz);
                            if (level.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
                                return checkPos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void updateLitState() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        boolean shouldBeLit = !stack.isEmpty() && stack.is(ItemTags.create(THERMOLITH_INGREDIENT_TAG));
        if (isLit != shouldBeLit) {
            isLit = shouldBeLit;
            level.setBlock(worldPosition, getBlockState().setValue(ThermolithBlock.LIT, isLit), Block.UPDATE_ALL);
        }
    }
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.thermolith");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new ThermolithMenu(id, playerInventory, this);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Port note: replaces the Forge getCapability override (same side mapping); registered in ModCapabilities.
     */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return handler;
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        isLit = tag.getBoolean("isLit");
        tickCounter = tag.getInt("tickCounter");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("isLit", isLit);
        tag.putInt("tickCounter", tickCounter);
        super.saveAdditional(tag, registries);
    }

    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
    public boolean isLit() {
        return isLit;
    }
}