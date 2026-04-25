package com.jeremyseq.inhabitants.debug;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Matrix4f;

public class ConcherDebugRenderer {

    public static void renderStateLabel(ConcherEntity entity, PoseStack poseStack,
    MultiBufferSource bufferSource, 
    EntityRenderDispatcher dispatcher, Font font, int packedLight) {

        String stateText = "State: " + entity.getAIState();
        stateText += " | Stage: " + entity.getStage();

        boolean isWalking = entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001;
        boolean isRotating = Math.abs(entity.yRotO - entity.getYRot()) > 0.05f;

        stateText += " | Walking: " + isWalking;
        stateText += " | Rotating: " + isRotating;
        stateText += " | Targeting: " + entity.getAI().isTargeting();

        Component label = Component.literal(stateText);
        float height = entity.getBbHeight() + 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = poseStack.last().pose();
        
        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);

        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        float textX = (float) (-font.width(label) / 2);

        font.drawInBatch(label, textX, 0, -1, false, matrix4f, bufferSource,
            Font.DisplayMode.NORMAL, backgroundColor, packedLight);

        poseStack.popPose();
    }
}
