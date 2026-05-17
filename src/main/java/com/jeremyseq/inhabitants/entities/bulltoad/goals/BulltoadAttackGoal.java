package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Objects;

public class BulltoadAttackGoal extends Goal {

    private static final double TONGUE_RANGE = 7;
    private static final double TONGUE_RANGE_SQ = TONGUE_RANGE * TONGUE_RANGE;
    private static final int FACE_TICKS = 10;
    private static final int STRIKE_TICKS = 2;

    private final BulltoadEntity bulltoad;
    private LivingEntity target;

    private enum Phase { CHASE, FACE, STRIKE, RETRACT }
    private Phase phase = Phase.CHASE;
    private int phaseTimer = 0;

    public BulltoadAttackGoal(BulltoadEntity bulltoad) {
        this.bulltoad = bulltoad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = bulltoad.getTarget();
        if (t == null || !t.isAlive() || bulltoad.isBaby()) return false;
        target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && bulltoad.getTarget() == target;
    }

    @Override
    public void start() {
        phase = Phase.CHASE;
        phaseTimer = 0;
        bulltoad.clearTongueTarget();
    }

    @Override
    public void stop() {
        this.bulltoad.setSnapYaw(false);
        bulltoad.clearTongueTarget();
        bulltoad.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        double distSq = bulltoad.distanceToSqr(target);

        switch (phase) {
            case CHASE -> {
                bulltoad.getNavigation().moveTo(target, 1.2);
                bulltoad.getLookControl().setLookAt(target);

                if (distSq <= TONGUE_RANGE_SQ) {
                    bulltoad.getNavigation().stop();
                    phase = Phase.FACE;
                    phaseTimer = 0;
                }
            }

            case FACE -> {
                bulltoad.getNavigation().stop();
                bulltoad.snapToFaceTargetEntity(target);
                this.bulltoad.setSnapYaw(true);
                phaseTimer++;

                if (distSq > TONGUE_RANGE_SQ) {
                    phase = Phase.CHASE;
                    return;
                }

                if (phaseTimer >= FACE_TICKS) {
                    bulltoad.setTongueTarget(target.getEyePosition());
                    phase = Phase.STRIKE;
                    phaseTimer = 0;
                }
            }

            case STRIKE -> {
                bulltoad.snapToFaceTargetEntity(target);
                this.bulltoad.setSnapYaw(true);

                if (phaseTimer == 0) {
                    Vec3 knockbackDir = target.position().subtract(bulltoad.position()).normalize();
                    target.knockback(3, -knockbackDir.x, -knockbackDir.z);
                    target.hurt(bulltoad.damageSources().mobAttack(bulltoad),
                            (float) Objects.requireNonNull(bulltoad.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue());
                }

                phaseTimer++;

                if (phaseTimer >= STRIKE_TICKS) {
                    bulltoad.clearTongueTarget();
                    phase = Phase.RETRACT;
                    phaseTimer = 0;
                }
            }

            case RETRACT -> {
                bulltoad.snapToFaceTargetEntity(target);
                phaseTimer++;
                if (phaseTimer >= 5) {
                    phase = Phase.CHASE;
                    phaseTimer = 0;
                }
            }
        }
    }
}