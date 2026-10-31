package top.kristeal.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import top.kristeal.init.ModDataComponents;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Every read and write of gun state on an item stack goes through here.
 * <ul>
 *     <li>Typed components (ModDataComponents) for AmmoCount, Attachments, the "Gun"/"Custom" override, IgnoreAmmo and
 *     AdditionalDamage.</li>
 *     <li>The reference's transient reload/animation keys (DrawnTick, IsReloading, FireTimer, ...) keep their names in
 *     the CUSTOM_DATA compound. 1.21 components are immutable, so writes go through {@link #updateState}; the
 *     reference mutated getOrCreateTag() in place.</li>
 * </ul>
 */
public final class GunStackData {
    private GunStackData() {}

    // ---- AmmoCount ----
    public static int getAmmoCount(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO_COUNT, 0);
    }

    public static void setAmmoCount(ItemStack stack, int count) {
        stack.set(ModDataComponents.AMMO_COUNT, count);
    }

    public static boolean hasAmmoCount(ItemStack stack) {
        return stack.has(ModDataComponents.AMMO_COUNT);
    }

    // ---- IgnoreAmmo / AdditionalDamage ----
    public static boolean isIgnoreAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.IGNORE_AMMO, false);
    }

    public static void setIgnoreAmmo(ItemStack stack, boolean ignore) {
        stack.set(ModDataComponents.IGNORE_AMMO, ignore);
    }

    public static boolean hasAdditionalDamage(ItemStack stack) {
        return stack.has(ModDataComponents.ADDITIONAL_DAMAGE);
    }

    public static float getAdditionalDamage(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ADDITIONAL_DAMAGE, 0.0F);
    }

    public static void setAdditionalDamage(ItemStack stack, float damage) {
        stack.set(ModDataComponents.ADDITIONAL_DAMAGE, damage);
    }

    // ---- Prototype ----
    public static boolean isPrototype(ItemStack stack) {
        return stack.has(ModDataComponents.PROTOTYPE);
    }

    public static void setPrototype(ItemStack stack, boolean prototype) {
        if (prototype) stack.set(ModDataComponents.PROTOTYPE, Unit.INSTANCE);
        else stack.remove(ModDataComponents.PROTOTYPE);
    }

    // ---- Attachments ----
    /** A prototype takes no attachments, so it always reads as having none. */
    public static GunAttachments getAttachments(ItemStack stack) {
        if (isPrototype(stack)) return GunAttachments.EMPTY;
        return stack.getOrDefault(ModDataComponents.ATTACHMENTS, GunAttachments.EMPTY);
    }

    public static boolean hasAttachment(ItemStack stack, String key) {
        return getAttachments(stack).has(key);
    }

    public static ItemStack getAttachment(ItemStack stack, String key) {
        return getAttachments(stack).get(key);
    }

    public static void setAttachment(ItemStack stack, String key, ItemStack attachment) {
        if (isPrototype(stack)) return; // Prototypes take no attachments
        GunAttachments attachments = getAttachments(stack).with(key, attachment);
        if (attachments.isEmpty()) stack.remove(ModDataComponents.ATTACHMENTS);
        else stack.set(ModDataComponents.ATTACHMENTS, attachments);
    }

    public static void removeAttachment(ItemStack stack, String key) {
        setAttachment(stack, key, ItemStack.EMPTY);
    }

    // ---- "Gun" override + "Custom" flag ----
    /** @return the override component (compound with the reference keys "Gun" and "Custom"), or null if absent */
    @Nullable
    public static CustomData getGunOverride(ItemStack stack) {
        return stack.get(ModDataComponents.GUN_OVERRIDE);
    }

    /** @return a copy of the reference's "Gun" compound, or null if the stack has no override */
    @Nullable
    public static CompoundTag getGunTag(ItemStack stack) {
        CustomData data = getGunOverride(stack);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains("Gun", CompoundTag.TAG_COMPOUND) ? tag.getCompound("Gun") : null;
    }

    public static void updateGunOverride(ItemStack stack, Consumer<CompoundTag> updater) {
        CustomData.update(ModDataComponents.GUN_OVERRIDE.get(), stack, updater);
    }

    // ---- transient state (CUSTOM_DATA, reference key names) ----
    /** @return a copy of the state compound; changes must be written back with {@link #setState} */
    public static CompoundTag getState(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void setState(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static void updateState(ItemStack stack, Consumer<CompoundTag> updater) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
    }

    /**
     * A copy of the state compound that writes itself back to the stack after every change. It stands in for
     * the reference's getOrCreateTag() in handlers that mutate the tag across many early returns. AmmoCount and the
     * other componentised keys must still go through the typed accessors above.
     */
    public static CompoundTag liveState(ItemStack stack) {
        return new LiveState(stack);
    }

    private static final class LiveState extends CompoundTag {
        private final ItemStack stack;
        private boolean ready;

        private LiveState(ItemStack stack) {
            this.stack = stack;
            this.merge(getState(stack));
            this.ready = true;
        }

        private void sync() {
            if (this.ready) setState(this.stack, this);
        }

        @Override public Tag put(String key, Tag value) { Tag t = super.put(key, value); this.sync(); return t; }
        @Override public void putByte(String key, byte value) { super.putByte(key, value); this.sync(); }
        @Override public void putShort(String key, short value) { super.putShort(key, value); this.sync(); }
        @Override public void putInt(String key, int value) { super.putInt(key, value); this.sync(); }
        @Override public void putLong(String key, long value) { super.putLong(key, value); this.sync(); }
        @Override public void putUUID(String key, UUID value) { super.putUUID(key, value); this.sync(); }
        @Override public void putFloat(String key, float value) { super.putFloat(key, value); this.sync(); }
        @Override public void putDouble(String key, double value) { super.putDouble(key, value); this.sync(); }
        @Override public void putString(String key, String value) { super.putString(key, value); this.sync(); }
        @Override public void putBoolean(String key, boolean value) { super.putBoolean(key, value); this.sync(); }
        @Override public void remove(String key) { super.remove(key); this.sync(); }
    }
}
