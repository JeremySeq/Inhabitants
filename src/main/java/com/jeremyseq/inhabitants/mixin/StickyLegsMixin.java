package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class StickyLegsMixin {

    @Inject(
        method = "onClimbable",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inhabitants$stickyLegsClimb(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {

        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && player.hasEffect(ModEffects.STICKY_LEGS.get())
            && player.horizontalCollision) {
                
            callbackInfoReturnable.setReturnValue(true);
        }
    }
}
