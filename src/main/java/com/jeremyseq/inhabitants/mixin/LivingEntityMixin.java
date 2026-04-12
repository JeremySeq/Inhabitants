package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Map;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    
    @Mixin(LivingEntity.class)
    public interface LivingEntityAccessor {
        @Accessor("activeEffects")
        Map<MobEffect, MobEffectInstance> inhabitants$getActiveEffects();

        @Accessor("jumping")
        boolean inhabitants$isJumping();
    }

    @Redirect(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V", 
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isUsingItem()Z"
        )
    )
    private boolean inhabitants$noSlowdownForDrill(LivingEntity entity) {
        if (entity.getUseItem().getItem() instanceof SpikeDrillItem) {
            return false;
        }
        return entity.isUsingItem();
    }

}
