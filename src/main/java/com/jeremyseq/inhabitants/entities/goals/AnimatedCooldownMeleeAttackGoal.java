package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import software.bernie.geckolib.animatable.GeoEntity;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Extends CooldownMeleeAttackGoal to include an attack animation phase
 * where the mob cannot move or turn for a given duration.
 * Allows specifying at what tick during the animation the hit should occur.
 * Allows for freezing the mob's movement during specified ticks of the animation.
 */
public class AnimatedCooldownMeleeAttackGoal extends CooldownMeleeAttackGoal {

    private final String controllerName;
    private final String animationName;
    private final int animationDuration;
    private final int attackHitTick;
    private final GeoEntity geckomob;
    private final Consumer<LivingEntity> onAttackStart;
    private int animationTicks = -1;
    private boolean hitDone = false;

    private int startFreezeTick;
    private int endFreezeTick;

    private boolean isAreaAttack = false;

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
        this.onAttackStart = null;
    }

    public <T extends PathfinderMob & GeoEntity> AnimatedCooldownMeleeAttackGoal(
            T mob,
            double speedModifier,
            boolean followTargetEvenIfNotSeen,
            int attackIntervalTicks,
            boolean setSprinting,
            boolean overshootFix,
            String controllerName,
            String animationName,
            int animationDurationTicks,
            int attackHitTick,
            @Nullable Consumer<LivingEntity> onAttackStart) {

        super(mob, speedModifier, followTargetEvenIfNotSeen, attackIntervalTicks, setSprinting, overshootFix);
        this.controllerName = controllerName;
        this.animationName = animationName;
        this.animationDuration = animationDurationTicks;
        this.attackHitTick = attackHitTick;
        this.geckomob = mob;
        this.onAttackStart = onAttackStart;
    }

    public AnimatedCooldownMeleeAttackGoal setFreezeMovement(int startTick, int endTick) {
        this.startFreezeTick = startTick;
        this.endFreezeTick = endTick;
        return this;
    }

    /**
     * @param isAreaAttack if true, the attack will call doHurtTarget regardless of distance
     */
    public AnimatedCooldownMeleeAttackGoal setAreaAttack(boolean isAreaAttack) {
        this.isAreaAttack = isAreaAttack;
        return this;
    }

    @Override
    public void tick() {
        if (animationTicks < this.animationDuration) {
            animationTicks++;

            boolean freezeMovement = animationTicks >= startFreezeTick && animationTicks <= endFreezeTick;

            if (freezeMovement) {
                this.mob.getNavigation().stop();
            }

            if (!hitDone && animationTicks == attackHitTick) {
                LivingEntity target = this.mob.getTarget();
                if (target != null && target.isAlive()) {
                    double distSq = this.mob.distanceToSqr(target);
                    if (isAreaAttack || distSq <= this.mob.getMeleeAttackRangeSqr(target)) {
                        this.mob.swing(InteractionHand.MAIN_HAND);
                        this.mob.doHurtTarget(target);
                    }
                }
                hitDone = true;
            }

            if (animationTicks >= this.animationDuration) {
                hitDone = false;
            }

            if (freezeMovement) return;
        }

        super.tick();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSq) {
        double attackReach = this.mob.getMeleeAttackRangeSqr(target);
        if (distanceToTargetSq <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();

            this.animationTicks = 0;
            this.hitDone = false;
            this.mob.getNavigation().stop();

            // call the attack start callback if provided
            if (this.onAttackStart != null) {
                this.onAttackStart.accept(target);
            }

            this.geckomob.stopTriggeredAnimation(controllerName, animationName);
            this.geckomob.triggerAnim(controllerName, animationName);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (animationTicks < this.animationDuration)
            return true;
        return super.canContinueToUse();
    }

}
