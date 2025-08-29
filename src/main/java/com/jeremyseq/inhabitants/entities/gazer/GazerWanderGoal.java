package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.entities.zinger.FlyingWanderGoal;

public class GazerWanderGoal extends FlyingWanderGoal {
    private final GazerEntity gazer;

    public GazerWanderGoal(GazerEntity gazer, double speed) {
        super(gazer, speed);
        this.gazer = gazer;
    }

    @Override
    public boolean canUse() {
        return (gazer.currentState == GazerEntity.GazerState.IDLE)
                && super.canUse();
    }
}
