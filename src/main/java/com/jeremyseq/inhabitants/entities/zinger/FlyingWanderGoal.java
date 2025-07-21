package com.jeremyseq.inhabitants.entities.zinger;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;

import java.util.EnumSet;

public class FlyingWanderGoal extends Goal {
    private final Mob mob;
    private final double speed;

    public FlyingWanderGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.mob.getNavigation().isDone() && this.mob.getRandom().nextInt(10) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        double x = this.mob.getX() + (this.mob.getRandom().nextDouble() * 16 - 8);
        double y = this.mob.getY() + (this.mob.getRandom().nextDouble() * 8 - 4);
        double z = this.mob.getZ() + (this.mob.getRandom().nextDouble() * 16 - 8);

        this.mob.getNavigation().moveTo(x, y, z, this.speed);
    }
}
