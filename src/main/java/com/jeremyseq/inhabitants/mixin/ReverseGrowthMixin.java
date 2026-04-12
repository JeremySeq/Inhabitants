package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.particles.ModParticles;
import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;

public class ReverseGrowthMixin {
    private static final float shrinkScale = 0.5f;

    @Mixin(Player.class)
    public abstract static class Physics {
        private boolean hadReverseGrowth;

        private boolean hasReverseGrowth() {
            Map<MobEffect, MobEffectInstance> effects =
                ((LivingEntityMixin.LivingEntityAccessor) this).inhabitants$getActiveEffects();
            
            return effects != null && effects.containsKey(ModEffects.REVERSE_GROWTH.get());
        }

        @Inject(
            method = "tick",
            at = @At("HEAD")
        )
        private void inhabitants$tickReverseGrowth(CallbackInfo ci) {
            Player player = (Player) (Object) this;
            boolean hasEffect = hasReverseGrowth();

            if (hasEffect != hadReverseGrowth) {
                if (hadReverseGrowth && !hasEffect) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSoundEvents.REVERSE_GROWTH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                    if (player.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 20; i++) {
                            double px = player.getX() + (player.level().random.nextDouble() - 0.5) * 1.0;
                            double py = player.getY() + player.level().random.nextDouble() * player.getBbHeight();
                            double pz = player.getZ() + (player.level().random.nextDouble() - 0.5) * 1.0;
                            serverLevel.sendParticles(ModParticles.ABRACADABRA.get(), px, py, pz, 1, 0, 0, 0, 0.05);
                        }
                    }
                }
                hadReverseGrowth = hasEffect;
                player.refreshDimensions();
            }
        }

        @Inject(
            method = "getDimensions",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$shrinkPlayer(Pose pose,
            CallbackInfoReturnable<EntityDimensions> callbackInfoReturnable) {
            if (hasReverseGrowth()) {
                EntityDimensions original = callbackInfoReturnable.getReturnValue();
                callbackInfoReturnable.setReturnValue(EntityDimensions.scalable(
                        original.width * shrinkScale,
                        original.height * shrinkScale
                ));
            }
        }

        @Inject(
            method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$reverseGrowthEyeHeight(Pose pose, EntityDimensions dimensions,
            CallbackInfoReturnable<Float> callbackInfoReturnable) {
            if (hasReverseGrowth()) {
                callbackInfoReturnable.setReturnValue(callbackInfoReturnable.getReturnValue() * shrinkScale);
            }
        }
    }

    @Mixin(PlayerRenderer.class)
    public static class Client {
        @Inject(
            method = "scale",
            at = @At("HEAD")
        )
        private void inhabitants$shrinkScale(AbstractClientPlayer entity, PoseStack poseStack, float partialTickTime, CallbackInfo callbackInfo) {
            if (entity.hasEffect(ModEffects.REVERSE_GROWTH.get())) {
                poseStack.scale(shrinkScale, shrinkScale, shrinkScale);
            }
        }
    }
}
