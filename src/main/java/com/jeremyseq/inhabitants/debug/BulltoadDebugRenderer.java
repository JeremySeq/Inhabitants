package com.jeremyseq.inhabitants.debug;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class BulltoadDebugRenderer {

    /**
     * Shows jump target.
     */
    public static void renderJumpTarget(BulltoadEntity bulltoad, Vec3 pos) {
        if (bulltoad.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z,
                    8, 0.3, 0.3,0.3,0.0);
        }
    }
}
