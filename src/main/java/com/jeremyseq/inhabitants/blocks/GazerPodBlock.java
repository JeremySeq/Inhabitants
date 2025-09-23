package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
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
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && tool.getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0) {
            if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GazerPodBlockEntity pod) {
                if (pod.hasGazer()) {
                    ItemStack stack = new ItemStack(ModItems.GAZER_POD.get());
                    stack.getOrCreateTag().putBoolean("HasGazer", true);
                    return Collections.singletonList(stack);
                } else {
                    return Collections.singletonList(new ItemStack(ModItems.GAZER_POD.get()));
                }
            }
            return Collections.singletonList(new ItemStack(ModItems.GAZER_POD.get()));
        }
        return Collections.emptyList();

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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof GazerPodBlockEntity pod) pod.tick();
        };
    }
}
