package com.jeremyseq.inhabitants.screen_shake;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, value = Dist.CLIENT)
public class CameraRenderEvent {

    private static int shakeTicks = 0;
    private static int totalShakeTicks = 0;
    private static float targetX, targetY, targetRot;
    private static float currentX, currentY, currentRot;
    private static int noiseTick = 0;


    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (shakeTicks <= 0) return;

        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        // change targets occasionally (every 3 ticks)
        if (noiseTick++ % 3 == 0) {
            targetX = (Minecraft.getInstance().level.random.nextFloat() - 0.5f) * 0.6f;
            targetY = (Minecraft.getInstance().level.random.nextFloat() - 0.5f) * 0.4f;
            targetRot = (Minecraft.getInstance().level.random.nextFloat() - 0.5f) * 2.5f;
        }

        // smooth toward target (critical!)
        currentX += (targetX - currentX) * 0.25f;
        currentY += (targetY - currentY) * 0.25f;
        currentRot += (targetRot - currentRot) * 0.25f;

        // fade curve
        float progress = 1f - ((shakeTicks - partialTick) / totalShakeTicks);
        float fade = (float) Math.sin(Math.PI * progress);

        // apply
        poseStack.translate(currentX * fade, currentY * fade, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentRot * fade));

        shakeTicks--;
    }

    public static void triggerShake(int durationTicks) {
        shakeTicks = durationTicks;
        totalShakeTicks = durationTicks;
    }
}
