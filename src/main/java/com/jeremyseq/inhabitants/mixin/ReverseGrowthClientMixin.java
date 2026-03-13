package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class ReverseGrowthClientMixin {

    private static final float shrinkScale = 0.5f;

    @Inject(method = "scale", at = @At("HEAD"))
    private void shrinkScale(AbstractClientPlayer entity, PoseStack poseStack, float partialTickTime,
        CallbackInfo callbackInfo) {
        
        if (entity.hasEffect(ModEffects.REVERSE_GROWTH.get())) {
            poseStack.scale(shrinkScale, shrinkScale, shrinkScale);
        }
    }
}
