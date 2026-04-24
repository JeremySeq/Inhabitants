package com.jeremyseq.inhabitants.entities.concher.render;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ConcherRenderer extends GeoEntityRenderer<ConcherEntity> {
    public ConcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ConcherModel());
        
        //blinking eyes
        this.addRenderLayer(new ConcherEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ConcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            String.format("textures/entity/concher_%d.png", animatable.getStage()));
    }

    private static class ConcherEyesLayer extends GeoRenderLayer<ConcherEntity> {
        public ConcherEyesLayer(GeoEntityRenderer<ConcherEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(
            PoseStack poseStack,
            ConcherEntity animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
        ) {
                
            // TODO:  layer of eyes blinking
            if (animatable.isBlinking()) {
                
            } else {
                
            }
        }
    }
}
