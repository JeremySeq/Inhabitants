package com.jeremyseq.inhabitants.entities.impaler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class BreakTorchGoal extends Goal {
    private final PathfinderMob mob;
    private final double chance; // 0.0 to 1.0
    private BlockPos torchPos;

    public BreakTorchGoal(PathfinderMob mob, double chance) {
        this.mob = mob;
        this.chance = chance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextDouble() > chance) return false;

        BlockPos mobPos = mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-6, -3, -6), mobPos.offset(6, 3, 6))) {
            BlockState state = mob.level().getBlockState(pos);
            if (isLightBlock(state.getBlock())) {
                torchPos = pos.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return torchPos != null &&
                mob.level().getBlockState(torchPos).getBlock() != Blocks.AIR &&
                mob.distanceToSqr(Vec3.atCenterOf(torchPos)) <= 64.0;
    }

    @Override
    public void start() {
        if (torchPos != null) {
            mob.getNavigation().moveTo(torchPos.getX(), torchPos.getY(), torchPos.getZ(), 1.0D);
        }
    }

    @Override
    public void tick() {
        if (torchPos == null) return;

        if (mob.distanceToSqr(Vec3.atCenterOf(torchPos)) < 3) {
            BlockState state = mob.level().getBlockState(torchPos);
            if (isLightBlock(state.getBlock())) {
                mob.level().destroyBlock(torchPos, false);
            }
            torchPos = null;
        } else {
            mob.getNavigation().moveTo(torchPos.getX(), torchPos.getY(), torchPos.getZ(), 1.0D);
        }
    }

    @Override
    public void stop() {
        torchPos = null;
    }

    private boolean isLightBlock(Block block) {
        return block == Blocks.TORCH || block == Blocks.WALL_TORCH ||
                block == Blocks.LANTERN || block == Blocks.GLOWSTONE;
    }
}
