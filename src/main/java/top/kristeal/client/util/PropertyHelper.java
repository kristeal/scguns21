package top.kristeal.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.kristeal.cache.ObjectCache;
import top.kristeal.client.MetaLoader;
import top.kristeal.common.Gun;
import top.kristeal.common.properties.SightAnimation;
import top.kristeal.item.IMeta;
import top.kristeal.item.attachment.IAttachment;
import top.kristeal.item.attachment.IBarrel;
import top.kristeal.item.attachment.IScope;

import java.util.Optional;

// Port note: Framework's DataObject/DataArray/DataNumber are replaced by Gson's JsonObject/
// JsonArray/JsonPrimitive. has(key, DataType.X) became the has*() helpers below; the lookups, paths and defaults are
// unchanged. ObjectCache entries are keyed by the JSON element itself (value equality) instead of Framework's ids.

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("ConstantConditions")
public final class PropertyHelper
{
    public static final String CACHE_KEY = "properties";
    public static final String MODEL_KEY = "scguns:model";
    public static final String WEAPON_KEY = "scguns:weapon";
    public static final String SCOPE_KEY = "scguns:scope";
    public static final String BARREL_KEY = "scguns:barrel";
    public static final Vec3 GUN_DEFAULT_ORIGIN = new Vec3(8.0, 0.0, 8.0);
    public static final Vec3 ATTACHMENT_DEFAULT_ORIGIN = new Vec3(8.0, 8.0, 8.0);
    public static final Vec3 DEFAULT_SCALE = new Vec3(1.0, 1.0, 1.0);
    public static final Vec3 RED = new Vec3(255.0, 0.0, 0.0);

    public static void resetCache()
    {
        ObjectCache.getInstance(CACHE_KEY).reset();
    }

    private static final JsonObject EMPTY = new JsonObject();

    private static JsonObject getCustomData(ItemStack stack)
    {
        if(stack.getItem() instanceof IMeta)
        {
            return MetaLoader.getInstance().getData(stack.getItem());
        }
        // Port note: the reference read Framework open-model data embedded in the item model here. No scguns item
        // model carries any, so this was always empty.
        return EMPTY;
    }

    private static boolean hasObject(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonObject();
    }

    private static boolean hasArray(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonArray();
    }

    private static boolean hasNumber(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber();
    }

    private static boolean hasString(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isString();
    }

    public static Vec3 getScopeCamera(ItemStack stack) {
        JsonObject customObject = PropertyHelper.getCustomData(stack);
        if (hasObject(customObject, SCOPE_KEY)) {
            JsonObject scopeObject = customObject.getAsJsonObject(SCOPE_KEY);
            if (hasArray(scopeObject, "camera")) {
                JsonArray cameraArray = scopeObject.getAsJsonArray("camera");
                Vec3 cameraPosition = arrayToVec3(cameraArray, Vec3.ZERO);
                return cameraPosition;
            }
        }

        return ATTACHMENT_DEFAULT_ORIGIN;
    }


    public static Vec3 getIronSightCamera(ItemStack stack, Gun modifiedGun, Vec3 gunOrigin)
    {
        JsonObject ironSightObject = getObjectByPath(stack, WEAPON_KEY, "ironSight");
        if(hasArray(ironSightObject, "camera"))
        {
            JsonArray cameraArray = ironSightObject.getAsJsonArray("camera");
            return arrayToVec3(cameraArray, Vec3.ZERO);
        }
        var zoom = modifiedGun.getModules().getZoom();
        if(zoom != null)
        {
            double cameraX = 8 - modifiedGun.getModules().getZoom().getXOffset();
            double cameraY = modifiedGun.getModules().getZoom().getYOffset();
            double cameraZ = 8 - modifiedGun.getModules().getZoom().getZOffset();
            return new Vec3(cameraX, cameraY, cameraZ);
        }
        return Vec3.ZERO;
    }

    public static boolean isLegacyIronSight(ItemStack stack)
    {
        JsonObject ironSightObject = getObjectByPath(stack, WEAPON_KEY, "ironSight");
        return !hasArray(ironSightObject, "camera");
    }

    public static Vec3 getModelOrigin(ItemStack stack, Vec3 defaultOrigin)
    {
        JsonObject customObject = PropertyHelper.getCustomData(stack);
        if(hasObject(customObject, MODEL_KEY))
        {
            JsonObject modelObject = customObject.getAsJsonObject(MODEL_KEY);
            if(hasArray(modelObject, "origin"))
            {
                JsonArray originArray = modelObject.getAsJsonArray("origin");
                return arrayToVec3(originArray, defaultOrigin);
            }
        }
        return defaultOrigin;
    }

    public static Vec3 getAttachmentPosition(ItemStack stack, Gun modifiedGun, IAttachment.Type type) {
        JsonObject scopeObject = getObjectByPath(stack, WEAPON_KEY, "attachments", type.getSerializeKey());
        if (hasArray(scopeObject, "translation")) {
            JsonArray translationArray = scopeObject.getAsJsonArray("translation");
            Vec3 translation = arrayToVec3(translationArray, Vec3.ZERO);
            return translation;
        }
        Gun.ScaledPositioned positioned = modifiedGun.getAttachmentPosition(type);
        if (positioned != null) {
            double displayX = positioned.getXOffset();
            double displayY = positioned.getYOffset();
            double displayZ = positioned.getZOffset();
            Vec3 translation = new Vec3(displayX, displayY, displayZ).add(GUN_DEFAULT_ORIGIN);
            return translation;
        }
        return Vec3.ZERO;
    }


    public static Vec3 getAttachmentScale(ItemStack weapon, Gun modifiedGun, IAttachment.Type type)
    {
        JsonObject scopeObject = getObjectByPath(weapon, WEAPON_KEY, "attachments", type.getSerializeKey());
        if(hasArray(scopeObject, "scale"))
        {
            JsonArray scaleArray = scopeObject.getAsJsonArray("scale");
            return arrayToVec3(scaleArray, DEFAULT_SCALE);
        }
        Gun.ScaledPositioned positioned = modifiedGun.getAttachmentPosition(type);
        if(positioned != null)
        {
            return new Vec3(positioned.getScale(), positioned.getScale(), positioned.getScale());
        }
        return DEFAULT_SCALE;
    }

    public static boolean hasMuzzleFlash(ItemStack weapon, Gun modifiedGun)
    {
        JsonObject weaponObject = getObjectByPath(weapon, WEAPON_KEY);
        return hasObject(weaponObject, "muzzleFlash") || modifiedGun.getDisplay().getFlash() != null;
    }

    public static Vec3 getMuzzleFlashPosition(ItemStack weapon, Gun modifiedGun)
    {
        if(Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.BARREL))
        {
            ItemStack barrelStack = Gun.getAttachment(IAttachment.Type.BARREL, weapon);
            if(barrelStack.getItem() instanceof IBarrel)
            {
                JsonObject barrelObject = getObjectByPath(barrelStack, BARREL_KEY);
                if(hasObject(barrelObject, "muzzleFlash"))
                {
                    JsonObject muzzleObject = barrelObject.getAsJsonObject("muzzleFlash");
                    JsonArray translationArray = muzzleObject.getAsJsonArray("translation");
                    Vec3 muzzlePosition = arrayToVec3(translationArray, Vec3.ZERO);
                    Vec3 barrelOrigin = PropertyHelper.getModelOrigin(barrelStack, ATTACHMENT_DEFAULT_ORIGIN);
                    Vec3 barrelPosition = PropertyHelper.getAttachmentPosition(weapon, modifiedGun, IAttachment.Type.BARREL);
                    Vec3 barrelScale = PropertyHelper.getAttachmentScale(weapon, modifiedGun, IAttachment.Type.BARREL);
                    return muzzlePosition.subtract(barrelOrigin).multiply(barrelScale).add(barrelPosition);
                }
            }
        }

        JsonObject weaponObject = getObjectByPath(weapon, WEAPON_KEY);
        if(hasObject(weaponObject, "muzzleFlash"))
        {
            JsonObject muzzleObject = weaponObject.getAsJsonObject("muzzleFlash");
            JsonArray translationArray = muzzleObject.getAsJsonArray("translation");
            return arrayToVec3(translationArray, Vec3.ZERO);
        }

        Gun.Positioned muzzleFlash = modifiedGun.getDisplay().getFlash();
        if(muzzleFlash != null)
        {
            double displayX = muzzleFlash.getXOffset();
            double displayY = muzzleFlash.getYOffset();
            double displayZ = muzzleFlash.getZOffset();
            return new Vec3(displayX, displayY, displayZ).add(GUN_DEFAULT_ORIGIN);
        }
        return Vec3.ZERO;
    }

    public static Vec3 getMuzzleFlashScale(ItemStack weapon, Gun modifiedGun)
    {
        JsonObject weaponObject = getObjectByPath(weapon, WEAPON_KEY);
        if(hasObject(weaponObject, "muzzleFlash"))
        {
            JsonObject muzzleObject = weaponObject.getAsJsonObject("muzzleFlash");
            if(hasArray(muzzleObject, "scale"))
            {
                JsonArray scaleArray = muzzleObject.getAsJsonArray("scale");
                return arrayToVec3(scaleArray, DEFAULT_SCALE);
            }
            return DEFAULT_SCALE;
        }
        Gun.Display.Flash muzzleFlash = modifiedGun.getDisplay().getFlash();
        if(muzzleFlash != null)
        {
            double scale = muzzleFlash.getSize();
            return new Vec3(scale, scale, 1.0);
        }
        return DEFAULT_SCALE;
    }

    public static boolean isUsingBarrelMuzzleFlash(ItemStack barrel)
    {
        JsonObject customObject = getObjectByPath(barrel, BARREL_KEY);
        return hasObject(customObject, "muzzleFlash");
    }


    public static SightAnimation getSightAnimations(ItemStack weapon, Gun modifiedGun)
    {
        if(Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.SCOPE))
        {
            ItemStack scopeStack = Gun.getScopeStack(weapon);
            if(scopeStack.getItem() instanceof IScope scope)
            {
                JsonObject scopeObject = getObjectByPath(scopeStack, SCOPE_KEY);
                if(scopeObject.get("sightAnimation") instanceof JsonObject sightObject)
                {
                    return objectToSightAnimation(sightObject);
                }
            }
        }
        JsonObject customObject = getObjectByPath(weapon, WEAPON_KEY, "ironSight");
        if(customObject.get("sightAnimation") instanceof JsonObject sightObject)
        {
            return objectToSightAnimation(sightObject);
        }

        return SightAnimation.DEFAULT;
    }

    public static double getViewportFov(ItemStack weapon, Gun modifiedGun)
    {
        if(Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.SCOPE))
        {
            ItemStack scopeStack = Gun.getScopeStack(weapon);
            JsonObject customObject = getObjectByPath(scopeStack, SCOPE_KEY);
            if(hasNumber(customObject, "viewportFov"))
            {
                return Mth.clamp(customObject.get("viewportFov").getAsDouble(), 1.0, 100.0);
            }
        }

        JsonObject customObject = getObjectByPath(weapon, WEAPON_KEY, "ironSight");
        if(hasNumber(customObject, "viewportFov"))
        {
            return Mth.clamp(customObject.get("viewportFov").getAsDouble(), 1.0, 100.0);
        }

        return 0;
    }

    private static SightAnimation objectToSightAnimation(JsonObject object)
    {
        ObjectCache cache = ObjectCache.getInstance(CACHE_KEY);
        Optional<SightAnimation> cachedValue = cache.get(object);
        return cachedValue.orElseGet(() -> cache.store(object, () -> {
            SightAnimation.Builder builder = SightAnimation.builder();
            getOptionalString(object, "viewportCurve").ifPresent(s -> builder.setViewportCurve(Easings.byName(s)));
            getOptionalString(object, "sightCurve").ifPresent(s -> builder.setSightCurve(Easings.byName(s)));
            getOptionalString(object, "fovCurve").ifPresent(s -> builder.setFovCurve(Easings.byName(s)));
            getOptionalString(object, "aimTransformCurve").ifPresent(s -> builder.setAimTransformCurve(Easings.byName(s)));
            return builder.build();
        }));
    }

    private static Optional<String> getOptionalString(JsonObject src, String key)
    {
        if(hasString(src, key))
        {
            return Optional.ofNullable(src.get(key).getAsString());
        }
        return Optional.empty();
    }

    public static JsonObject getObjectByPath(ItemStack stack, String... path)
    {
        JsonObject result = PropertyHelper.getCustomData(stack);
        for(String key : path)
        {
            if(hasObject(result, key))
            {
                result = result.getAsJsonObject(key);
                continue;
            }
            return EMPTY;
        }
        return result;
    }

    public static Vec3 arrayToVec3(JsonArray array, Vec3 defaultValue)
    {
        if(array == null || array.size() != 3)
            return defaultValue;

        ObjectCache cache = ObjectCache.getInstance(CACHE_KEY);
        Optional<Vec3> cachedValue = cache.get(array);
        return cachedValue.orElseGet(() -> cache.store(array, () ->
        {
            if(array.asList().stream().allMatch(entry -> entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isNumber()))
            {
                double x = array.get(0).getAsDouble();
                double y = array.get(1).getAsDouble();
                double z = array.get(2).getAsDouble();
                return new Vec3(x, y, z);
            }
            return defaultValue;
        }));


    }
}