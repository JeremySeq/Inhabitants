package com.jeremyseq.inhabitants.blocks.impaler_head;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

import org.jetbrains.annotations.NotNull;

public class ImpalerHeadBlock extends AbstractImpalerHeadBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    public ImpalerHeadBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ROTATION, 0)
            .setValue(POWERED, false));
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState pState,
        @NotNull BlockGetter pLevel,
        @NotNull BlockPos pPos,
        @NotNull CollisionContext pContext
    ) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState()
            .setValue(ROTATION, RotationSegment.convertToSegment(pContext.getRotation()))
            .setValue(POWERED, pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
    }

    @Override
    public @NotNull BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(ROTATION, pRotation.rotate(pState.getValue(ROTATION), 16));
    }

    @Override
    public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(ROTATION, pMirror.mirror(pState.getValue(ROTATION), 16));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ROTATION, POWERED);
    }
}
