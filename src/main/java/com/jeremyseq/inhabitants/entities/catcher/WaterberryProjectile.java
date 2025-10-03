package com.jeremyseq.inhabitants.entities.catcher;

import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class WaterberryProjectile extends ThrowableItemProjectile {

    public WaterberryProjectile(EntityType<? extends WaterberryProjectile> type, Level level) {
        super(type, level);
    }

    public WaterberryProjectile(Level level, LivingEntity thrower) {
        super(ModEntities.WATERBERRY_PROJECTILE.get(), thrower, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.WATERBERRY_ITEM.get();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        if (!this.level().isClientSide) {
            BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
            if (this.level().getBlockState(pos).isAir()) {
                this.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
            this.discard(); // remove projectile
        }
    }
}
