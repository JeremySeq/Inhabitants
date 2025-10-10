package com.jeremyseq.inhabitants.entities.catcher;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class CatcherBurrowGoal extends Goal {
    private final CatcherEntity catcher;

    public CatcherBurrowGoal(CatcherEntity catcher) {
        this.catcher = catcher;
    }

    @Override
    public boolean canUse() {
        // handle relaunch session case
        if (catcher.getState() == CatcherEntity.State.BURROWED)
            return true;

        // only burrow if no target nearby and on sand and idle
        if (catcher.getTarget() == null && catcher.onGround() && catcher.getState() == CatcherEntity.State.IDLE) {
            if (catcher.level().getBlockState(catcher.blockPosition().below()).is(BlockTags.SAND)) {
                return true;
            }
            else {
                // navigate to nearest sand block
                BlockPos nearestSand = null;
                int searchRadius = 5;
                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        BlockPos basePos = catcher.blockPosition().offset(x, 0, z);
                        BlockPos topPos = catcher.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, basePos);
                        if (catcher.level().getBlockState(topPos.below()).is(BlockTags.SAND)) {
                            if (nearestSand == null || catcher.blockPosition().distSqr(topPos) < catcher.blockPosition().distSqr(nearestSand)) {
                                nearestSand = topPos;
                            }
                        }
                    }
                }

                if (nearestSand != null) {
                    catcher.getNavigation().moveTo(nearestSand.getX() + 0.5, nearestSand.getY(), nearestSand.getZ() + 0.5, 1.0);
                }
            }
        }

        return false;
    }

    @Override
    public void start() {
        catcher.setState(CatcherEntity.State.BURROWED);
        catcher.triggerAnim("ground_change", "digging");
        catcher.snapToBlockCenter();
        catcher.setNoGravity(true);
        catcher.getNavigation().stop();
    }

    @Override
    public void stop() {
        catcher.setNoGravity(false);
    }

    @Override
    public boolean canContinueToUse() {
        Player nearest = catcher.level().getNearestPlayer(catcher, 5.0D);
        return nearest == null || !catcher.hasLineOfSight(nearest);
        // ambush goal takes over
    }

    @Override
    public void tick() {
        catcher.setDeltaMovement(Vec3.ZERO);
    }
}
