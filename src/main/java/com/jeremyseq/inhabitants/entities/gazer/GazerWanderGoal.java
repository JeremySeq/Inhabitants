package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class GazerWanderGoal extends Goal {
    private final GazerEntity gazer;
    private int cooldown = 0;
    private Path path;

    public GazerWanderGoal(GazerEntity gazer) {
        this.gazer = gazer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (gazer.isEnteringPod() || gazer.getGazerState() != GazerEntity.GazerState.IDLE) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        RandomSource rand = gazer.getRandom();
        double x = gazer.getX() + (rand.nextDouble() - 0.5) * 16.0;
        double y = gazer.getY() + (rand.nextDouble() - 0.5) * 8.0;
        double z = gazer.getZ() + (rand.nextDouble() - 0.5) * 16.0;
        path = gazer.getNavigation().createPath(x, y, z, 0);
        if (path != null) {
            gazer.getNavigation().moveTo(path, 1.0);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !gazer.getNavigation().isDone() && gazer.getGazerState() == GazerEntity.GazerState.IDLE && !gazer.isEnteringPod();
    }

    @Override
    public void stop() {
        gazer.getNavigation().stop();
        cooldown = 20 + gazer.getRandom().nextInt(20); // random cooldown before next wander
    }
}
