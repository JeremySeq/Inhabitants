package com.jeremyseq.inhabitants.screen_shake;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, value = Dist.CLIENT)
public class CameraRenderEvent {

    private static int shakeTicks = 0;
    private static int totalShakeTicks = 0;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        if (shakeTicks > 0) {
            PoseStack poseStack = event.getPoseStack();
            float partialTick = event.getPartialTick();

            // Smooth fade in/out using sine curve: sin(π * progress)
            float interpolatedShake = shakeTicks - partialTick;
            float progress = 1f - (interpolatedShake / totalShakeTicks);
            float fade = (float) Math.sin(Math.PI * progress); // bell curve: ramps in & out

            // Deterministic per-frame noise (no blur) using tickCounter
            double time = (tickCounter + partialTick) * 0.2; // frequency of movement
            double offsetX = Math.sin(time * 1.3) * 0.4 * fade;
            double offsetY = Math.cos(time * 1.7) * 0.15 * fade;

            // Even ticks: translation
            if (tickCounter % 4 == 0) {
                poseStack.translate(offsetX, offsetY, 0);
            }

            // Odd ticks: rotation (multi-axis)
            if (tickCounter % 4 == 2) {
                float maxAngle = 5f * fade;
                float angleX = (float)(Math.sin(time * 0.7) * 2.0f); // subtle pitch
                float angleY = (float)(Math.cos(time * 0.9) * 2.0f); // subtle yaw
                float angleZ = (float)(Math.sin(time * 1.1) * maxAngle); // stronger roll

                poseStack.mulPose(Axis.XP.rotationDegrees(angleX));
                poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
                poseStack.mulPose(Axis.ZP.rotationDegrees(angleZ));
            }

            tickCounter++;
            shakeTicks--;
        }
    }

    public static void triggerShake(int durationTicks) {
        shakeTicks = durationTicks;
        totalShakeTicks = durationTicks;
        tickCounter = 0;
    }

}
