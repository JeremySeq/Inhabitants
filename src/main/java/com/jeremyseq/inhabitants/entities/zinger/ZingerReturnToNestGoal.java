package com.jeremyseq.inhabitants.entities.zinger;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ZingerReturnToNestGoal extends Goal {
    private final ZingerEntity zinger;

    public ZingerReturnToNestGoal(ZingerEntity zinger) {
        this.zinger = zinger;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.zinger.returningToNest;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.zinger.isAtNest() && this.zinger.returningToNest;
    }

    @Override
    public void stop() {
        this.zinger.returningToNest = false;
        this.zinger.ejectPassengers();
        this.zinger.setTargetPosition(null);
    }

    @Override
    public void start() {
        this.zinger.returningToNest = true;
        this.zinger.setTargetPosition(zinger.nestPosition);
    }
}
