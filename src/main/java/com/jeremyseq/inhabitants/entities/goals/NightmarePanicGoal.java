package com.jeremyseq.inhabitants.entities.goals;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;

public class NightmarePanicGoal extends PanicGoal {
    public NightmarePanicGoal(PathfinderMob pMob, double pSpeedModifier) {
        super(pMob, pSpeedModifier);
    }

    @Override
    public boolean shouldPanic() {
        return this.mob.hasEffect(ModEffects.PANIC.get());
    }

    @Override
    public boolean canUse() {
        if (!this.shouldPanic()) {
            return false;
        } else {
            return this.findRandomPosition();
        }
    }
}
