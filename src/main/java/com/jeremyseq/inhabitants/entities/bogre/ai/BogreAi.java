package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.entities.bogre.skill.*;
import com.jeremyseq.inhabitants.recipe.*;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

/**
 * The brain of the bogre.
 * 
 * @jeremy should we name it bogreBrain.java ?
 */

public class BogreAi {
    private final BogreEntity bogre;

    // --- AI States ---
    public enum State {
        NEUTRAL, AGGRESSIVE, SKILLING
    }

    // --- AI Sub-States ---
    public enum NeutralState {
        IDLE, WANDERING, DANCING
    }

    public enum AggressiveState {
        ATTACKING, ROARING, CHASING
    }

    public enum SkillingState {
        NONE, COOKING, CARVING, TRANSFORMATION,
        DELIVERING, MOVING_TO_TARGET
    }

    public enum DeliveryState {
        SEARCHING, DELIVERING_TO_PLAYER, MOVING_TO_CHEST,
        OPENING_CHEST, DEPOSITING, CLOSING_CHEST
    }

    private DeliveryState deliveryState = DeliveryState.SEARCHING;

    // --- AI Constants ---
    public static final float FORGET_RANGE = 20f;
    public static final float ROAR_RANGE = 12f;

    private IBogreRecipe activeRecipe = null;
    private ItemStack storedSkillResult = ItemStack.EMPTY;
    private UUID resultOwnerUUID = null; // Player who initiated the current recipe

    private Vec3 lastPos = null;
    private int stuckTicks = 0;
    private boolean pathSet = false;
    private int skillingMoveSide = 0; // 1: +X, -1: -X, 2: +Z, -2: -Z (0: NONE)
    private int deliveryFailures = 0;

    public BogreAi(BogreEntity bogre) {
        this.bogre = bogre;
    }

    public void registerGoals() {
        bogre.goalSelector.addGoal(2, new BogreAttackGoal(bogre));
        bogre.goalSelector.addGoal(3, new BogreSkillingGoalWrap(bogre));
        bogre.goalSelector.addGoal(6, new BogreNeutralGoal(bogre));
    }

    public void aiStep() {
        
        if (bogre.getAIState() == State.NEUTRAL && !bogre.isRoaring()) {
            BogreSkillingGoal.handleSkills(bogre);
            if (bogre.getAIState() == State.SKILLING) return;
        }

        CookingSkill.handleCauldron(bogre);
        handleHandItem();
        handleStuckFailsafe();

        if (bogre.getTarget() != null) {
            
            if (bogre.distanceTo(bogre.getTarget()) > FORGET_RANGE ||
            !bogre.getTarget().isAlive() ||
            bogre.getTarget().isDeadOrDying()) {
                bogre.setTarget(null);

            } else if (bogre.getTarget() instanceof Player player &&
            (player.isCreative() || player.isSpectator())) {
                bogre.setTarget(null);

            } else if (bogre.getTarget() instanceof Player player &&
                !BogreUtil.isPlayerHoldingWeapon(player) &&
                (bogre.getAttackGoal() == null ||
                !bogre.getAttackGoal().getAttackedByPlayers().contains(player.getUUID())) &&
                bogre.getAIState() != State.AGGRESSIVE) {
                    bogre.setTarget(null);

                if (bogre.getAttackGoal() != null) {
                    bogre.getAttackGoal().getWarnedPlayers().remove(player);
                }
            } else {
                if (bogre.getAIState() != State.AGGRESSIVE) {
                    if (bogre.getAttackGoal() != null)
                        bogre.getAttackGoal().enterAttacking();
                }
                return;
            }
        }
    }

    public void handleStuckFailsafe() {
        if (bogre.getAIState() == State.SKILLING) {
            // Only apply stuck failsafe during movement phases
            if (bogre.getCraftingState() != SkillingState.MOVING_TO_TARGET && 
                bogre.getCraftingState() != SkillingState.DELIVERING) {
                this.resetStuckTicks();
                return;
            }

            Vec3 currentPos = bogre.position();
            if (this.getLastPos() != null && this.getLastPos().distanceToSqr(currentPos) < 0.01) {
                this.incrementStuckTicks();
                if (this.getStuckTicks() > 100) {
                    // Check if stuck because of chains above cauldron
                    if (bogre.cauldronPos != null) {
                        int chains = 0;
                        for (int i = 1; i <= 3; i++) {
                            if (bogre.level().getBlockState(bogre.cauldronPos.above(i))
                            .is(Blocks.CHAIN)) {
                                chains++;
                            }
                        }
                        if (chains >= 2) {
                            if (this.getStuckTicks() < 300) return; 
                        }
                    }

                    if (bogre.getNeutralGoal() != null) bogre.getNeutralGoal().enterIdle();
                    this.resetStuckTicks();
                }
            } else {
                this.setLastPos(currentPos);
                this.resetStuckTicks();
            }
        }
    }

    public void handleHandItem() {
        // Handle hand item logic
    }

    public void enterSkilling() {
        bogre.setAIState(State.SKILLING);
    }

    public boolean hasProgress() {
        State state = bogre.getAIState();
        if (state == State.AGGRESSIVE) {
            AggressiveState agg = bogre.getAggressiveState();
            return agg == AggressiveState.ROARING ||
            agg == AggressiveState.ATTACKING;
        }
        if (state == State.SKILLING) {
            SkillingState skill = bogre.getCraftingState();
            return skill == SkillingState.COOKING ||
                    skill == SkillingState.CARVING ||
                    skill == SkillingState.TRANSFORMATION ||
                    skill == SkillingState.DELIVERING;
        }
        return false;
    }

    public void interruptSkilling() {
        if (bogre.getAIState() == State.SKILLING) {
            IBogreRecipe recipe = getActiveRecipe();
            if (recipe != null && recipe.getBogreRecipeType() == IBogreRecipe.Type.CARVING) {
                BogreSkills.CARVING.clearCracks(bogre);
            }

            stopAnimation("all");
            bogre.getNavigation().stop();
            if (!bogre.getItemHeld().isEmpty()) {
                bogre.throwHeldItem();
            }

            bogre.setCraftingState(SkillingState.NONE);
            bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);

            this.setActiveRecipe(null);
            this.setPathSet(false);
            this.resetStuckTicks();
            this.setSkillingMoveSide(0);
        }
    }

    public void stopAnimation(String... type) {
        for (String t : type) {
            switch (t) {
                case "cooking" -> bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                case "carving" -> bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                case "dancing" -> {
                    if (bogre.getNeutralState() == NeutralState.DANCING) {
                        if (bogre.getNeutralGoal() != null)
                            bogre.getNeutralGoal().enterIdle();
                    }
                }
                case "roar" -> {
                    if (bogre.getAggressiveState() == AggressiveState.ROARING) {
                        if (bogre.getAttackGoal() != null)
                            bogre.getAttackGoal().enterChasing();
                    }
                }
                case "all" -> {
                    bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                    bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                    if (bogre.getNeutralState() == NeutralState.DANCING) {
                        if (bogre.getNeutralGoal() != null)
                            bogre.getNeutralGoal().enterIdle();
                    }
                    if (bogre.getAggressiveState() == AggressiveState.ROARING) {
                        if (bogre.getAttackGoal() != null)
                            bogre.getAttackGoal().enterChasing();
                    }
                    bogre.resetAiTicks();
                }
            }
        }
    }

    public IBogreRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public void setActiveRecipe(IBogreRecipe recipe) {
        this.activeRecipe = recipe;
        if (recipe != null) {
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, BogreSkills
                    .forType(recipe.getBogreRecipeType()).getDuration(bogre));

            if (recipe instanceof CarvingRecipe carving) {
                bogre.getEntityData().set(BogreEntity.HAMMER_HITS, carving.hammer_hits());
                if (carving.hammerSound().isPresent()) {
                    bogre.getEntityData().set(BogreEntity.HAMMER_SOUND,
                            carving.hammerSound().get().getLocation().toString());
                } else {
                    bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
                }
            } else if (recipe instanceof TransformationRecipe transformation) {
                bogre.getEntityData().set(BogreEntity.HAMMER_HITS, transformation.hammer_hits());
                if (transformation.hammerSound().isPresent()) {
                    bogre.getEntityData().set(BogreEntity.HAMMER_SOUND,
                            transformation.hammerSound().get().getLocation().toString());
                } else {
                    bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
                }
            } else {
                bogre.getEntityData().set(BogreEntity.HAMMER_HITS, 1);
                bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
            }
        } else {
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, 130);
            bogre.getEntityData().set(BogreEntity.HAMMER_HITS, 1);
            bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
            this.resultOwnerUUID = null;
        }
    }

    public Vec3 getLastPos() {
        return lastPos;
    }

    public void setLastPos(Vec3 pos) {
        this.lastPos = pos;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public void incrementStuckTicks() {
        this.stuckTicks++;
    }

    public void resetStuckTicks() {
        this.stuckTicks = 0;
    }

    public boolean isPathSet() {
        return pathSet;
    }

    public void setPathSet(boolean pathSet) {
        this.pathSet = pathSet;
    }

    public int getSkillingMoveSide() {
        return skillingMoveSide;
    }

    public void setSkillingMoveSide(int skillingMoveSide) {
        this.skillingMoveSide = skillingMoveSide;
    }

    public UUID getResultOwnerUUID() {
        return resultOwnerUUID;
    }

    public void setResultOwnerUUID(UUID ownerUUID) {
        this.resultOwnerUUID = ownerUUID;
    }

    private boolean shouldDance() {
        return BogreNeutralGoal.shouldDance(this.bogre);
    }
    
    // remove this later
    private static class BogreSkillingGoalWrap extends Goal {
        private final BogreEntity bogre;

        public BogreSkillingGoalWrap(BogreEntity bogre) {
            this.bogre = bogre;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return bogre.getAIState() == State.SKILLING;
        }

        @Override
        public void tick() {
            BogreSkillingGoal.aiStep(bogre);
        }
    }

    public void setDeliveryState(DeliveryState state) {
        this.deliveryState = state;
        bogre.getEntityData().set(BogreEntity.DELIVERY_STATE, state.ordinal());
    }

    public DeliveryState getDeliveryState() {
        return DeliveryState.values()[bogre.getEntityData().get(BogreEntity.DELIVERY_STATE)];
    }

    public void setStoredSkillResult(ItemStack stack) {
        this.storedSkillResult = stack;
    }

    public ItemStack getStoredSkillResult() {
        return storedSkillResult;
    }

    public static boolean playAnimation(BogreEntity bogre, String name) {
        if (name == null || name.isEmpty())
            return false;

        bogre.triggerAnim("trigger_controller", name);
        return true;
    }

    public int getDeliveryFailures() {
        return deliveryFailures;
    }

    public void setDeliveryFailures(int deliveryFailures) {
        this.deliveryFailures = deliveryFailures;
    }

    public void incrementDeliveryFailures() {
        this.deliveryFailures++;
    }
}
