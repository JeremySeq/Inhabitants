package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.events.ModClientEvents;
import com.jeremyseq.inhabitants.util.GhostTracker;
import com.jeremyseq.inhabitants.util.GhostTracker.GhostState;
import com.jeremyseq.inhabitants.util.GhostRenderUtils;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Unique
    private static boolean inhabitants$isGhostRendering = false;

    // handle concussion trail rendering
    @SuppressWarnings("unchecked")
    @Inject(
        method = "render", 
        at = @At("TAIL")
    )
    private void inhabitants$renderConcussionTrail(T pEntity, float pEntityYaw,
        float pPartialTick, PoseStack pMatrixStack, MultiBufferSource pBuffer,
        int pPackedLight, CallbackInfo ci) {
        
        if (inhabitants$isGhostRendering) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null &&
            mc.player.hasEffect(ModEffects.CONCUSSION.get())) {
            
            List<GhostState> history = GhostTracker.getHistory(pEntity.getUUID());

            if (history == null || history.isEmpty()) return;

            inhabitants$isGhostRendering = true;
            
            ResourceLocation texture = ((EntityRenderer<T>)(Object)this).getTextureLocation(pEntity);
            RenderType ghostType = RenderType.entityTranslucent(texture);
            
            double curX = Mth.lerp(pPartialTick, pEntity.xo, pEntity.getX());
            double curY = Mth.lerp(pPartialTick, pEntity.yo, pEntity.getY());
            double curZ = Mth.lerp(pPartialTick, pEntity.zo, pEntity.getZ());
            
            int renderedCount = 0;
            for (int i = 1; i < history.size() && renderedCount < 2; i += 2) {
                GhostState state = history.get(i);
                renderedCount++;
                
                pMatrixStack.pushPose();
                
                pMatrixStack.translate(
                    state.pos().x - curX,
                    state.pos().y - curY,
                    state.pos().z - curZ
                );
                
                float deltaYaw = state.camYaw() - mc.player.getYRot();
                float deltaPitch = state.camPitch() - mc.player.getXRot();
                
                while (deltaYaw < -180.0F) deltaYaw += 360.0F;
                while (deltaYaw >= 180.0F) deltaYaw -= 360.0F;
                
                float rotationLagFactor = 0.05F * ModClientEvents.muffleLerp;
                pMatrixStack.translate(deltaYaw * rotationLagFactor, -deltaPitch * rotationLagFactor, 0);
                
                float alphaBase = ModClientEvents.muffleLerp * 0.2F;
                float fade = 1.0F - ((float)i / history.size());
                float alpha = alphaBase * fade;
                
                MultiBufferSource ghostSource = new GhostRenderUtils.GhostBufferSource(pBuffer, alpha);

                EntityRenderer<T> renderer = (EntityRenderer<T>) (Object) this;

                renderer.render(pEntity, state.yRot(),
                    pPartialTick, pMatrixStack,
                    ghostSource, pPackedLight);
                
                pMatrixStack.popPose();
            }
            
            inhabitants$isGhostRendering = false;
        }
    }

    @Inject(
        method = "renderNameTag", 
        at = @At("HEAD"), 
        cancellable = true
    )
    private void inhabitants$skipGhostNameTag(T pEntity, Component pDisplayName,
        PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight,
        CallbackInfo ci) {
        if (inhabitants$isGhostRendering) {
            ci.cancel();
        }
    }
}
