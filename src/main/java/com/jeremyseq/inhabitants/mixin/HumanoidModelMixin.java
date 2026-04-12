package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.items.javelin.JavelinItem;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(
        method = "setupAnim*",
        at = @At("TAIL")
    )
    private void inhabitants$animateArm(
        T pEntity, 
        float pLimbSwing, 
        float pLimbSwingAmount, 
        float pAgeInTicks, 
        float pNetHeadYaw, 
        float pHeadPitch, 
        CallbackInfo ci) {
        
        if (pEntity.isUsingItem() && pEntity.getUseItem().getItem() instanceof JavelinItem) {

            float useDuration = (float)pEntity.getUseItem().getUseDuration();
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float remainingTicks = (float)pEntity.getUseItemRemainingTicks() - partialTicks + 1.0f;
            float chargeProgress = Mth.clamp((useDuration - remainingTicks) / 60.0f, 0.0f, 1.0f);
            
            ModelPart arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;

            if (pEntity.getUsedItemHand() == InteractionHand.OFF_HAND) {
                arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
            }
            
            inhabitants$applyJavelinArmRaise(arm, chargeProgress);
            inhabitants$applyJavelinArmShake(arm, pAgeInTicks, chargeProgress);
        }

        if (pEntity.isUsingItem() &&
            pEntity.getUseItem().getItem() instanceof SpikeDrillItem) {
            
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float time = pAgeInTicks + partialTicks;

            ModelPart arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;
            if (pEntity.getUsedItemHand() == InteractionHand.OFF_HAND) {
                arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
            }

            int mainSide = (arm == rightArm) ? 1 : -1;

            inhabitants$applyDrillPose(arm, time, mainSide);
        }
        
        if (pEntity instanceof Player player &&
            pEntity.attackAnim > 0 &&
            player.getCooldowns().isOnCooldown(ModItems.JAVELIN.get())) {
            
            inhabitants$applyJavelinArmThrow(pEntity);
        }
    }

    @Unique
    private void inhabitants$applyJavelinArmRaise(ModelPart arm, float chargeProgress) {
        float raiseLerp = Mth.clamp(chargeProgress * 5.0f, 0.0f, 1.0f);
        arm.xRot = Mth.lerp(raiseLerp, 0.0f, arm.xRot);
        arm.zRot += (1.0f - raiseLerp) * -0.2f;
    }

    @Unique
    private void inhabitants$applyJavelinArmThrow(T entity) {
        float swingProgress = entity.getAttackAnim(Minecraft.getInstance().getFrameTime());
        ModelPart arm = entity.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;

        if (entity.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND) {
            arm = entity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
        }

        arm.xRot = -2.0f + (swingProgress * 2.8f);
        arm.zRot += Mth.sin(swingProgress * (float)Math.PI) * 0.2f;
    }


    @Unique
    private void inhabitants$applyJavelinArmShake(
        ModelPart arm,
        float ageInTicks,
        float chargeProgress) {
        if (chargeProgress > 0.2f) {
            float intensity = (chargeProgress - 0.2f) * 0.015f;
            float shakeX = Mth.sin(ageInTicks * 3.5f) * intensity;
            float shakeY = Mth.cos(ageInTicks * 4.0f) * intensity;

            arm.xRot += shakeX;
            arm.zRot += shakeY;
        }
    }

    @Unique
    private void inhabitants$applyDrillPose(ModelPart arm, float time, int mainSide) {
        float targetX = -0.8f;
        float targetZ = mainSide * -0.15f; 

        float stab = Math.max(0, Mth.sin(time * (float)(Math.PI / 4.0))) * 0.12f;

        float vX = Mth.sin(time * 4.5f) * 0.010f;
        float vZ = Mth.cos(time * 7.0f) * 0.006f;

        arm.xRot = Mth.lerp(0.4f, arm.xRot, targetX - stab + vX);
        arm.zRot = Mth.lerp(0.4f, arm.zRot, targetZ + vZ);
    }
}
