package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeProjectile;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Shadow
    private static boolean loadProjectile(LivingEntity pShooter, ItemStack pCrossbowStack, ItemStack pAmmoStack, boolean pHasAmmo, boolean pIsCreative) {
        return false;
    }

    @Inject(method="getAllSupportedProjectiles", at=@At("HEAD"), cancellable = true)
    private void getAllSupportedProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        cir.setReturnValue(ProjectileWeaponItem.ARROW_ONLY.or((item) -> item.is(ModItems.IMPALER_SPIKE.get())));
        cir.cancel();
    }

    @Inject(method="tryLoadProjectiles", at=@At("HEAD"), cancellable = true)
    private static void loadProjectile(LivingEntity pShooter, ItemStack pCrossbowStack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack projectileStack = pShooter.getProjectile(pCrossbowStack);
        if (!projectileStack.is(ModItems.IMPALER_SPIKE.get())) return;

        int multishotLevel = pCrossbowStack.getEnchantmentLevel(Enchantments.MULTISHOT);
        int projectileCount = multishotLevel == 0 ? 1 : 3;

        boolean isCreative = pShooter instanceof Player player && player.getAbilities().instabuild;
        ItemStack original = projectileStack.copy();

        for (int i = 0; i < projectileCount; ++i) {
            if (i > 0) {
                projectileStack = original.copy();
            }

            if (projectileStack.isEmpty() && isCreative) {
                projectileStack = new ItemStack(ModItems.IMPALER_SPIKE.get());
                original = projectileStack.copy();
            }

            if (!loadProjectile(pShooter, pCrossbowStack, projectileStack, i > 0, isCreative)) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "shootProjectile", at = @At("HEAD"), cancellable = true)
    private static void onShoot(Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pCrossbowStack, ItemStack pAmmoStack, float pSoundPitch, boolean pIsCreativeMode, float pVelocity, float pInaccuracy, float pProjectileAngle, CallbackInfo ci) {
        if (pAmmoStack.is(ModItems.IMPALER_SPIKE.get())) {

            // shoot impaler spike
            if (!pLevel.isClientSide) {
                ImpalerSpikeProjectile spike = new ImpalerSpikeProjectile(ModEntities.IMPALER_SPIKE_PROJECTILE.get(), pShooter, pLevel);

                Vec3 vec31 = pShooter.getUpVector(1.0F);
                Quaternionf quaternionf = (new Quaternionf()).setAngleAxis(pProjectileAngle * ((float)Math.PI / 180F), vec31.x, vec31.y, vec31.z);
                Vec3 vec3 = pShooter.getViewVector(1.0F);
                Vector3f vector3f = vec3.toVector3f().rotate(quaternionf);
                spike.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity, pInaccuracy);

                // if not in creative mode, consume one item
                if (!pIsCreativeMode) {
                    pAmmoStack.shrink(1);
                }

                pCrossbowStack.hurtAndBreak(1, pShooter, (p_40858_) -> p_40858_.broadcastBreakEvent(pHand));

                pLevel.addFreshEntity(spike);
            }

            pLevel.playSound(null, pShooter.getX(), pShooter.getY(), pShooter.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, pSoundPitch);

            ci.cancel();
        }
    }
}
