package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.blocks.entity.ConcherShellBlockEntity;
import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConcherShellBlock extends HorizontalDirectionalBlock implements EntityBlock {
    protected static final VoxelShape STAGE_1_SHAPE = Block.box(1.0d, 0.0d, 1.0d, 15.0d, 9.0d, 15.0d);

    protected static final VoxelShape STAGE_2_SHAPE = Shapes.or(
            Block.box(-4.0d, 0.0d, -4.0d, 20.0d, 11.0d, 20.0d),
            Block.box(1.0d, 11.0d, 1.0d, 15.0d, 20.0d, 15.0d)
    );

    protected static final VoxelShape STAGE_3_SHAPE = Shapes.or(
            Block.box(-8.0d, 0.0d, -8.0d, 24.0d, 13.0d, 24.0d),
            Block.box(-4.0d, 13.0d, -4.0d, 20.0d, 24.0d, 20.0d),
            Block.box(1.0d, 24.0d, 1.0d, 15.0d, 32.0d, 15.0d)
    );

    public ConcherShellBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.is(ModBlocks.CONCHER_SHELL_BLOCK_STAGE_2.get())) {
            return STAGE_2_SHAPE;
        }
        if (state.is(ModBlocks.CONCHER_SHELL_BLOCK_STAGE_3.get())) {
            return STAGE_3_SHAPE;
        }
        return STAGE_1_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ConcherShellBlockEntity(pos, state);
    }
}
