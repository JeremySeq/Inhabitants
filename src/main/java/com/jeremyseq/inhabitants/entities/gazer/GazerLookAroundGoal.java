package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

public class GazerLookAroundGoal extends RandomLookAroundGoal {
    private final GazerEntity gazer;

    public GazerLookAroundGoal(GazerEntity gazer) {
        super(gazer);
        this.gazer = gazer;
    }

    @Override
    public boolean canUse() {
        return gazer.getGazerState() == GazerEntity.GazerState.IDLE;
    }
}
