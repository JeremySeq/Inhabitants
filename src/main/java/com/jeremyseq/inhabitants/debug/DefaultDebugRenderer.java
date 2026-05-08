package com.jeremyseq.inhabitants.debug;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class DefaultDebugRenderer {

    /**
     * Called by tick() server-side to visualize the path.
     */
    public static void renderPath(ServerLevel level, Path path, Vec3 preciseTarget, Vec3 entityPos) {
        if (path != null && !path.isDone()) {

            for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                double x = node.x + 0.5;
                double y = node.y + 0.5;
                double z = node.z + 0.5;

                if (entityPos != null && entityPos.closerThan(new Vec3(x, entityPos.y, z), 1.0)) {
                    continue;
                }

                level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0, 0, 0);
            }
        }

        if (preciseTarget != null) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, preciseTarget.x, preciseTarget.y + 0.2, preciseTarget.z, 5, 0.1, 0.1, 0.1, 0.02);
        }
    }
}
