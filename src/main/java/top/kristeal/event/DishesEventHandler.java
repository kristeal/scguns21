package top.kristeal.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.kristeal.Reference;
import top.kristeal.item.ammo_boxes.DishesPouch;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class DishesEventHandler {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ItemStack resultStack = event.getResultStack();
        if (isDishItem(resultStack)) {
            if (addDishToPouch(player, resultStack)) {
                event.setResultStack(ItemStack.EMPTY);
            }
            return;
        }
        // Eating or drinking from a stack keeps the food in hand and puts the empty container straight into the
        // inventory (no pickup event), so move one matching container from the inventory into the pouch.
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack container = DishesPouch.getContainerOf(event.getItem());
        if (!isDishItem(container)) {
            return;
        }
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack != resultStack && ItemStack.isSameItemSameComponents(invStack, container)) {
                ItemStack single = invStack.copyWithCount(1);
                if (addDishToPouch(player, single)) {
                    invStack.shrink(1);
                }
                return;
            }
        }
    }

    // Port note: Forge's cancelable EntityItemPickupEvent -> NeoForge ItemEntityPickupEvent.Pre (not cancelable). Denying
    // the pickup with TriState.FALSE replaces setCanceled(true); emptying the item entity is kept from the reference.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemStack pickedItem = event.getItemEntity().getItem();

        if (isDishItem(pickedItem)) {
            if (addDishToPouch(player, pickedItem)) {
                event.setCanPickup(TriState.FALSE);
                event.getItemEntity().setItem(ItemStack.EMPTY);
            }
        }
    }

    private static boolean isDishItem(ItemStack stack) {
        return DishesPouch.isDish(stack);
    }

    private static boolean addDishToPouch(Player player, ItemStack dishStack) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof DishesPouch) {
                int insertedItems = DishesPouch.add(itemStack, dishStack);
                if (insertedItems > 0) {
                    dishStack.shrink(insertedItems);
                    return true;
                }
            }
        }

        AtomicBoolean result = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof DishesPouch) {
                    int insertedItems = DishesPouch.add(stack, dishStack);
                    if (insertedItems > 0) {
                        dishStack.shrink(insertedItems);
                        result.set(true);
                        break;
                    }
                }
            }
        });

        return result.get();
    }
}