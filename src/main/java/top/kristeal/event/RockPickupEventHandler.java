package top.kristeal.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.kristeal.Reference;
import top.kristeal.item.ammo_boxes.RockPouch;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class RockPickupEventHandler {

    // Port note: Forge's cancelable EntityItemPickupEvent -> NeoForge ItemEntityPickupEvent.Pre (not cancelable). Denying
    // the pickup with TriState.FALSE replaces setCanceled(true); emptying the item entity is kept from the reference.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemStack pickedItem = event.getItemEntity().getItem();

        if (isRockItem(pickedItem)) {
            if (addRockToPouch(player, pickedItem)) {
                event.setCanPickup(TriState.FALSE);
                event.getItemEntity().setItem(ItemStack.EMPTY);
            }
        }
    }

    private static boolean isRockItem(ItemStack stack) {
        return stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("scguns", "rocks")));
    }

    private static boolean addRockToPouch(Player player, ItemStack rockStack) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof RockPouch) {
                int insertedItems = RockPouch.add(itemStack, rockStack);
                if (insertedItems > 0) {
                    rockStack.shrink(insertedItems);
                    return true;
                }
            }
        }

        AtomicBoolean result = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof RockPouch) {
                    int insertedItems = RockPouch.add(stack, rockStack);
                    if (insertedItems > 0) {
                        rockStack.shrink(insertedItems);
                        result.set(true);
                        break;
                    }
                }
            }
        });

        return result.get();
    }
}