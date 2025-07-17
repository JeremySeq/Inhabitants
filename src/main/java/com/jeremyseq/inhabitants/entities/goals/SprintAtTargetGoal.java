package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SprintAtTargetGoal extends Goal {

    private final PathfinderMob mob;
    private final double sprintSpeedMod;
    private final double stopDistanceSqr;
    private final double startDistanceSqr;
    private LivingEntity target;
    private int tickCounter = 0;

    public SprintAtTargetGoal(PathfinderMob mob, double sprintSpeedMod, double startDistance, double stopDistance) {
        this.mob = mob;
        this.sprintSpeedMod = sprintSpeedMod;
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.startDistanceSqr = startDistance * startDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity potential = mob.getTarget();
        if (potential == null || !potential.isAlive()) return false;
        return mob.distanceToSqr(potential) > startDistanceSqr;
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

        if (tickCounter++ % 10 == 0 || mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(target, sprintSpeedMod);
        }

        // handle overshooting a path node
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            int nextNodeIndex = path.getNextNodeIndex();
            if (nextNodeIndex < path.getNodeCount()) {
                Vec3 nextNodePos = path.getNode(nextNodeIndex).asVec3();
                double distToNext = mob.position().distanceToSqr(nextNodePos);

                // if we're too far or already past it, skip to the next node
                if (distToNext > 6.0) {
                    path.advance(); // skip the node to prevent backtracking
                }
            }
        }
    }
}
