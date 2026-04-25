package com.jeremyseq.inhabitants.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ConcherShellBlockEntity extends BlockEntity {
    public ConcherShellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONCHER_SHELL_BLOCK_ENTITY.get(), pos, state);
    }
}
