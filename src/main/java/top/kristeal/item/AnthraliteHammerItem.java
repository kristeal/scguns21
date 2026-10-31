package top.kristeal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Create: Ironworks compat: a pickaxe that mines a 3x3 area through Ironworks' own hammer procedure, so it behaves like
 * the Ironworks hammers. Registered only when Create: Ironworks is loaded; the procedure is looked up by reflection so
 * there's no compile dependency on Ironworks.
 * <p>
 * Port note: Ironworks 4.x (1.21.1) added an ItemStack parameter to {@code HammerProcedure.execute}; the reference called
 * the five-parameter 1.20.1 version.
 */
public class AnthraliteHammerItem extends PickaxeItem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Method hammerProcedure;
    private static boolean lookedUp;

    public AnthraliteHammerItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean result = super.mineBlock(stack, level, state, pos, entity);
        Method procedure = getHammerProcedure();
        if (procedure != null) {
            try {
                procedure.invoke(null, level, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), entity, stack);
            } catch (ReflectiveOperationException e) {
                LOGGER.debug("Create: Ironworks hammer procedure failed", e);
            }
        }
        return result;
    }

    private static Method getHammerProcedure() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                hammerProcedure = Class.forName("create_ironworks.procedures.HammerProcedure").getMethod("execute",
                        LevelAccessor.class, double.class, double.class, double.class, Entity.class, ItemStack.class);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("Create: Ironworks hammer procedure not found; the Anthralite Hammer mines single blocks", e);
            }
        }
        return hammerProcedure;
    }
}
