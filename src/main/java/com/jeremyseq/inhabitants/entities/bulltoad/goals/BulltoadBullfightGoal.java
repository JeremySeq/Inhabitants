package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class BulltoadBullfightGoal extends Goal {

    private static final double FIGHT_RANGE = 20;
    private static final double LEAP_RANGE = 6; // bulltoad will target within 2 blocks of this range
    private static final double HIT_RANGE = 2.5;
    private static final int COOLDOWN_TICKS = 20 * 60;
    private static final int FACE_TICKS = 30;

    private final BulltoadEntity bulltoad;
    private int faceTimer = 0;
    private boolean readyToLeap = false;
    private boolean hasLeaped = false;
    private boolean hasDealtDamage = false;
    private int leapWaitTimer = 0;
    private static final int MAX_LEAP_WAIT = 20 * 8;

    public BulltoadBullfightGoal(BulltoadEntity bulltoad) {
        this.bulltoad = bulltoad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean canFight(BulltoadEntity bulltoad) {
        return !bulltoad.isBaby()
                && bulltoad.getTarget() == null
                && bulltoad.hasHorns()
                && bulltoad.bullfightCooldown == 0
                && bulltoad.fightRival == null;
    }

    @Override
    public boolean canUse() {
        if (bulltoad.fightRival != null && bulltoad.fightRival.isAlive()) {
            return true;
        }

        if (!canFight(bulltoad)) return false;

        // rarity
        if (bulltoad.getRandom().nextInt(1200) != 0) return false;

        List<BulltoadEntity> nearby = bulltoad.level().getEntitiesOfClass(
                BulltoadEntity.class,
                bulltoad.getBoundingBox().inflate(FIGHT_RANGE),
                other -> other != bulltoad && canFight(other)
        );

        if (nearby.isEmpty()) return false;

        // find the nearest bulltoad that doesn't already have a rival
        BulltoadEntity nearest = nearby.stream()
                .filter(other -> other.fightRival == null)
                .min(java.util.Comparator.comparingDouble(bulltoad::distanceToSqr))
                .orElse(null);

        if (nearest != null) {
            bulltoad.fightRival = nearest;
            nearest.fightRival = bulltoad;
            return true;
        }

        return false;
    }


    @Override
    public boolean canContinueToUse() {
        return bulltoad.fightRival != null
                && bulltoad.fightRival.isAlive()
                && bulltoad.distanceToSqr(bulltoad.fightRival) < FIGHT_RANGE * FIGHT_RANGE
                && bulltoad.getTarget() == null
                && bulltoad.fightRival.fightRival == bulltoad;
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
        if (bulltoad.fightRival != null && bulltoad.fightRival.fightRival == bulltoad) {
            bulltoad.fightRival.fightRival = null;
        }
        bulltoad.fightRival = null;
        bulltoad.fightReadyToLeap = false;
        bulltoad.setBullfightJumping(false);
        bulltoad.bullfightCooldown = COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        BulltoadEntity rival = bulltoad.fightRival;
        if (rival == null) { stop(); return; }

        double distSq = bulltoad.distanceToSqr(rival);

        if (!hasLeaped) {
            if (!readyToLeap) {
                double dist = Math.sqrt(distSq);

                if (dist > LEAP_RANGE+1) {
                    bulltoad.getNavigation().moveTo(rival, 1.4);
                    bulltoad.getLookControl().setLookAt(rival);
                    faceTimer = 0;
                } else if (dist < LEAP_RANGE-1) {
                    Vec3 awayDir = bulltoad.position().subtract(rival.position()).normalize();
                    Vec3 backTarget = bulltoad.position().add(awayDir.scale(2.0));
                    bulltoad.getNavigation().moveTo(backTarget.x, backTarget.y, backTarget.z, 1.4);
                    bulltoad.getLookControl().setLookAt(rival);
                    faceTimer = 0;
                } else {
                    bulltoad.getNavigation().stop();
                    bulltoad.snapToFaceTargetEntity(rival);
                    faceTimer++;

                    if (faceTimer >= FACE_TICKS) {
                        readyToLeap = true;
                        bulltoad.fightReadyToLeap = true;
                    }
                }
            } else {
                bulltoad.snapToFaceTargetEntity(rival);
                boolean rivalReady = rival.fightRival == bulltoad && rival.isFightReadyToLeap();

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
            if (!hasDealtDamage && distSq < HIT_RANGE * HIT_RANGE) {
                bulltoad.setBullfightJumping(true);
                rival.hurt(bulltoad.damageSources().mobAttack(bulltoad), 4.0f);
                Vec3 knockDir = rival.position().subtract(bulltoad.position()).normalize();
                rival.setDeltaMovement(knockDir.x * 1.2, 0.4, knockDir.z * 1.2);
                hasDealtDamage = true;
                bulltoad.setLastHurtByMob(null);
                bulltoad.setTarget(null);
                rival.setLastHurtByMob(null);
                rival.setTarget(null);

                if (bulltoad.hasHorns()) {
                    rival.dropHorns();
                }
            }

            if (bulltoad.onGround()) {
                bulltoad.setBullfightJumping(false);
                stop();
            }
        }
    }
}