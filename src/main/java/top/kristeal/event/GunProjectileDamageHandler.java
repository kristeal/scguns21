package top.kristeal.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import top.kristeal.Reference;
import top.kristeal.common.GunStackData;
import top.kristeal.entity.projectile.ProjectileEntity;
import top.kristeal.item.GunItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The reference's three common mixins, moved to NeoForge events.
 * <ul>
 *     <li>{@code LivingEntityMixin}: a gun projectile's hit knocks back with {@link ProjectileEntity#getModifiedKnockback()}
 *     instead of vanilla's 0.4, with a 10-tick per-entity cooldown during which the knockback is 0. The mixin remembered the
 *     damage source at the head of {@code hurt} and changed the {@code knockback} call inside it; here the source is
 *     remembered in {@link LivingIncomingDamageEvent} (fired at the start of {@code hurt}) and used by the next
 *     {@link LivingKnockBackEvent} for that entity in the same tick.</li>
 *     <li>{@code SittingPhaseMixin}: a perched Ender Dragon takes no damage from gun projectiles (the reference set the
 *     sitting phase's {@code onHurt} result to 0, which makes {@code EnderDragon#hurt} ignore the hit).</li>
 *     <li>{@code EndPortalBlockMixin}: a gun thrown through the End's exit portal comes back with {@code Scale} 2.0, which
 *     {@code GunRenderingHandler#applyWeaponScale} reads from the gun's state.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunProjectileDamageHandler {
    private static final int KNOCKBACK_COOLDOWN_TICKS = 10; // 0.5 seconds

    private static final Map<UUID, Long> KNOCKBACK_COOLDOWNS = new HashMap<>();
    private static final Map<LivingEntity, PendingHit> PENDING_HITS = new WeakHashMap<>();

    private record PendingHit(DamageSource source, long gameTime) {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        // SittingPhaseMixin
        if (entity instanceof EnderDragon dragon && source.getDirectEntity() instanceof ProjectileEntity
                && dragon.getPhaseManager().getCurrentPhase().isSitting()) {
            event.setCanceled(true);
            return;
        }

        // LivingEntityMixin#onHurtStart
        if (!entity.level().isClientSide) {
            PENDING_HITS.put(entity, new PendingHit(source, entity.level().getGameTime()));
        }
    }

    // LivingEntityMixin#modifyApplyKnockbackArgs
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        PendingHit hit = PENDING_HITS.remove(entity);
        if (hit == null || !(hit.source().getDirectEntity() instanceof ProjectileEntity projectile)) {
            return;
        }

        long currentTick = entity.level().getGameTime();
        if (hit.gameTime() != currentTick) {
            return;
        }

        UUID entityId = entity.getUUID();
        Long lastKnockbackTick = KNOCKBACK_COOLDOWNS.get(entityId);
        if (lastKnockbackTick != null && (currentTick - lastKnockbackTick) < KNOCKBACK_COOLDOWN_TICKS) {
            event.setStrength(0.0F);
            return;
        }
        KNOCKBACK_COOLDOWNS.put(entityId, currentTick);
        KNOCKBACK_COOLDOWNS.entrySet().removeIf(entry -> (currentTick - entry.getValue()) > 100);

        event.setStrength(projectile.getModifiedKnockback());
    }

    // EndPortalBlockMixin: the reference hooked EndPortalBlock#entityInside right before changeDimension
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level().dimension() == Level.END && event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof GunItem) {
                ItemStack gun = stack.copy();
                GunStackData.updateState(gun, tag -> tag.putFloat("Scale", 2.0F));
                itemEntity.setItem(gun);
            }
        }
    }
}
