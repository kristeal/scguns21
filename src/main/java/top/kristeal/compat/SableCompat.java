package top.kristeal.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

/**
 * Sable sub-levels (physics-driven block structures) through Sable Companion. Without Sable installed, Companion's
 * default implementation reports no sub-levels, so every method here falls through to plain world behaviour.
 * <p>
 * A sub-level stores its blocks in a distant plot of the same level and places them in the world through a pose.
 * Block traces into a sub-level therefore run in plot space.
 */
public final class SableCompat {
    private SableCompat() {}

    /**
     * Traces a ray against the world and every sub-level it crosses, returning the closest hit.
     * A sub-level hit keeps its plot-space block position and face, so the real block can be read, damaged and broken,
     * while its location is moved back into world space for distances, particles and further tracing.
     *
     * @param tracer traces blocks between two points in the given level, in that level's own coordinates
     */
    public static BlockHitResult clipIncludingSubLevels(Level level, Vec3 from, Vec3 to, BiFunction<Vec3, Vec3, BlockHitResult> tracer) {
        BlockHitResult closest = tracer.apply(from, to);
        double closestDistance = closest.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : from.distanceToSqr(closest.getLocation());

        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(from, to))) {
            Pose3dc pose = subLevel.logicalPose();
            BlockHitResult hit = tracer.apply(pose.transformPositionInverse(from), pose.transformPositionInverse(to));
            if (hit.getType() == HitResult.Type.MISS || SableCompanion.INSTANCE.getContaining(level, hit.getBlockPos()) != subLevel) {
                continue;
            }
            Vec3 location = pose.transformPosition(hit.getLocation());
            double distance = from.distanceToSqr(location);
            if (distance < closestDistance) {
                closest = new BlockHitResult(location, hit.getDirection(), hit.getBlockPos(), hit.isInside());
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Moves a position inside a sub-level plot to where it appears in the world. Other positions are returned as they are.
     */
    public static Vec3 toWorld(Level level, Vec3 pos) {
        // Cast selects the Position overload; the Vec3 one is deprecated
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
    }

    /**
     * Whether a block position lies inside a sub-level plot.
     */
    public static boolean isInSubLevel(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos) != null;
    }
}
