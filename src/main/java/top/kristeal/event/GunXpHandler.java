package top.kristeal.event;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.common.Gun;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedSculkGunItem;
import top.kristeal.item.attachment.IAttachment;

// Fix for a reference bug: was a Dist.CLIENT subscriber, so XP pickup (a server event) never repaired on a dedicated server
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunXpHandler {
    private static final float SCULK_REPAIR_RATIO = 0.5F;

    @SubscribeEvent
    public static void onPlayerXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.getItem() instanceof GunItem gunItem) {
            ExperienceOrb orb = event.getOrb();
            int remainingXp = orb.value;
            if (mainHand.getItem() instanceof AnimatedSculkGunItem && mainHand.isDamaged() && remainingXp > 0) {
                int repairAmount = Math.min((int)(remainingXp * 2 * SCULK_REPAIR_RATIO), mainHand.getDamageValue());
                if (repairAmount > 0) {
                    mainHand.setDamageValue(mainHand.getDamageValue() - repairAmount);
                    remainingXp -= repairAmount;
                }
            }
            for (IAttachment.Type type : IAttachment.Type.values()) {
                ItemStack attachmentStack = Gun.getAttachment(type, mainHand);
                if (!attachmentStack.isEmpty() &&
                        EnchantmentHelper.getItemEnchantmentLevel(player.registryAccess().holderOrThrow(Enchantments.MENDING), attachmentStack) > 0 &&
                        attachmentStack.isDamaged() &&
                        remainingXp > 0) {
                    int repairAmount = Math.min(remainingXp * 2, attachmentStack.getDamageValue());
                    if (repairAmount > 0) {
                        attachmentStack.setDamageValue(attachmentStack.getDamageValue() - repairAmount);
                        // the attachment is a copy of the stored one, so write it back
                        top.kristeal.common.GunStackData.setAttachment(mainHand, type.getTagKey(), attachmentStack);
                        gunItem.onAttachmentChanged(mainHand);
                        remainingXp -= (repairAmount / 2);
                    }
                }
            }

            orb.value = remainingXp;
        }
    }
}