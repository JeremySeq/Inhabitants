package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.events.ModClientEvents;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ConcussionMixins {
    
    // handles movement speed reduction
    @Mixin(LivingEntity.class)
    public static abstract class Movement {
        @Inject(
            method = "getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$concussionDynamicAttribute(Attribute pAttribute, CallbackInfoReturnable<Double> cir) {
            LivingEntity living = (LivingEntity) (Object) this;

            if (pAttribute == Attributes.MOVEMENT_SPEED && living.hasEffect(ModEffects.CONCUSSION.get())) {
                cir.setReturnValue(cir.getReturnValue() * 0.7);
            }
        }

        @Inject(
            method = "getSpeed",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$concussionDynamicSpeed(CallbackInfoReturnable<Float> cir) {
            LivingEntity living = (LivingEntity) (Object) this;

            if (living.hasEffect(ModEffects.CONCUSSION.get())) {
                cir.setReturnValue(cir.getReturnValue() * 0.7F);
            }
        }
    }
    
    // handles camera rotation lag or "smoothness"
    @Mixin(Entity.class)
    public static abstract class ClientInput {
        @ModifyVariable(
            method = "turn(DD)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
        )
        private double inhabitants$concussionYawLag(double yaw) {
            if ((Object) this instanceof LocalPlayer player) {
                if (player.hasEffect(ModEffects.CONCUSSION.get()) &&
                    ModClientEvents.muffleLerp > 0.0F) {

                    return yaw * (1.0 - (ModClientEvents.muffleLerp * 0.4));
                }
            }
            return yaw;
        }

        @ModifyVariable(
            method = "turn(DD)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
        )
        private double inhabitants$concussionPitchLag(double pitch) {

            if ((Object) this instanceof LocalPlayer player) {
                if (player.hasEffect(ModEffects.CONCUSSION.get()) &&
                    ModClientEvents.muffleLerp > 0.0F) {
                    return pitch * (1.0 - (ModClientEvents.muffleLerp * 0.4));
                }
            }
            return pitch;
        }
    }
}
