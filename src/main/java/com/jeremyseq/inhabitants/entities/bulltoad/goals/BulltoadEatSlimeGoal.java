package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class BulltoadEatSlimeGoal extends Goal {
    private final BulltoadEntity bulltoad;
    private Slime target;

    private enum Phase { CHASE, FACE, STRIKE, REEL, RETRACT }
    private Phase phase = Phase.CHASE;
    private int phaseTimer = 0;

    public BulltoadEatSlimeGoal(BulltoadEntity bulltoad) {
        this.bulltoad = bulltoad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bulltoad.isBaby() || bulltoad.getTarget() != null) return false;

        List<Slime> nearby = bulltoad.level().getEntitiesOfClass(Slime.class,
                bulltoad.getBoundingBox().inflate(BulltoadEntity.TONGUE_RANGE * 2),
                slime -> slime.isAlive() && slime.getSize() <= 2);

        if (nearby.isEmpty()) return false;

        target = nearby.stream().min(java.util.Comparator.comparingDouble(bulltoad::distanceToSqr)).orElse(null);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return (target != null && target.isAlive()) || phase == Phase.REEL || phase == Phase.RETRACT;
    }

    @Override
    public void start() {
        phase = Phase.CHASE;
        phaseTimer = 0;
        bulltoad.clearTongueTarget();
    }

    @Override
    public void stop() {
        bulltoad.setSnapYaw(false);
        bulltoad.clearTongueTarget();
        bulltoad.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        switch (phase) {
            case CHASE -> {
                if (target == null || !target.isAlive()) { stop(); return; }
                double distSq = bulltoad.distanceToSqr(target);
                bulltoad.getNavigation().moveTo(target, 1.2);
                bulltoad.getLookControl().setLookAt(target);

                if (distSq <= BulltoadEntity.TONGUE_RANGE * BulltoadEntity.TONGUE_RANGE) {
                    bulltoad.getNavigation().stop();
                    phase = Phase.FACE;
                    phaseTimer = 0;
                }
            }

            case FACE -> {
                if (target == null || !target.isAlive()) { stop(); return; }
                double distSq = bulltoad.distanceToSqr(target);
                bulltoad.getNavigation().stop();
                bulltoad.snapToFaceTargetEntity(target);
                bulltoad.setSnapYaw(true);
                phaseTimer++;

                if (distSq > BulltoadEntity.TONGUE_RANGE * BulltoadEntity.TONGUE_RANGE) {
                    phase = Phase.CHASE;
                    return;
                }

                if (phaseTimer >= BulltoadEntity.FACE_TICKS) {
                    bulltoad.setTongueTarget(target.getEyePosition());
                    phase = Phase.STRIKE;
                    phaseTimer = 0;
                }
            }

            case STRIKE -> {
                bulltoad.snapToFaceTargetEntity(target);
                bulltoad.setSnapYaw(true);
                phaseTimer++;

                bulltoad.setTongueTarget(target.getEyePosition());

                if (phaseTimer >= BulltoadEntity.STRIKE_TICKS) {
                    target.setNoGravity(true);
                    // move target towards bulltoad
                    Vec3 toMouth = target.position().vectorTo(bulltoad.getEyePosition()).normalize().scale(1);
                    target.setDeltaMovement(toMouth);
                    phase = Phase.REEL;
                    phaseTimer = 0;
                }
            }

            case REEL -> {
                if (target == null || !target.isAlive()) { stop(); return; }
                bulltoad.setTongueTarget(target.getEyePosition());

                // reapply force every 3 ticks to keep pulling
                if (phaseTimer % 3 == 0) {
                    Vec3 toMouth = target.position().vectorTo(bulltoad.getEyePosition()).normalize().scale(0.75);
                    target.setDeltaMovement(toMouth);
                }
                phaseTimer++;

                if (target.distanceToSqr(bulltoad) < 1.5 * 1.5) {
                    bulltoad.spawnAtLocation(new ItemStack(Items.SLIME_BALL, 1 + bulltoad.getRandom().nextInt(2)));
                    target.discard();
                    target = null;
                    bulltoad.clearTongueTarget();
                    phase = Phase.RETRACT;
                    phaseTimer = 0;
                }
            }

            case RETRACT -> {
                phaseTimer++;
                if (phaseTimer >= 10) {
                    bulltoad.setSnapYaw(false);
                    bulltoad.clearTongueTarget();
                    bulltoad.getNavigation().stop();
                    target = null;
                    phase = Phase.CHASE;
                    phaseTimer = 0;
                }
            }
        }
    }
}