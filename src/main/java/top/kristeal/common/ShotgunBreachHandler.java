package top.kristeal.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import top.kristeal.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Breaks a block once more than a configured number of pellets from a single shot strike it.
 * Pellets fired by the same shooter in the same game tick count as one shot.
 */
public final class ShotgunBreachHandler {
    private static final long EXPIRY_TICKS = 20L;
    private static final List<Block> UNBREAKABLE_BLOCKS = List.of(
            Blocks.BEDROCK,
            Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN,
            Blocks.END_PORTAL_FRAME,
            Blocks.ANCIENT_DEBRIS,
            Blocks.REINFORCED_DEEPSLATE
    );

    private record ShotHit(ResourceKey<Level> dimension, int shooterId, long fireTime, BlockPos pos) {}

    private static final Map<ShotHit, Integer> HITS = new HashMap<>();

    private ShotgunBreachHandler() {}

    /**
     * Whether a gun firing this many pellets per shot can ever breach.
     */
    public static boolean canBreach(int pelletsPerShot) {
        return Config.COMMON.gameplay.griefing.enableBlockBreaking.get()
                && Config.COMMON.gameplay.griefing.enableShotgunBreaching.get()
                && pelletsPerShot > Config.COMMON.gameplay.griefing.shotgunBreachPelletThreshold.get();
    }

    public static void onPelletHit(ServerLevel level, int shooterId, long fireTime, BlockPos pos, double x, double y, double z) {
        long now = level.getGameTime();
        HITS.keySet().removeIf(hit -> now - hit.fireTime() > EXPIRY_TICKS);

        BlockState state = level.getBlockState(pos);
        if (!isBreachable(level, pos, state)) {
            return;
        }

        ShotHit key = new ShotHit(level.dimension(), shooterId, fireTime, pos.immutable());
        int count = HITS.merge(key, 1, Integer::sum);
        if (count <= Config.COMMON.gameplay.griefing.shotgunBreachPelletThreshold.get()) {
            return;
        }

        HITS.remove(key);
        level.destroyBlock(pos, true);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 30, 0.0D, 0.0D, 0.0D, 0.15D);
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static boolean isBreachable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || UNBREAKABLE_BLOCKS.contains(state.getBlock())) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > Config.COMMON.gameplay.griefing.shotgunBreachMaxHardness.get()) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }
        return state.getFluidState().isEmpty();
    }
}
