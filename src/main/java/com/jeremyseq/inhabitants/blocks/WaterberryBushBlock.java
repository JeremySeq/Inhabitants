package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WaterberryBushBlock extends Block {
    public static final BooleanProperty FAKE = BooleanProperty.create("fake");

    public WaterberryBushBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FAKE, false));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Block.box(4, 0, 4, 12, 16, 12);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return true;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        ItemStack itemInHand = pPlayer.getItemInHand(pHand);
        if (itemInHand.getItem() == Items.BONE_MEAL) {
            if (!pLevel.isClientSide) {
                // Drop the bush block itself
                popResource(pLevel, pPos, new ItemStack(this));
                // Consume one bonemeal
                if (!pPlayer.isCreative()) {
                    itemInHand.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState pState, LootParams.@NotNull Builder pParams) {
        return List.of(new ItemStack(ModItems.WATERBERRY_ITEM.get(), 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FAKE);
    }
}
