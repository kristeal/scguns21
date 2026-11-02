package top.kristeal.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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
     * Helper methods for turrets
     */
    public static Vec3 toWorld(Level level, Vec3 pos) {
        // Cast selects the Position overload; the Vec3 one is deprecated
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
    }

    public static boolean isInSubLevel(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos) != null;
    }

    @Nullable
    public static SubLevelAccess getContaining(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos);
    }

    public static Vec3 toPlotFrame(@Nullable SubLevelAccess subLevel, Vec3 worldPos) {
        return subLevel == null ? worldPos : subLevel.logicalPose().transformPositionInverse(worldPos);
    }

    public static boolean hasLineOfSight(Level level, Vec3 fromWorld, Vec3 toWorld) {
        BlockHitResult hit = clipIncludingSubLevels(level, fromWorld, toWorld, (from, to) ->
                level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())));
        return hit.getType() == HitResult.Type.MISS;
    }

    /**
     * Entities of the given class inside a world-space box, plus those inside any sub-level plot the box crosses
     * (queried in that plot's own coordinates, since entities tracking a sub-level live in its plot).
     */
    public static <T extends Entity> List<T> getEntitiesOfClassIncludingSubLevels(Level level, Class<T> clazz, AABB worldBox, Predicate<? super T> predicate) {
        List<T> result = new ArrayList<>(level.getEntitiesOfClass(clazz, worldBox, predicate));
        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(worldBox))) {
            AABB plotBox = toPlotBox(subLevel, worldBox);
            for (T entity : level.getEntitiesOfClass(clazz, plotBox, predicate)) {
                boolean duplicate = false;
                for (T existing : result) {
                    if (existing.getId() == entity.getId()) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    result.add(entity);
                }
            }
        }
        return result;
    }

    /**
     * Maps a world-space box into a sub-level's plot frame by transforming its corners.
     */
    private static AABB toPlotBox(SubLevelAccess subLevel, AABB worldBox) {
        Pose3dc pose = subLevel.logicalPose();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = pose.transformPositionInverse(new Vec3(
                    (i & 1) == 0 ? worldBox.minX : worldBox.maxX,
                    (i & 2) == 0 ? worldBox.minY : worldBox.maxY,
                    (i & 4) == 0 ? worldBox.minZ : worldBox.maxZ));
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
