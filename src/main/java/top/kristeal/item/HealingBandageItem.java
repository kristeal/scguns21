package top.kristeal.item;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.kristeal.ScorchedGuns;
import top.kristeal.init.ModEffects;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HealingBandageItem extends Item {
    int healingAmount;
    List<MobEffectInstance> potionEffects;

    public HealingBandageItem(Item.Properties properties, int healingAmount, MobEffectInstance... potionEffects) {
        super(properties);
        this.potionEffects = Arrays.stream(potionEffects).filter(Objects::nonNull).toList();
        this.healingAmount = healingAmount;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, player, hand);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
        if (entityLiving instanceof Player player && !world.isClientSide) {
            player.heal(healingAmount);
            if (player.hasEffect(ModEffects.LACERATED)) {
                player.removeEffect(ModEffects.LACERATED);
            }
            for (MobEffectInstance effect : potionEffects) {
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect));
                }
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.scguns.healing_bandage.heal", healingAmount).withStyle(ChatFormatting.GREEN));
        if (!potionEffects.isEmpty()) {
            for (MobEffectInstance effect : potionEffects) {
                if (effect != null) {
                    effect.getEffect();
                    Component effectName = Component.translatable(effect.getEffect().value().getDescriptionId()).withStyle(ChatFormatting.BLUE);
                    int durationInSeconds = effect.getDuration() / 20;
                    int minutes = durationInSeconds / 60;
                    int seconds = durationInSeconds % 60;
                    String formattedDuration = String.format(" (%02d:%02d)", minutes, seconds);
                    Component effectDuration = Component.literal(formattedDuration).withStyle(ChatFormatting.BLUE);
                    tooltip.add(Component.empty().append(effectName).append(effectDuration));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }


}
