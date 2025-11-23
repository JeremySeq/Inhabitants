package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.blocks.entity.ScrollBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScrollBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 2);

    protected static final int HEIGHT_SMALL = 24;
    protected static final int HEIGHT_MEDIUM = 38;
    protected static final int HEIGHT_LARGE = 46;

    protected ScrollBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIZE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(SIZE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int size = 1; // default medium
        ItemStack stack = ctx.getItemInHand();
        if (stack.getItem() == ModBlocks.SCROLL_SMALL.get()) size = 0;
        else if (stack.getItem() == ModBlocks.SCROLL_LARGE.get()) size = 2;

        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(SIZE, size);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new ScrollBlockEntity(pPos, pState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        int height = switch (pState.getValue(SIZE)) {
            case 0 -> HEIGHT_SMALL;
            case 2 -> HEIGHT_LARGE;
            default -> HEIGHT_MEDIUM;
        };

        return switch (pState.getValue(FACING)) {
            case SOUTH -> Block.box(0, 16-height, 0, 16, 16, 3);
            case EAST  -> Block.box(0, 16-height, 0, 3, 16, 16);
            case WEST  -> Block.box(13, 16-height, 0, 16, 16, 16);
            default -> Block.box(0, 16-height, 13, 16, 16, 16);
        };
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(FACING, pMirror.mirror(pState.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, BlockState state) {
        int size = state.getValue(SIZE);

        return switch (size) {
            case 0 -> new ItemStack(ModBlocks.SCROLL_SMALL.get());
            case 2 -> new ItemStack(ModBlocks.SCROLL_LARGE.get());
            default -> new ItemStack(ModBlocks.SCROLL_MEDIUM.get());
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public java.util.@NotNull List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.@NotNull Builder builder) {
        int size = state.getValue(SIZE);

        ItemStack drop = switch (size) {
            case 0 -> new ItemStack(ModBlocks.SCROLL_SMALL.get());
            case 2 -> new ItemStack(ModBlocks.SCROLL_LARGE.get());
            default -> new ItemStack(ModBlocks.SCROLL_MEDIUM.get());
        };

        return java.util.List.of(drop);
    }

}
