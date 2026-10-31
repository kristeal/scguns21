package top.kristeal.util;


import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import top.kristeal.init.ModBlocks;
import top.kristeal.init.ModItems;

public class ModCauldronInteraction {
    public static void register() {
        CauldronInteraction.EMPTY.map().put(ModItems.VICIOUS_ACID_BUCKET.get(), (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.setBlockAndUpdate(pos, ModBlocks.VICIOUS_ACID_CAULDRON.get().defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}
