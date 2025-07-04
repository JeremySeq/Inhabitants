package com.jeremyseq.inhabitants.entities.impaler;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SprintAtTargetGoal extends Goal {

    private final PathfinderMob mob;
    private final double sprintSpeedMod;
    private final double stopDistanceSqr;
    private final double startDistanceSqr;
    private LivingEntity target;

    public SprintAtTargetGoal(PathfinderMob mob, double sprintSpeedMod, double startDistance, double stopDistance) {
        this.mob = mob;
        this.sprintSpeedMod = sprintSpeedMod;
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.startDistanceSqr = startDistance * startDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity potential = mob.getTarget();
        if (!(potential instanceof Player player) || !player.isAlive()) return false;
        return mob.distanceToSqr(player) > startDistanceSqr;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null
                && target.isAlive()
                && mob.distanceToSqr(target) > stopDistanceSqr;
    }

    @Override
    public void start() {
        target = mob.getTarget();
        mob.setSprinting(true);
    }

    @Override
    public void stop() {
        mob.setSprinting(false);
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        mob.getNavigation().moveTo(target, sprintSpeedMod);
    }
}
