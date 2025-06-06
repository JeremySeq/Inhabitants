package com.jeremyseq.inhabitants.entities.bogre;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class ConditionalStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final BogreEntity bogre;

    public ConditionalStrollGoal(BogreEntity bogre, double speed) {
        super(bogre, speed);
        this.bogre = bogre;
    }

    @Override
    public boolean canUse() {
        return bogre.getTarget() == null && bogre.state != BogreEntity.State.MAKE_CHOWDER && !bogre.isRoaring() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return bogre.getTarget() == null && bogre.state != BogreEntity.State.MAKE_CHOWDER && !bogre.isRoaring() && super.canContinueToUse();
    }
}
