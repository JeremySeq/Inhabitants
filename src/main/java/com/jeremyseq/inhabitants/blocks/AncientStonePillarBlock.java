package com.jeremyseq.inhabitants.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;

public class AncientStonePillarBlock extends Block {

    // 0 = middle, 1 = top, 2 = bottom
    public static final IntegerProperty END = IntegerProperty.create("end", 0, 2);

    public AncientStonePillarBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(END, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(END);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        LevelAccessor level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return calculateEndState(this.defaultBlockState(), level, pos);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction dir, @NotNull BlockState neighbor,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {

        if (dir == Direction.UP || dir == Direction.DOWN) {
            return calculateEndState(state, level, pos);
        }

        return state;
    }

    private BlockState calculateEndState(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        boolean hasAbove = above.getBlock() == this;
        boolean hasBelow = below.getBlock() == this;

        int end;

        if (hasAbove && hasBelow) {
            end = 0; // middle piece
        } else if (hasBelow) {
            end = 1; // top piece
        } else if (hasAbove) {
            end = 2; // bottom piece
        } else {
            end = 0; // single pillar = treat as middle
        }

        return state.setValue(END, end);
    }
}
