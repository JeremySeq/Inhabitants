package com.jeremyseq.inhabitants.entities.impaler;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ImpalerRageGoal extends Goal {
    private final ImpalerEntity mob;
    private int rageTimer = 0;

    public ImpalerRageGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !mob.isSpiked() && mob.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return rageTimer <= 14;
    }

    @Override
    public void start() {
        mob.getEntityData().set(ImpalerEntity.SPIKED, true);
        rageTimer = 0;
        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }
        mob.triggerAnim("rage", "rage");
    }

    @Override
    public void tick() {
        rageTimer++;
    }
}
