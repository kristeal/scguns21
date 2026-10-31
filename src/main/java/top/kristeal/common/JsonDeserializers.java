package top.kristeal.common;

import com.google.gson.JsonDeserializer;
import top.kristeal.client.util.Easings;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public class JsonDeserializers
{
    // Port note: Forge's CraftingHelper.getItemStack is gone. Reads the 1.20.1 {"item", "count"} shape; the reference also
    // read "nbt", which 1.21 item components replace (no custom_guns data uses it).
    public static final JsonDeserializer<ItemStack> ITEM_STACK = (json, typeOfT, context) -> {
        JsonObject object = json.getAsJsonObject();
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(GsonHelper.getAsString(object, "item")));
        return new ItemStack(item, GsonHelper.getAsInt(object, "count", 1));
    };
    public static final JsonDeserializer<ResourceLocation> RESOURCE_LOCATION = (json, typeOfT, context) -> ResourceLocation.parse(json.getAsString());
    public static final JsonDeserializer<FireMode> FIRE_MODE = (json, typeOfT, context) -> FireMode.getType(ResourceLocation.tryParse(json.getAsString()));
    public static final JsonDeserializer<ReloadType> RELOAD_TYPE = (json, typeOfT, context) -> ReloadType.getType(ResourceLocation.tryParse(json.getAsString()));
    public static final JsonDeserializer<GripType> GRIP_TYPE = (json, typeOfT, context) -> GripType.getType(ResourceLocation.tryParse(json.getAsString()));
    public static final JsonDeserializer<Easings> EASING = (json, typeOfT, context) -> Easings.byName(json.getAsString());
}
