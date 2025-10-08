package com.jeremyseq.inhabitants.entities.catcher;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
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
        return catcher.getTarget() == null && catcher.onGround() && catcher.getState() == CatcherEntity.State.IDLE
                && catcher.level().getBlockState(catcher.blockPosition().below()).is(BlockTags.SAND);
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
