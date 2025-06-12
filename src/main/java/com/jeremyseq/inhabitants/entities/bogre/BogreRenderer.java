package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class BogreRenderer extends GeoEntityRenderer<BogreEntity> {
    public BogreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreModel());
        this.shadowRadius = 1.25f;
        addRenderLayer(new HeldItemLayer(this));
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, BogreEntity animatable,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float scale = 1.33f;
        poseStack.scale(scale, scale, scale);
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BogreEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre.png");
    }

    private static class HeldItemLayer extends BlockAndItemGeoLayer<BogreEntity> {
        public HeldItemLayer(GeoEntityRenderer<BogreEntity> renderer) {
            super(renderer);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, BogreEntity animatable) {
            return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        @Override
        protected @Nullable ItemStack getStackForBone(GeoBone bone, BogreEntity animatable) {
            if ("fish".equals(bone.getName()) && !animatable.getAnimateFishHeld().isEmpty()) {
                return animatable.getFishHeld();
            }

            return super.getStackForBone(bone, animatable);
        }
    }
}
