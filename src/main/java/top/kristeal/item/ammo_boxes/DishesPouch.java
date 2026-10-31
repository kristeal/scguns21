package top.kristeal.item.ammo_boxes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.Tags;
import top.kristeal.item.AmmoBoxItem;

import java.util.HashSet;
import java.util.Set;

public class DishesPouch extends AmmoBoxItem {
    private static final int CASING_MAX_ITEM_COUNT = 512;
    private static final int CASING_BAR_COLOR = Mth.color(0.4F, 0.4F, 0.7F);
    private static final TagKey<Item> DISHES_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("scguns", "dishes"));

    /** Containers left behind by any registered food or drink; built on first use, once every mod has registered. */
    private static Set<Item> derivedDishes;

    public DishesPouch(Properties properties) {
        super(properties);
    }

    @Override
    protected ResourceLocation getAmmoTag() {
        return DISHES_TAG.location();
    }
    @Override
    protected String getDescriptionKey() {
        return "item.scguns.dishes_pouch.description";
    }

    @Override
    public boolean canStore(ItemStack stack) {
        return super.canStore(stack) || isDish(stack);
    }

    /**
     * A dish is anything in the dishes tag, or the empty container of some food or drink (its crafting remainder or the
     * food's using_converts_to), so mod bowls, bottles and jars work without tagging each one. Buckets and damageable
     * items are never dishes, and neither is a container that is itself edible.
     */
    public static boolean isDish(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(DISHES_TAG)) {
            return true;
        }
        return getDerivedDishes().contains(stack.getItem())
                && !stack.is(Tags.Items.BUCKETS)
                && !stack.isDamageableItem();
    }

    /** The container a food or drink leaves behind when consumed, or EMPTY if it has none. */
    public static ItemStack getContainerOf(ItemStack consumed) {
        FoodProperties food = consumed.get(DataComponents.FOOD);
        if (food != null && food.usingConvertsTo().isPresent()) {
            return food.usingConvertsTo().get();
        }
        return consumed.getItem().getCraftingRemainingItem(consumed);
    }

    private static Set<Item> getDerivedDishes() {
        if (derivedDishes == null) {
            Set<Item> dishes = new HashSet<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) {
                    continue;
                }
                boolean consumable = stack.has(DataComponents.FOOD)
                        || item.getUseAnimation(stack) == UseAnim.DRINK
                        || item.getUseAnimation(stack) == UseAnim.EAT;
                if (!consumable) {
                    continue;
                }
                Item container = getContainerOf(stack).getItem();
                if (container != Items.AIR && container != item && !new ItemStack(container).has(DataComponents.FOOD)) {
                    dishes.add(container);
                }
            }
            derivedDishes = dishes;
        }
        return derivedDishes;
    }


    @Override
    public int getBarColor(ItemStack stack) {
        return CASING_BAR_COLOR;
    }

    @Override
    protected int getBaseMaxItemCount() {
        return CASING_MAX_ITEM_COUNT;
    }

}
