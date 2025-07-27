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
        return this.zinger.hasExactlyOnePlayerPassenger();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.zinger.isAtNest();
    }

    @Override
    public void stop() {
        this.zinger.ejectPassengers();
        this.zinger.setTargetPosition(null);
        super.stop();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void tick() {
        this.zinger.setTargetPosition(zinger.nestPosition);
    }
}
