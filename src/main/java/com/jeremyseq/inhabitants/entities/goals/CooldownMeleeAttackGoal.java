package com.jeremyseq.inhabitants.entities.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;

import java.util.EnumSet;

/**
 * Same as {@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal}, but with a customizable cooldown between attacks.
 */
public class CooldownMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private final int attackInterval; // how many ticks between attacks
    private long lastCanUseCheck;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    private int failedPathFindingPenalty = 0;
    private final boolean canPenalize = false;
    private final boolean setSprinting;

    public CooldownMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followTargetEvenIfNotSeen, int attackIntervalTicks) {
        this(mob, speedModifier, followTargetEvenIfNotSeen, attackIntervalTicks, false);
    }

    public CooldownMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followTargetEvenIfNotSeen, int attackIntervalTicks, boolean setSprinting) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.attackInterval = attackIntervalTicks;
        this.setSprinting = setSprinting;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }


    @Override
    public boolean canUse() {
        long gameTime = this.mob.level().getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }
        this.lastCanUseCheck = gameTime;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        if (canPenalize) {
            if (--this.ticksUntilNextPathRecalculation <= 0) {
                this.path = this.mob.getNavigation().createPath(target, 0);
                this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                return this.path != null;
            } else {
                return true;
            }
        }

        this.path = this.mob.getNavigation().createPath(target, 0);
        return this.path != null || this.getAttackReachSqr(target) >= this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!this.followTargetEvenIfNotSeen) return !this.mob.getNavigation().isDone();
        if (!this.mob.isWithinRestriction(target.blockPosition())) return false;
        return !(target instanceof Player player) || (!player.isSpectator() && !player.isCreative());
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
        if (this.setSprinting) {
            this.mob.setSprinting(true);
        }
    }

    @Override
    public void stop() {
        LivingEntity target = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.mob.setTarget(null);
        }
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        if (this.setSprinting) {
            this.mob.setSprinting(false);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceToTargetSq = this.mob.getPerceivedTargetDistanceSquareForMeleeAttack(target);
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        if ((this.followTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
                && this.ticksUntilNextPathRecalculation <= 0
                && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D
                || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D
                || this.mob.getRandom().nextFloat() < 0.05F)) {

            this.pathedTargetX = target.getX();
            this.pathedTargetY = target.getY();
            this.pathedTargetZ = target.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

            if (this.canPenalize) {
                this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                Path currentPath = this.mob.getNavigation().getPath();
                if (currentPath != null) {
                    Node endNode = currentPath.getEndNode();
                    if (endNode != null && target.distanceToSqr(endNode.x, endNode.y, endNode.z) < 1)
                        failedPathFindingPenalty = 0;
                    else
                        failedPathFindingPenalty += 10;
                } else {
                    failedPathFindingPenalty += 10;
                }
            }

            if (distanceToTargetSq > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distanceToTargetSq > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            if (!this.mob.getNavigation().moveTo(target, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 15;
            }

            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

        if (this.ticksUntilNextAttack > 0) return;

        this.checkAndPerformAttack(target, distanceToTargetSq);
    }

    protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSq) {
        double attackReach = this.getAttackReachSqr(target);
        if (distanceToTargetSq <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(target);
            System.out.println("Bogre attacked! Cooldown set to " + attackInterval + " ticks");
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackInterval);
    }

    protected double getAttackReachSqr(LivingEntity target) {
        return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + target.getBbWidth();
    }
}
