package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class GazerPodBlock extends BaseEntityBlock {
    protected GazerPodBlock(Properties pProperties) {
        super(pProperties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GazerPodBlockEntity(blockPos, blockState);
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GazerPodBlockEntity pod) {
            if (pod.hasGazer()) {
                ItemStack stack = new ItemStack(ModItems.GAZER_POD.get());
                stack.getOrCreateTag().putBoolean("HasGazer", true);
                return Collections.singletonList(stack);
            } else {
                ItemStack stack = new ItemStack(ModItems.GAZER_POD.get());
                stack.getOrCreateTag().putBoolean("HasGazer", false);
                return Collections.singletonList(stack);
            }
        }
        return Collections.singletonList(new ItemStack(ModItems.GAZER_POD.get()));
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return Blocks.NETHERRACK.defaultBlockState().canHarvestBlock(level, pos, player);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return Blocks.NETHERRACK.defaultBlockState().getDestroyProgress(player, level, pos);
    }


    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Block.box(3, 0, 3, 13, 10, 13);
    }

    @Override
    protected void spawnDestroyParticles(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState) {
        for (int i = 0; i < 20; i++) {
            double x = pPos.getX() + 0.5 + (pLevel.random.nextDouble() - 0.5);
            double y = pPos.getY() + 0.5 + (pLevel.random.nextDouble() - 0.5);
            double z = pPos.getZ() + 0.5 + (pLevel.random.nextDouble() - 0.5);
            pLevel.addParticle(
                    ParticleTypes.CRIMSON_SPORE,
                    x, y, z,
                    0, 0.05, 0
            );
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof GazerPodBlockEntity pod) pod.tick();
        };
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.CRIMSON_NYLIUM.getSoundType(state, level, pos, entity);
    }
}
