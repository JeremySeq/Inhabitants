package com.jeremyseq.inhabitants.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InvisibleCauldronBlock extends Block {
    private static final VoxelShape topPart = Block.box(-10, 17, -10, 26, 20, 26);
    private static final VoxelShape bottomPart = Block.box(-8, 0, -8, 24, 17, 24);

    private static final VoxelShape colShape = Shapes.or(topPart, bottomPart);

    public InvisibleCauldronBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter world,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return bottomPart;
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter world,
        @NotNull BlockPos pos
    ) {
        return colShape;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter world,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return colShape;
    }

    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            List<BogreCauldronEntity> cauldrons = level.getEntitiesOfClass(
                BogreCauldronEntity.class, 
                new AABB(pos).inflate(3.0D)
            );
            
            for (BogreCauldronEntity cauldron : cauldrons) {
                if (cauldron.blockPosition().closerThan(pos, 3.0D)) {
                    return cauldron.interact(player, hand);
                }
            }
        }
        
        return InteractionResult.SUCCESS;
    }
}