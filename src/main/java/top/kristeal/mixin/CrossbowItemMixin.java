package top.kristeal.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.kristeal.item.ICrossbowAmmo;

import java.util.function.Predicate;

//Crossbow mixin for grenadessssss
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin
{
    private static final Predicate<ItemStack> GRENADE_AMMO = stack -> stack.getItem() instanceof ICrossbowAmmo;

    public Predicate<ItemStack> getSupportedHeldProjectiles()
    {
        return ProjectileWeaponItem.ARROW_OR_FIREWORK.or(GRENADE_AMMO);
    }

    public Predicate<ItemStack> getAllSupportedProjectiles()
    {
        return ProjectileWeaponItem.ARROW_ONLY.or(GRENADE_AMMO);
    }

    @Inject(method = "createProjectile", at = @At("HEAD"), cancellable = true)
    private void scguns$createGrenadeProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit, CallbackInfoReturnable<Projectile> cir)
    {
        if (ammo.getItem() instanceof ICrossbowAmmo crossbowAmmo)
        {
            cir.setReturnValue(crossbowAmmo.createThrowable(level, shooter, ammo));
        }
    }

    @Inject(method = "getShootingPower", at = @At("RETURN"), cancellable = true)
    private static void scguns$grenadeShootingPower(ChargedProjectiles projectiles, CallbackInfoReturnable<Float> cir)
    {

        if (projectiles.getItems().stream().anyMatch(stack -> stack.getItem() instanceof ICrossbowAmmo))
        {
            cir.setReturnValue(2.3F); // Grenade velocity adjust if doesnt look good
        }
    }
}
