package com.jeremyseq.inhabitants.entities.apex;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ApexSleepGoal extends Goal {
    private final ApexEntity apex;
    private int endTick = 0;
    private boolean trigger = false;

    public ApexSleepGoal(ApexEntity apex) {
        this.apex = apex;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.apex.getEntityData().get(ApexEntity.SLEEPING);
    }

    @Override
    public boolean canContinueToUse() {
        return endTick < 55;
    }

    @Override
    public void tick() {

        if (this.apex.getTarget() != null || this.apex.getLastHurtByMob() != null) {
            this.trigger = true;
            apex.triggerAnim("sleep_states", "wake");
            apex.getEntityData().set(ApexEntity.SLEEPING, false);
        }

        if (trigger) {
            endTick++;
        } else {
            endTick = 0;
        }
    }
}
