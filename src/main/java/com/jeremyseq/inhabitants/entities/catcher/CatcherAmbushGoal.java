package com.jeremyseq.inhabitants.entities.catcher;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class CatcherAmbushGoal extends Goal {
    private final CatcherEntity catcher;
    private int emergeTicks;

    public CatcherAmbushGoal(CatcherEntity catcher) {
        this.catcher = catcher;
    }

    @Override
    public boolean canUse() {
        if (catcher.getState() == CatcherEntity.State.AMBUSH) return true; // handle relaunch session case
        if (catcher.getState() != CatcherEntity.State.BURROWED) return false;
        Player player = catcher.level().getNearestPlayer(catcher, 5.0D);
        return player != null && catcher.hasLineOfSight(player);
    }

    @Override
    public void start() {
        catcher.setState(CatcherEntity.State.AMBUSH);
        catcher.triggerAnim("ground_change", "emerging");
        catcher.setNoGravity(false);
        emergeTicks = 20; // 1 second emerge anim delay
    }

    @Override
    public void tick() {
        emergeTicks--;
        if (emergeTicks > 0) {
            // stay in place while emerging
            catcher.setDeltaMovement(0, 0, 0);
        } else {
            // finished emerging, now attack
            catcher.setTarget(catcher.level().getNearestPlayer(catcher, 10.0D)); // maybe unecessary
            catcher.setState(CatcherEntity.State.IDLE);
            // let melee goal take over
        }
    }

    @Override
    public boolean canContinueToUse() {
        // stops after emerge animation is done
        return emergeTicks > 0;
    }
}
