package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

import org.jetbrains.annotations.*;

public abstract class AbstractImpalerHeadBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected AbstractImpalerHeadBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new ImpalerHeadBlockEntity(pPos, pState);
    }

    @Override
    public void neighborChanged(
        @NotNull BlockState pState,
        Level pLevel,
        @NotNull BlockPos pPos,
        @NotNull Block pBlock,
        @NotNull BlockPos pFromPos,
        boolean pIsMoving
    ) {
        if (!pLevel.isClientSide) {
            boolean isPowered = pLevel.hasNeighborSignal(pPos);
            if (isPowered != pState.getValue(POWERED)) {
                pLevel.setBlock(pPos, pState.setValue(POWERED, isPowered), 3);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level pLevel,
        @NotNull BlockState pState,
        @NotNull BlockEntityType<T> pBlockEntityType
    ) {
        return createTickerHelper(pBlockEntityType,
            ModBlockEntities.IMPALER_HEAD_BLOCK_ENTITY.get(),
            ImpalerHeadBlockEntity::tick);
    }
}
