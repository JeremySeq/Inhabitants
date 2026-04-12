package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.javelin.JavelinItem;
import com.jeremyseq.inhabitants.animation.FPVAnimationPlayer;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(
        method = "renderArmWithItem", 
        at = @At("HEAD")
    )
    private void inhabitants$animateArm(AbstractClientPlayer pPlayer,
        float pPartialTick, 
        float pPitch, 
        InteractionHand pHand, 
        float pSwingProgress, 
        ItemStack pStack, 
        float pEquipProgress, 
        PoseStack pPoseStack, 
        MultiBufferSource pBuffer, 
        int pCombinedLight, 
        CallbackInfo ci) {
        
        if (pStack.getItem() instanceof JavelinItem &&
            pPlayer.isUsingItem() && 
            pPlayer.getUseItemRemainingTicks() > 0 && 
            pPlayer.getUsedItemHand() == pHand) {
            
            float useDuration = (float)pStack.getUseDuration();
            float remainingTicks = (float)pPlayer.getUseItemRemainingTicks() - pPartialTick + 1.0f;
            float chargeProgress = Mth.clamp((useDuration - remainingTicks) / 60.0f, 0.0f, 1.0f);

            inhabitants$applyArmAim(pPoseStack, chargeProgress);
            inhabitants$applyArmShake(pPoseStack, pPlayer, pPartialTick, chargeProgress);
        }
        
        if (!(pStack.getItem() instanceof SpikeDrillItem)) {
            FPVAnimationPlayer.INSTANCE.apply(
                null,
                pPoseStack,
                (LocalPlayer)pPlayer,
                pHand == InteractionHand.MAIN_HAND ?
                    pPlayer.getMainArm() : pPlayer.getMainArm().getOpposite(),
                pStack,
                pPartialTick,
                pEquipProgress,
                false
            );
        }
    }

    @Unique
    private void inhabitants$applyArmAim(PoseStack poseStack, float chargeProgress) {
        float xOffset = 0.05f - (chargeProgress * 0.15f);
        float yOffset = 0.05f + (chargeProgress * 0.05f);
        poseStack.translate(xOffset, yOffset, 0);
    }

    @Unique
    private void inhabitants$applyArmShake(
        PoseStack poseStack, 
        AbstractClientPlayer player, 
        float partialTick, 
        float chargeProgress) {
        
        if (chargeProgress > 0.15f) {
            float intensity = (chargeProgress - 0.15f) * 0.01f;
            float time = (float)player.tickCount + partialTick;

            float shakeX = Mth.sin(time * 3.5f) * intensity;
            float shakeY = Mth.cos(time * 3.5f) * intensity;
            float shakeZ = Mth.sin(time * 4.0f) * intensity;

            poseStack.translate(shakeX, shakeY, shakeZ);
        }
    }
}
