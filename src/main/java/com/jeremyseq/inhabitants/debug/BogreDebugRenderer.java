package com.jeremyseq.inhabitants.debug;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.skill.CookingSkill;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Matrix4f;

public class BogreDebugRenderer {

    /**
     * Called by BogreRenderer.render() to visualize the state.
     */
    public static void renderStateLabel(BogreEntity entity, PoseStack poseStack,
    MultiBufferSource bufferSource, 
    EntityRenderDispatcher dispatcher, Font font, int packedLight) {

        String stateText = "State: " + entity.getAIState();

        if (entity.getAIState() == BogreAi.State.NEUTRAL) {
            stateText += " (" + entity.getNeutralState() + ")";
        } else if (entity.getAIState() == BogreAi.State.AGGRESSIVE) {
            stateText += " (" + entity.getAggressiveState() + ")";
        } else if (entity.getAIState() == BogreAi.State.SKILLING) {
            stateText += " (" + entity.getCraftingState() + ")";
            if (entity.getCraftingState() == BogreAi.SkillingState.DELIVERING) {
                stateText += " (" + entity.getAi().getDeliveryState() + ")";
            }
        }

        if (entity.getAi().hasProgress()) {
            int ticks = entity.getAiTicks();
            int total = entity.getEntityData().get(BogreEntity.SKILL_DURATION);
            
            BogreAi.SkillingState skillState = entity.getCraftingState();
            if (skillState == BogreAi.SkillingState.DELIVERING) {
                total = switch (entity.getAi().getDeliveryState()) {
                    case OPENING_CHEST -> CookingSkill.chestOpeningDuration;
                    case DEPOSITING -> CookingSkill.chestDepositingDuration;
                    case CLOSING_CHEST -> CookingSkill.chestClosingDuration;
                    default -> 0;
                };
            }
            
            if (total > 0) {
                stateText += " [" + ticks + "/" + total + "]";
            }
            
            if (entity.getAIState() == BogreAi.State.SKILLING && 
                (skillState == BogreAi.SkillingState.CARVING || skillState == BogreAi.SkillingState.TRANSFORMATION)) {
                int hits = entity.getEntityData().get(BogreEntity.SKILL_HITS);
                int totalHits = entity.getEntityData().get(BogreEntity.HAMMER_HITS);
                stateText += " [" + hits + "/" + totalHits + "]";
            }
        }

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