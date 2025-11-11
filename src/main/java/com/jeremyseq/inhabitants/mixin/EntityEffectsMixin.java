package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class EntityEffectsMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at=@At("HEAD"), cancellable = true)
    private void tickEffectsInject(MobEffectInstance pEffectInstance, Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (pEntity instanceof LivingEntity livingEntity) {
            if (livingEntity.hasEffect(ModEffects.IMMUNITY.get())) {
                cir.cancel();
                cir.setReturnValue(false);
            }
        }
    }
}
