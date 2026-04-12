package com.jeremyseq.inhabitants.blocks.impaler_head;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.*;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ImpalerWallHeadBlock extends AbstractImpalerHeadBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    protected static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.NORTH, Block.box(4.0D, 4.0D, 8.0D, 12.0D, 12.0D, 16.0D),
        Direction.SOUTH, Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 8.0D),
        Direction.WEST, Block.box(8.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D),
        Direction.EAST, Block.box(0.0D, 4.0D, 4.0D, 8.0D, 12.0D, 12.0D)
    );

    public ImpalerWallHeadBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false));
    }

    @Override
    public @NotNull VoxelShape getShape(
        BlockState pState,
        BlockGetter pLevel,
        BlockPos pPos,
        CollisionContext pContext
    ) {
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState state = this.defaultBlockState();
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        
        for(Direction direction : pContext.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                Direction opposite = direction.getOpposite();
                state = state.setValue(FACING, opposite);
                
                if (!level.getBlockState(pos.relative(direction)).canBeReplaced(pContext)) {
                    return state.setValue(POWERED, level.hasNeighborSignal(pos));
                }
            }
        }

        return state.setValue(POWERED, level.hasNeighborSignal(pos));
    }

    @Override
    public @NotNull BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED);
    }
}
