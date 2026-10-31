// Enhanced BlueprintItem.java
package top.kristeal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import top.kristeal.client.network.ClientPlayHandler;
import top.kristeal.common.exosuit.ExoSuitData;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Ported to NeoForge 1.21.1. Client-only calls go through ClientPlayHandler (common code never links BlueprintScreen or
 * Minecraft, so a dedicated server never loads them), and the "ActiveRecipe" string lives in DataComponents.CUSTOM_DATA.
 */
public class BlueprintItem extends Item {
    public static final String ACTIVE_RECIPE_TAG = "ActiveRecipe";

    public BlueprintItem(Properties properties) {
        this(properties, "");
    }

    public BlueprintItem(Properties properties, String blueprintType) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                ClientPlayHandler.clearBlueprintRecipe(hand);
            } else {
                ClientPlayHandler.openBlueprintScreen(itemstack, player, hand);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext context, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, context, pTooltipComponents, pIsAdvanced);

        pTooltipComponents.add(Component.translatable("item.scguns.blueprint.tooltip.right_click")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

        // Port note: the reference checked level.isClientSide; 1.21 tooltips get no level, so this checks the dist
        // (the helper also returns null without a client level)
        if (FMLEnvironment.dist.isClient()) {
            ClientPlayHandler.addBlueprintResearchTooltip(pStack, pTooltipComponents); // research progression

            String activeRecipeName = ClientPlayHandler.getBlueprintActiveRecipeName(pStack);
            if (activeRecipeName != null) {
                pTooltipComponents.add(Component.translatable("item.scguns.blueprint.tooltip.active_recipe", activeRecipeName)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));

                pTooltipComponents.add(Component.translatable("item.scguns.blueprint.tooltip.shift_right_click_clear")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            }
        }
    }

    /**
     * Port note: moved here from BlueprintScreen#getActiveRecipe. GunBenchMenu calls it on the server, and in the
     * reference that loaded the client-only BlueprintScreen class (a dedicated-server crash). Logic unchanged.
     */
    @Nullable
    public static ResourceLocation getActiveRecipe(ItemStack blueprint) {
        CompoundTag tag = ExoSuitData.getCustomTag(blueprint);
        if (tag != null && tag.contains(ACTIVE_RECIPE_TAG)) {
            return ResourceLocation.parse(tag.getString(ACTIVE_RECIPE_TAG));
        }
        return null;
    }

    public static void setActiveRecipe(ItemStack blueprint, String recipeId) {
        CompoundTag tag = ExoSuitData.getCustomTag(blueprint);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putString(ACTIVE_RECIPE_TAG, recipeId);
        ExoSuitData.setCustomTag(blueprint, tag);
    }

    public static void clearActiveRecipe(ItemStack blueprint) {
        CompoundTag tag = ExoSuitData.getCustomTag(blueprint);
        if (tag != null && tag.contains(ACTIVE_RECIPE_TAG)) {
            tag.remove(ACTIVE_RECIPE_TAG);
            // as in the reference, an emptied tag is removed entirely (setTag(null))
            if (tag.isEmpty()) {
                blueprint.remove(DataComponents.CUSTOM_DATA);
            } else {
                ExoSuitData.setCustomTag(blueprint, tag);
            }
        }
    }
}
