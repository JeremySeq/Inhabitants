package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

/**
 * Handles Neutral state:
 * - Idle: in case the bogre is not dancing, roaring, or wandering.
 * - Wandering (clamped within cauldron range if set).
 * - Dancing (jukebox) stops movement.
 * - Roaring to warn players with weapons.
 * - Detections - Top priority
 */
public class BogreNeutralGoal extends WaterAvoidingRandomStrollGoal {
    private final BogreEntity bogre;

    public enum DancePhase { NONE, START, LOOP, END }

    public static final double MAX_CAULDRON_DIST_SQR = 14 * 14;

    // wander
    private int nextWanderTick = 0;
    public static final int MIN_WANDER_PAUSE = 80;
    public static final int WANDER_PAUSE_RANDOM = 60;


    public BogreNeutralGoal(BogreEntity bogre) {
        super(bogre, 1.0D);
        this.bogre = bogre;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bogre.getAIState() != BogreAi.State.NEUTRAL ||
            bogre.isRoaring()) {
            return false;
        }

        if (shouldDance(bogre)) {
            return true;
        }
        
        if (bogre.tickCount < nextWanderTick) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (bogre.getAIState() != BogreAi.State.NEUTRAL ||
            bogre.isRoaring()) {
            return false;
        }

        if (bogre.getNeutralState() == BogreAi.NeutralState.DANCING) {
            return shouldDance(bogre);
        }

        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        bogre.setNeutralState(BogreAi.NeutralState.WANDERING);
    }

    @Override
    public void stop() {
        super.stop();
        if (bogre.getNeutralState() == BogreAi.NeutralState.WANDERING || 
            bogre.getNeutralState() == BogreAi.NeutralState.DANCING) {
            bogre.setNeutralState(BogreAi.NeutralState.IDLE);
        }
        
        this.nextWanderTick = bogre.tickCount +
            MIN_WANDER_PAUSE +
            bogre.getRandom().nextInt(WANDER_PAUSE_RANDOM);
    }

    @Override
    public void tick() {
        handleRoaringInitiation();
        if (bogre.getAIState() == BogreAi.State.AGGRESSIVE || bogre.getAIState() == BogreAi.State.SKILLING) {
            return;
        }
        
        handleJukebox();
        
        if (bogre.getNeutralState() == BogreAi.NeutralState.DANCING) {
            handleDancing();
            return;
        }
        
        if (!bogre.isRoaring()) {
            super.tick();
            if (!bogre.getNavigation().isDone()) {
                bogre.setNeutralState(BogreAi.NeutralState.WANDERING);
            } else {
                bogre.setNeutralState(BogreAi.NeutralState.IDLE);
            }
        }
    }
    
    private void handleRoaringInitiation() {
        List<Player> withinRoarRange = bogre.level().getEntitiesOfClass(Player.class,
                bogre.getBoundingBox().inflate(BogreAi.ROAR_RANGE), Predicate.not(Player::isSpectator));

        withinRoarRange.sort((p1, p2) -> Float.compare(p1.distanceTo(bogre), p2.distanceTo(bogre)));

        for (Player player : withinRoarRange) {
            BogreAttackGoal attackGoal = bogre.getAttackGoal();
            if (attackGoal != null && !attackGoal.getWarnedPlayers().contains(player)
                    && player.distanceTo(bogre) <= BogreAi.ROAR_RANGE && bogre.hasLineOfSight(player)

                    && !player.isCreative() && !player.isSpectator()
                    && BogreUtil.isPlayerHoldingWeapon(player)) {

                attackGoal.enterRoaring(player);
                attackGoal.getWarnedPlayers().add(player);
                break;
            }
        }
    }

    @Override
    protected Vec3 getPosition() {
        if (bogre.cauldronPos != null) {
            for (int i = 0; i < 10; i++) {
                Vec3 candidate = super.getPosition();
                if (candidate == null) continue;

                double dist = candidate.distanceToSqr(Vec3.atCenterOf(bogre.cauldronPos));
                if (dist <= MAX_CAULDRON_DIST_SQR) return candidate;

            }
            return Vec3.atCenterOf(bogre.cauldronPos);
        }

        return super.getPosition();
    }

    public static boolean shouldDance(BogreEntity bogre) {
        return BogreDetectionHelper.isJukeboxPlayingNearby(bogre);
    }
    

    public void handleDancing() {
        if (!shouldDance(bogre)) {
            enterIdle();
        } else {
            bogre.getNavigation().stop();
            bogre.setDeltaMovement(Vec3.ZERO);
        }
    }

    public void handleJukebox() {
        if (bogre.getAIState() == BogreAi.State.NEUTRAL &&
            shouldDance(bogre) &&
            bogre.getNeutralState() != BogreAi.NeutralState.DANCING) {
            enterDancing();
        }
    }

    public void enterIdle() {
        bogre.setAIState(BogreAi.State.NEUTRAL);
        bogre.setNeutralState(BogreAi.NeutralState.IDLE);
        bogre.setCraftingState(BogreAi.SkillingState.NONE);
        bogre.getAi().setActiveRecipe(null);
        bogre.setSprinting(false);
    }

    public void enterWandering() {
        bogre.setAIState(BogreAi.State.NEUTRAL);
        bogre.setNeutralState(BogreAi.NeutralState.WANDERING);
        bogre.setSprinting(false);
    }

    public void enterDancing() {
        bogre.setAIState(BogreAi.State.NEUTRAL);
        bogre.setNeutralState(BogreAi.NeutralState.DANCING);
        bogre.getNavigation().stop();
        bogre.setTarget(null);
    }
}
