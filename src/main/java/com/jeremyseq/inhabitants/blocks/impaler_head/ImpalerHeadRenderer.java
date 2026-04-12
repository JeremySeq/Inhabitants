package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import org.joml.Matrix4f;

public class ImpalerHeadRenderer extends GeoBlockRenderer<ImpalerHeadBlockEntity> {
    private static final ResourceLocation NORMAL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/entity/impaler_head/impaler_head_texture.png");
    private static final ResourceLocation DRIPSTONE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/entity/impaler_head/dripstone_impaler_head_texture.png");

    public ImpalerHeadRenderer() {
        super(new ImpalerHeadModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ImpalerHeadBlockEntity animatable) {
        BlockState state = animatable.getBlockState();
        if (state.is(ModBlocks.DRIPSTONE_IMPALER_HEAD.get()) ||
            state.is(ModBlocks.DRIPSTONE_IMPALER_WALL_HEAD.get())) {
            return DRIPSTONE_TEXTURE;
        }

        return NORMAL_TEXTURE;
    }

    @Override
    public void actuallyRender(
        PoseStack poseStack,
        ImpalerHeadBlockEntity animatable,
        BakedGeoModel model,
        RenderType renderType,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight,
        int packedOverlay,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        if (!isReRender) {
            AnimationState<ImpalerHeadBlockEntity> animationState = 
                new AnimationState<>(animatable, 0, 0, partialTick, false);
            
            long instanceId = getInstanceId(animatable);
            GeoModel<ImpalerHeadBlockEntity> currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, (double)animatable.getTick(animatable));
            animationState.setData(DataTickets.BLOCK_ENTITY, animatable);
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);

            poseStack.translate(0.5, 0, 0.5);
            BlockState state = animatable.getBlockState();
            
            if (state.hasProperty(ImpalerWallHeadBlock.FACING)) {
                Direction direction = state.getValue(ImpalerWallHeadBlock.FACING);

                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - direction.toYRot()));
                poseStack.translate(0.0F, 0.1875F, -0.09375F);

            } else if (state.hasProperty(ImpalerHeadBlock.ROTATION)) {
                
                int rotation = state.getValue(ImpalerHeadBlock.ROTATION);
                float degrees = RotationSegment.convertToDegrees(rotation);

                poseStack.mulPose(Axis.YP.rotationDegrees(-degrees));
            }

            currentModel.handleAnimations(animatable, instanceId, animationState);
        }

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());
        
        updateAnimatedTextureFrame(animatable);

        for (GeoBone group : model.topLevelBones()) {
            renderRecursively(
                poseStack, animatable, group,
                renderType, bufferSource, buffer,
                isReRender, partialTick,
                packedLight, packedOverlay,
                red, green, blue, alpha
            );
        }
    }
}
