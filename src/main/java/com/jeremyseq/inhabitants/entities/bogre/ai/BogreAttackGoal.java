package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.networking.ScreenShakePacketS2C;
import com.jeremyseq.inhabitants.entities.bogre.render.RoarEffectRenderer;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the attack sequence for the Bogre.
 * 
 * This goal is responsible for managing the attack sequence of the Bogre, 
 * including the roar, chase, and attack phases.
 * 
 * @jeremy should we change the name to BogreAggressiveGoal?
 */
public class BogreAttackGoal extends Goal {

    private final BogreEntity bogre;

    // --- Roaring constants ---
    private static final int ROAR_TICKS = 36;
    // --- Combat constants ---
    public static final float SHOCKWAVE_RADIUS = 9;
    public static final float SHOCKWAVE_DAMAGE = 28f;

    private static final int ATTACK_INTERVAL = 35;
    private static final int ANIMATION_DURATION = 14;
    public static final double MIN_ATTACK_DISTANCE = 3.0;
    public static final double MAX_ATTACK_DISTANCE = 5.5;
    private int ticksUntilNextAttack = 0;
    public static final float FORGET_ATTACK_RANGE = 40f; // distance to forget Player who attacked Bogre

    private Player roaredPlayer = null;
    private final List<Player> warnedPlayers = new ArrayList<>();
    private final Set<UUID> attackedByPlayers = new HashSet<>();

    public BogreAttackGoal(BogreEntity bogre) {
        this.bogre = bogre;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        boolean canUse = bogre.getAIState() == BogreAi.State.AGGRESSIVE || bogre.isRoaring();
        return canUse;
    }

    @Override
    public void tick() {
        forgetFarPlayers();

        if (bogre.isRoaring()) {
            handleRoaring();
            return;
        }

        if (bogre.getTarget() == null || !bogre.getTarget().isAlive()) {
            bogre.getNeutralGoal().enterIdle();
            return;
        }
        
        if (ticksUntilNextAttack > 0) {
            // cooldown faster while chasing to ensure immediate attack when arrived
            int reduction = (bogre.getAggressiveState() == BogreAi.AggressiveState.CHASING) ? 2 : 1;
            ticksUntilNextAttack = Math.max(0, ticksUntilNextAttack - reduction);
        }

        if (bogre.getAggressiveState() == BogreAi.AggressiveState.ATTACKING && bogre.getAiTicks() > 0) {
            handleAttackAnimation();
            return; 
        }

        runPhaseLogic();
    }
    
    private void handleAttackAnimation() {
        bogre.incrementAiTicks();
        
        // Face target
        if (bogre.getTarget() != null) {
            this.bogre.getLookControl().setLookAt(this.bogre.getTarget(), 60.0F, 60.0F);
        }

        // Shockwave
        if (bogre.getAiTicks() == 10) {
            this.bogre.triggerShockwave();
        }
        
        // Reset to chasing after attack
        if (bogre.getAiTicks() >= ANIMATION_DURATION) {
            bogre.resetAiTicks();
            bogre.setAggressiveState(BogreAi.AggressiveState.CHASING);
        }
        
        bogre.getNavigation().stop();
    }
    
    private void runPhaseLogic() {
        if (bogre.getTarget() == null) return;
        double distance = bogre.distanceTo(bogre.getTarget());

        if (distance <= MAX_ATTACK_DISTANCE) {
            if (bogre.getAggressiveState() != BogreAi.AggressiveState.ATTACKING) {
                enterAttacking();
            }
            handleAttacking();
        } else {
            if (bogre.getAggressiveState() != BogreAi.AggressiveState.CHASING) {
                enterChasing();
            }
            handleChasing();
        }
    }
    
    private void handleRoaring() {
        if (roaredPlayer == null || !roaredPlayer.isAlive()) {
            stopRoaring();
            return;
        }

        bogre.getLookControl().setLookAt(roaredPlayer, 60.0F, 60.0F);
        bogre.setAggressiveState(BogreAi.AggressiveState.ROARING);
        bogre.incrementAiTicks();

        if (bogre.getAiTicks() >= ROAR_TICKS) {
            this.setRoaredPlayer(null);
            bogre.resetAiTicks();
            bogre.setRoaring(false);
            this.exitRoaring();
            this.runPhaseLogic(); 
            return;
        }

        performRoarEffects();
    }

    private void performRoarEffects() {
        if (bogre.getAiTicks() == 10) {
            if (roaredPlayer instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(new ScreenShakePacketS2C(80), serverPlayer);
            }
            
            RoarEffectRenderer.addRoar(bogre, 35, 1.0);
        }
    }

    public void resetRoarTicks() {
        bogre.resetAiTicks();
        bogre.getEntityData().set(BogreEntity.SKILL_DURATION, ROAR_TICKS);
    }

    public void stopRoaring() {
        if (bogre.getNeutralGoal() != null) bogre.getNeutralGoal().enterIdle();
        this.setRoaredPlayer(null);
        bogre.resetAiTicks();
        this.exitRoaring();
    }

    public void exitRoaring() {
    }
    
    private void handleChasing() {
        bogre.setSprinting(true); // Trigger run animation
        bogre.getNavigation().moveTo(bogre.getTarget(), 1.4); // Pursue at elevated speed
        bogre.getLookControl().setLookAt(bogre.getTarget(), 60.0F, 60.0F);
    }
    
    private void handleAttacking() {
        bogre.getLookControl().setLookAt(bogre.getTarget(), 60.0F, 60.0F);
        
        if (ticksUntilNextAttack <= 0 && bogre.getAiTicks() == 0) {
            startAttackSequence();
        } else {
            double distance = bogre.distanceTo(bogre.getTarget());
            if (distance > MIN_ATTACK_DISTANCE + 0.5) {
                bogre.setSprinting(true); 
                bogre.getNavigation().moveTo(bogre.getTarget(), 1.4); 
            } else if (distance < MIN_ATTACK_DISTANCE - 0.5) {
                bogre.setSprinting(false);
                bogre.getNavigation().stop();
                bogre.getLookControl().setLookAt(bogre.getTarget(), 60.0F, 60.0F);
            } else {
                bogre.getNavigation().stop();
                bogre.getLookControl().setLookAt(bogre.getTarget(), 60.0F, 60.0F);
            }
        }
    }

    private void startAttackSequence() {
        bogre.resetAiTicks();
        bogre.incrementAiTicks();
        bogre.getEntityData().set(BogreEntity.SKILL_DURATION, ANIMATION_DURATION);
        ticksUntilNextAttack = ATTACK_INTERVAL;
        
        if (bogre.getTarget() != null) {
            LivingEntity target = bogre.getTarget();
            double dx = target.getX() - bogre.getX();
            double dz = target.getZ() - bogre.getZ();
            float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            bogre.setYRot(yaw);
            bogre.setYHeadRot(yaw);
            bogre.yBodyRot = yaw;
            
            bogre.getLookControl().setLookAt(target, 360.0F, 360.0F);
        }

        bogre.setSprinting(false);
        
        BogreAi.playAnimation(bogre, "attack");
        
        BogreSoundHandler.playAttackSound(bogre);
        
        bogre.getNavigation().stop();
    }

    private void forgetFarPlayers() {
        if (roaredPlayer != null) {
            if (bogre.distanceTo(roaredPlayer) > FORGET_ATTACK_RANGE || !roaredPlayer.isAlive()) {
                stopRoaring();
            }
        }
        
        if (!attackedByPlayers.isEmpty()) {
            attackedByPlayers.removeIf(uuid -> {
                Player player = bogre.level().getPlayerByUUID(uuid);
                return player == null ||
                bogre.distanceTo(player) > FORGET_ATTACK_RANGE ||
                !player.isAlive();
            });
        }
    }

    public Player getRoaredPlayer() {
        return roaredPlayer;
    }

    public void setRoaredPlayer(Player player) {
        this.roaredPlayer = player;
    }

    public List<Player> getWarnedPlayers() {
        return warnedPlayers;
    }

    public Set<UUID> getAttackedByPlayers() {
        return attackedByPlayers;
    }

    public int getRoarTicks() {
        return bogre.getAIState() == BogreAi.State.AGGRESSIVE &&
        bogre.getAggressiveState() == BogreAi.AggressiveState.ROARING ? bogre.getAiTicks() : 0;
    }

    public int getAttackTicks() {
        return bogre.getAIState() == BogreAi.State.AGGRESSIVE &&
        bogre.getAggressiveState() == BogreAi.AggressiveState.ATTACKING ? bogre.getAiTicks() : 0;
    }

    public int getMaxRoarTicks() {
        return ROAR_TICKS;
    }

    public int getMaxAttackTicks() {
        return ANIMATION_DURATION;
    }
    
    public void enterAttacking() {
        bogre.getAi().interruptSkilling();
        bogre.setAIState(BogreAi.State.AGGRESSIVE);
        bogre.setAggressiveState(BogreAi.AggressiveState.ATTACKING);
    }

    public void enterChasing() {
        bogre.setAIState(BogreAi.State.AGGRESSIVE);
        bogre.setAggressiveState(BogreAi.AggressiveState.CHASING);
    }

    public void enterRoaring() {
        enterRoaring(this.roaredPlayer);
    }

    public void enterRoaring(Player player) {
        bogre.getAi().stopAnimation("all");
        bogre.getAi().interruptSkilling();
        bogre.setAIState(BogreAi.State.AGGRESSIVE);
        bogre.setAggressiveState(BogreAi.AggressiveState.ROARING);
        bogre.setRoaring(true);
        bogre.setSprinting(false);
        BogreAi.playAnimation(bogre, "roar");
        
        BogreSoundHandler.playRoarSound(bogre);
        
        resetRoarTicks();
        if (player != null) {
            this.setRoaredPlayer(player);
            bogre.setTarget(player);
        }
    }
}
