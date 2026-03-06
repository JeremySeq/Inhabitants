package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Fixes a common pathfinding issue where mobs will rapidly turn back and forth when moving fast downhill and overshooting their path nodes.
 * This is a utility method to be called in the tick method of any goal that involves pathfinding.
 */
public class PathfindingOvershootFix {
    public static void overshootFix(PathfinderMob mob) {
        Path currentPath = mob.getNavigation().getPath();
        if (currentPath != null && !currentPath.isDone()) {
            int nextNodeIndex = currentPath.getNextNodeIndex();
            if (nextNodeIndex < currentPath.getNodeCount()) {
                Vec3 nextNodePos = currentPath.getNode(nextNodeIndex).asVec3();
                double distToNext = mob.position().distanceToSqr(nextNodePos);
                Vec3 mobPos = mob.position();
                Vec3 mobVelocity = mob.getDeltaMovement();
                Vec3 toNode = nextNodePos.subtract(mobPos).normalize();

                double dot = mobVelocity.normalize().dot(toNode);
                // dot > 0 = moving toward the node
                // dot < 0 = moving away from the node (overshot)

                if (dot < 0 && distToNext > 2) {
                    currentPath.advance(); // we passed it, skip ahead
                }
            }
        }
    }
}
