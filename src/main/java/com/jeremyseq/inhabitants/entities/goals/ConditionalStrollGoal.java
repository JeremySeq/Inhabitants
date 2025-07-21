package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import java.util.function.Predicate;

public class ConditionalStrollGoal<T extends PathfinderMob> extends WaterAvoidingRandomStrollGoal {
    private final T mob;
    private final Predicate<T> conditions;

    public ConditionalStrollGoal(T mob, double speed, Predicate<T> conditions) {
        super(mob, speed);
        this.mob = mob;
        this.conditions = conditions;
    }

    @Override
    public boolean canUse() {
        if (!conditions.test(mob)) return false;

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!conditions.test(mob)) return false;

        return super.canContinueToUse();
    }
}
