package com.jeremyseq.inhabitants.blocks.entity.render;

import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.blocks.entity.ConcherShellBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.NotNull;

public class ConcherShellRenderer implements BlockEntityRenderer<ConcherShellBlockEntity> {
    public ConcherShellRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        ConcherShellBlockEntity blockEntity,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        BlockState state = blockEntity.getBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);

        poseStack.pushPose();
        
        if (state.is(ModBlocks.CONCHER_SHELL_BLOCK_STAGE_3.get())) {
            poseStack.translate(0, 1.0, 0);
        }

        dispatcher.getModelRenderer().renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.cutout()),
            state, model,
            1.0F, 1.0F, 1.0F,
            packedLight, packedOverlay,
            ModelData.EMPTY,
            RenderType.cutout()
        );

        poseStack.popPose();
    }
}
