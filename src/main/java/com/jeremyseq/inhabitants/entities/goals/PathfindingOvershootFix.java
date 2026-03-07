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
            if (nextNodeIndex > 0 && nextNodeIndex < currentPath.getNodeCount()) {
                Vec3 mobPos = mob.position();
                Vec3 currentNodePos = currentPath.getNode(nextNodeIndex - 1).asVec3();

                Vec3 toCurrentNode2D = new Vec3(currentNodePos.x - mobPos.x, 0, currentNodePos.z - mobPos.z);
                Vec3 velocity2D = new Vec3(mob.getDeltaMovement().x, 0, mob.getDeltaMovement().z);

                double dot = velocity2D.normalize().dot(toCurrentNode2D.normalize());
                double distFromCurrent = toCurrentNode2D.length();

                if (dot < 0 && distFromCurrent > .3) {
                    currentPath.advance();
                }
            }
        }
    }
}
