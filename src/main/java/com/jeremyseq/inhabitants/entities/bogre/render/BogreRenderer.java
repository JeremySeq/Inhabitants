package com.jeremyseq.inhabitants.entities.bogre.render;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.debug.BogreDebugRenderer;
import com.jeremyseq.inhabitants.debug.DevMode;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BogreRenderer extends GeoEntityRenderer<BogreEntity> {
    private static final float scale = 1.25f;

    public BogreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreModel());
        this.shadowRadius = 1.25f;
        this.withScale(scale);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new HeldItemLayer(this));
    }

    @Override
    public void render(@NotNull BogreEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        this.getGeoModel().getBone("mouthPoint").ifPresent(bone -> {
            entity.clientMouthPos = new Vec3(
                    bone.getWorldPosition().x,
                    bone.getWorldPosition().y,
                    bone.getWorldPosition().z
            );
        });

        if (DevMode.bogreStates()) {
            BogreDebugRenderer.renderStateLabel(entity, poseStack, bufferSource,
            this.entityRenderDispatcher, this.getFont(), packedLight);
        }
    }

    @Override
    public RenderType getRenderType(BogreEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void postRender(PoseStack poseStack, BogreEntity animatable, BakedGeoModel model,
    MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
    int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BogreEntity animatable) {
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
            if ("item".equals(bone.getName()) && !animatable.getItemHeld().isEmpty()) {
                return animatable.getItemHeld();
            }

            return super.getStackForBone(bone, animatable);
        }
    }
}
