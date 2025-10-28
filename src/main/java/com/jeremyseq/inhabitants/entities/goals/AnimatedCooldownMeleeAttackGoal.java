package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * Extends CooldownMeleeAttackGoal to include an attack animation phase
 * where the mob cannot move or turn for a given duration.
 * Also allows specifying at what tick during the animation the hit should occur.
 */
public class AnimatedCooldownMeleeAttackGoal extends CooldownMeleeAttackGoal {

    private final String controllerName;
    private final String animationName;
    private final int animationDuration;
    private final int attackHitTick;
    private final GeoEntity geckomob;
    private int animationTicksRemaining = 0;
    private boolean hitDone = false;

    public <T extends PathfinderMob & GeoEntity> AnimatedCooldownMeleeAttackGoal(
            T mob,
            double speedModifier,
            boolean followTargetEvenIfNotSeen,
            int attackIntervalTicks,
            String controllerName,
            String animationName,
            int animationDurationTicks,
            int attackHitTick) {

        super(mob, speedModifier, followTargetEvenIfNotSeen, attackIntervalTicks);
        this.controllerName = controllerName;
        this.animationName = animationName;
        this.animationDuration = animationDurationTicks;
        this.attackHitTick = attackHitTick;
        this.geckomob = mob;
    }

    @Override
    public void tick() {
        if (animationTicksRemaining > 0) {
            animationTicksRemaining--;

            this.mob.getNavigation().stop();

            if (!hitDone && animationDuration - animationTicksRemaining == attackHitTick) {
                LivingEntity target = this.mob.getTarget();
                if (target != null && target.isAlive()) {
                    double distSq = this.mob.distanceToSqr(target);
                    if (distSq <= this.mob.getMeleeAttackRangeSqr(target)) {
                        this.mob.swing(InteractionHand.MAIN_HAND);
                        this.mob.doHurtTarget(target);
                    }
                }
                hitDone = true;
            }

            if (animationTicksRemaining <= 0) {
                hitDone = false;
            }

            return;
        }

        super.tick();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSq) {
        double attackReach = this.mob.getMeleeAttackRangeSqr(target);
        if (distanceToTargetSq <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();

            this.animationTicksRemaining = this.animationDuration;
            this.hitDone = false;
            this.mob.getNavigation().stop();

            this.geckomob.triggerAnim(controllerName, animationName);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (animationTicksRemaining > 0)
            return true;
        return super.canContinueToUse();
    }

}
