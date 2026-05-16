package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class BulltoadBullfightGoal extends Goal {

    private static final double FIGHT_RANGE = 20;
    private static final double LEAP_RANGE = 6;
    private static final double HIT_RANGE = 2.5;
    private static final int COOLDOWN_TICKS = 20*60;
    private static final int FACE_TICKS = 30; // both wait this long before jumping

    private final BulltoadEntity bulltoad;
    private BulltoadEntity rival;
    private int cooldown = 0;
    private int faceTimer = 0;
    private boolean readyToLeap = false;
    private boolean hasLeaped = false;
    private boolean hasDealtDamage = false;
    private int leapWaitTimer = 0;
    private static final int MAX_LEAP_WAIT = 60; // 3 seconds

    public BulltoadBullfightGoal(BulltoadEntity bulltoad) {
        this.bulltoad = bulltoad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (bulltoad.isBaby() || bulltoad.getTarget() != null || !bulltoad.hasHorns()) return false;

        if (bulltoad.fightRival != null && bulltoad.fightRival.isAlive()) {
            rival = bulltoad.fightRival;
            return true;
        }

        // random chance to bullfight
        if (bulltoad.getRandom().nextInt(80) != 0) return false;

        List<BulltoadEntity> nearby = bulltoad.level().getEntitiesOfClass(
                BulltoadEntity.class,
                bulltoad.getBoundingBox().inflate(FIGHT_RANGE),
                other -> other != bulltoad
                        && !other.isBaby()
                        && other.getTarget() == null
                        && other.fightRival == null
                        && other.hasHorns()
        );

        if (nearby.isEmpty()) return false;

        rival = nearby.stream()
                .min(java.util.Comparator.comparingDouble(bulltoad::distanceToSqr))
                .orElse(null);

        bulltoad.fightRival = rival;
        rival.fightRival = bulltoad;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return rival != null
                && rival.isAlive()
                && bulltoad.distanceToSqr(rival) < FIGHT_RANGE * FIGHT_RANGE
                && bulltoad.getTarget() == null;
    }

    @Override
    public void start() {
        hasLeaped = false;
        hasDealtDamage = false;
        readyToLeap = false;
        faceTimer = 0;
        leapWaitTimer = 0;
    }

    @Override
    public void stop() {
        bulltoad.getNavigation().stop();
        cooldown = COOLDOWN_TICKS;
        if (rival != null && rival.fightRival == bulltoad) {
            rival.fightRival = null;
        }
        bulltoad.fightRival = null;
        rival = null;
        bulltoad.fightReadyToLeap = false;
    }

    @Override
    public void tick() {
        double distSq = bulltoad.distanceToSqr(rival);

        if (!hasLeaped) {
            if (!readyToLeap) {
                // phase 1: walk toward rival until in leap range
                bulltoad.getNavigation().moveTo(rival, 1.4);
                bulltoad.getLookControl().setLookAt(rival);

                if (distSq < LEAP_RANGE * LEAP_RANGE) {
                    // in range, stop moving and start face countdown
                    bulltoad.getNavigation().stop();
                    bulltoad.snapToFaceTargetEntity(rival);
                    faceTimer++;

                    // mark self ready, wait for rival to be ready
                    if (faceTimer >= FACE_TICKS) {
                        readyToLeap = true;
                        bulltoad.fightReadyToLeap = true;
                    }
                }
            } else {
                // phase 2: ready, leap only when rival is also ready
                bulltoad.snapToFaceTargetEntity(rival);
                boolean rivalReady = rival.fightRival == bulltoad
                        && rival.isFightReadyToLeap();

                if (rivalReady && bulltoad.onGround()) {
                    Vec3 leapDir = rival.position().subtract(bulltoad.position()).normalize();
                    bulltoad.setDeltaMovement(leapDir.x * 0.8, 0.6, leapDir.z * 0.8);
                    hasLeaped = true;
                } else {
                    leapWaitTimer++;
                    if (leapWaitTimer >= MAX_LEAP_WAIT) {
                        stop();
                    }
                }
            }
        } else {
            // phase 3: airborne, deal damage on contact
            if (!hasDealtDamage && distSq < HIT_RANGE * HIT_RANGE) {
                rival.hurt(bulltoad.damageSources().mobAttack(bulltoad), 4.0f);
                Vec3 knockDir = rival.position().subtract(bulltoad.position()).normalize();
                rival.setDeltaMovement(knockDir.x * 1.2, 0.4, knockDir.z * 1.2);
                hasDealtDamage = true;
                bulltoad.setLastHurtByMob(null);
                bulltoad.setTarget(null);
                rival.setLastHurtByMob(null);
                rival.setTarget(null);

                // if bulltoad has horns, remove rival's
                if (bulltoad.hasHorns()) {
                    rival.dropHorns();
                }
            }

            if (bulltoad.onGround()) {
                stop();
            }
        }
    }
}