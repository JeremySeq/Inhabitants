package com.jeremyseq.inhabitants.entities.impaler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class BreakTorchGoal extends Goal {
    private static final int  RANGE_XZ = 5;
    private static final int  RANGE_Y = 2;
    private static final int  GIVE_UP_AFTER_TICKS = 40;
    private static final long FAILED_TARGET_TIMEOUT_MS = 10000;

    private final PathfinderMob mob;
    private final double chance;

    private BlockPos torchPos;
    private int ticksTrying;

    private final Map<BlockPos, Long> recentlyFailed = new HashMap<>();

    public BreakTorchGoal(PathfinderMob mob, double chance) {
        this.mob    = mob;
        this.chance = chance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        pruneFailedTargets();

        if (mob.getRandom().nextDouble() > chance) return false;

        BlockPos mobPos = mob.blockPosition();
        torchPos = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                mobPos.offset(-RANGE_XZ, -RANGE_Y, -RANGE_XZ),
                mobPos.offset( RANGE_XZ,  RANGE_Y,  RANGE_XZ))) {

            if (recentlyFailed.containsKey(pos)) continue;

            Block block = mob.level().getBlockState(pos).getBlock();
            if (!isLightBlock(block)) continue;

            double distSq = mob.distanceToSqr(Vec3.atCenterOf(pos));
            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                torchPos = pos.immutable();
            }
        }

        if (torchPos != null) ticksTrying = 0;
        return torchPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (torchPos == null) return false;

        Block block = mob.level().getBlockState(torchPos).getBlock();
        boolean stillLit = isLightBlock(block);
        boolean inRange  = mob.distanceToSqr(Vec3.atCenterOf(torchPos)) <= 64.0;
        boolean stillTrying = ticksTrying < GIVE_UP_AFTER_TICKS;

        return stillLit && inRange && stillTrying;
    }

    @Override
    public void start() {
        moveTowardTorch();
    }

    @Override
    public void tick() {
        if (torchPos == null) return;
        ticksTrying++;
        if (mob.getBoundingBox().getCenter().distanceToSqr(Vec3.atCenterOf(torchPos)) < 4.0) {
            tryBreakTorch();
            return;
        }

        if (!moveTowardTorch()) {
            giveUpOnCurrentTarget();
        }
    }

    @Override
    public void stop() {
        torchPos   = null;
        ticksTrying = 0;
    }

    private boolean moveTowardTorch() {
        return mob.getNavigation().moveTo(torchPos.getX() + 0.5, torchPos.getY(), torchPos.getZ() + 0.5, 1.0D);
    }

    private void tryBreakTorch() {
        if (mob.level() instanceof ServerLevel server &&
                server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            server.destroyBlock(torchPos,false);
        }
        torchPos = null;
        ticksTrying = 0;
    }

    private void giveUpOnCurrentTarget() {
        if (torchPos != null) {
            recentlyFailed.put(torchPos, System.currentTimeMillis());
        }
        torchPos   = null;
        ticksTrying = 0;
    }

    private void pruneFailedTargets() {
        long now = System.currentTimeMillis();
        recentlyFailed.entrySet().removeIf(e -> now - e.getValue() > FAILED_TARGET_TIMEOUT_MS);
    }

    private boolean isLightBlock(Block block) {
        return block == Blocks.TORCH      ||
                block == Blocks.WALL_TORCH ||
                block == Blocks.LANTERN    ||
                block == Blocks.GLOWSTONE;
    }
}
