package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class BogreReturnToCauldronGoal extends Goal {

    private final BogreEntity bogre;
    private final double speed;

    public BogreReturnToCauldronGoal(BogreEntity bogre, double speed) {
        this.bogre = bogre;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (bogre.cauldronPos == null) return false;
        if (bogre.getAIState() != BogreAi.State.NEUTRAL) return false;
        if (bogre.getTarget() != null) return false;
        if (bogre.isRoaring()) return false;

        boolean outOfRange = !this.bogre.canSeeCauldron() ||
            bogre.cauldronPos.distToCenterSqr(bogre.position()) > BogreNeutralGoal.MAX_CAULDRON_DIST_SQR;
        
        if (outOfRange) {
            double distSq = bogre.cauldronPos.distToCenterSqr(bogre.position());
            if (distSq > BogreNeutralGoal.MAX_CAULDRON_DIST_SQR * 2.0 || bogre.getRandom().nextInt(40) == 0) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (bogre.cauldronPos == null) return false;
        if (bogre.getAIState() != BogreAi.State.NEUTRAL) return false;
        if (bogre.getTarget() != null) return false;
        if (bogre.isRoaring()) return false;

        return bogre.cauldronPos.distToCenterSqr(bogre.position())
                > 16;
    }

    @Override
    public void start() {
        moveTowardTarget();
        bogre.setNeutralState(BogreAi.NeutralState.WANDERING);
    }

    @Override
    public void tick() {

        // re-evaluate periodically or if path ends
        if (bogre.getNavigation().isDone() || bogre.tickCount % 20 == 0) {
            moveTowardTarget();
        }

        if (bogre.getNavigation().isDone()) {
            bogre.setNeutralState(BogreAi.NeutralState.IDLE);
        } else {
            bogre.setNeutralState(BogreAi.NeutralState.WANDERING);
        }

        // teleport to entrance if path ended and cauldron not visible
        // or if bogre is very far from entrance
        if (bogre.entrancePos != null && ((bogre.getNavigation().isDone() && !bogre.canSeeCauldron()) || bogre.distanceToSqr(bogre.entrancePos.getCenter()) > 20*20)) {
            Vec3 entrance = Vec3.atCenterOf(bogre.entrancePos);
            bogre.setPos(entrance.x, entrance.y, entrance.z);
            bogre.getNavigation().stop();
            bogre.setNeutralState(BogreAi.NeutralState.IDLE);
        }
    }


    @Override
    public void stop() {
        bogre.getNavigation().stop();
        bogre.setNeutralState(BogreAi.NeutralState.IDLE);
    }

    /**
     * decides whether to go straight to the cauldron or
     * first navigate to the entrance
     */
    private void moveTowardTarget() {
        if (!bogre.canSeeCauldron() && bogre.entrancePos != null) {
            moveToEntrance();
            return;
        }

        moveToCauldron();
    }

    private void moveToCauldron() {
        Vec3 target = Vec3.atCenterOf(bogre.cauldronPos);

        bogre.getNavigation().moveTo(
                target.x,
                target.y,
                target.z,
                speed
        );
    }

    private void moveToEntrance() {
        Vec3 target = Vec3.atCenterOf(bogre.entrancePos);

        bogre.getNavigation().moveTo(
                target.x,
                target.y,
                target.z,
                speed
        );
    }
}
