package com.jeremyseq.inhabitants.entities.bogre;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

public class ConditionalStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final BogreEntity bogre;

    public ConditionalStrollGoal(BogreEntity bogre, double speed) {
        super(bogre, speed);
        this.bogre = bogre;
    }

    @Override
    public boolean canUse() {
        if (bogre.getTarget() != null || bogre.state == BogreEntity.State.MAKE_CHOWDER || bogre.isRoaring()) return false;

        if (bogre.cauldronPos != null) {
            double dist = bogre.distanceToSqr(Vec3.atCenterOf(bogre.cauldronPos));
            if (dist > BogreEntity.MAX_CAULDRON_DIST_SQR) return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (bogre.getTarget() != null || bogre.state == BogreEntity.State.MAKE_CHOWDER || bogre.isRoaring()) return false;

        if (bogre.cauldronPos != null) {
            double dist = bogre.distanceToSqr(Vec3.atCenterOf(bogre.cauldronPos));
            if (dist > BogreEntity.MAX_CAULDRON_DIST_SQR) return false;
        }

        return super.canContinueToUse();
    }

    @Override
    protected Vec3 getPosition() {
        // clamp target wander position within max range of cauldron
        if (bogre.cauldronPos != null) {
            for (int i = 0; i < 10; i++) { // try up to 10 times
                Vec3 candidate = super.getPosition();
                if (candidate == null) continue;

                double dist = candidate.distanceToSqr(Vec3.atCenterOf(bogre.cauldronPos));
                if (dist <= BogreEntity.MAX_CAULDRON_DIST_SQR) return candidate;
            }

            // fallback to cauldron center if no valid position found
            return Vec3.atCenterOf(bogre.cauldronPos);
        }

        return super.getPosition();
    }
}
