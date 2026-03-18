package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.debug.DevMode;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreNeutralGoal;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.entities.bogre.skill.*;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * The brain of the bogre.
 * @jeremy should we name it bogreBrain.java ?
 */

public class BogreAi {
    private final BogreEntity bogre;

    // --- AI States ---
    public enum State { NEUTRAL, AGGRESSIVE, SKILLING }
    // --- AI Sub-States ---
    public enum NeutralState { IDLE, WANDERING, DANCING }
    public enum AggressiveState { ATTACKING, ROARING, CHASING }
    public enum SkillingState { NONE, COOKING, CARVING, TRANSFORMATION,
    DELIVERING, MOVING_TO_TARGET, PLACING_ITEM }

    // --- AI Constants ---
    public static final float FORGET_RANGE = 20f;
    public static final float ROAR_RANGE = 12f;
    public static final float HOSTILE_RANGE = 10f;

    private BogreRecipe activeRecipe = null;
    private ItemEntity droppedIngredientItem = null;
    private Player droppedIngredientPlayer = null;
    private int cookingItemThrowDelay = -1;

    // GUI Future TODO: remove this and make list in the cauldron entity
    private ItemStack cookingIngredientInCauldron = ItemStack.EMPTY; 

    private Vec3 lastPos = null;
    private int stuckTicks = 0;
    private boolean pathSet = false;
    private int skillingMoveSide = 0; // 1: +X, -1: -X, 2: +Z, -2: -Z (0: NONE)

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
                    if (bogre.getAttackGoal() != null) bogre.getAttackGoal().enterAttacking();
                }
                return;
            }
        }
    }

    public void handleStuckFailsafe() {
        if (bogre.getAIState() == State.SKILLING) {
            // Only apply stuck failsafe during movement phases
            if (bogre.getCraftingState() != SkillingState.MOVING_TO_TARGET && 
                bogre.getCraftingState() != SkillingState.DELIVERING &&
                bogre.getCraftingState() != SkillingState.PLACING_ITEM) {
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
                   skill == SkillingState.PLACING_ITEM;
        }
        return false;
    }

    public void interruptSkilling() {
        if (bogre.getAIState() == State.SKILLING) {
            BogreRecipe recipe = getActiveRecipe();
            if (recipe != null && recipe.type() == BogreRecipe.Type.CARVING) {
                CarvingSkill.clearCracks(bogre);
            }

            stopAnimation();
            if (!bogre.getItemHeld().isEmpty()) {
                bogre.throwHeldItem();
            }

            // drop item from cauldron if interrupted
            if (!this.getCookingIngredientInCauldron().isEmpty() && bogre.cauldronPos != null) {
                EntityUtil.throwItemStack(
                    bogre.level(), 
                    Vec3.atCenterOf(bogre.cauldronPos).add(0, 0.8, 0),
                    Vec3.ZERO,
                    this.getCookingIngredientInCauldron(), 
                    0.0f, 
                    0.1f
                );

                this.setCookingIngredientInCauldron(ItemStack.EMPTY);
            }

            bogre.setCraftingState(SkillingState.NONE);
            bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
            
            this.setActiveRecipe(null);
            this.setDroppedIngredientPlayer(null);
            this.setDroppedIngredientItem(null);
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
                        if (bogre.getNeutralGoal() != null) bogre.getNeutralGoal().enterIdle();
                    }
                }
                case "roar" -> {
                    if (bogre.getAggressiveState() == AggressiveState.ROARING) {
                        if (bogre.getAttackGoal() != null) bogre.getAttackGoal().enterChasing();
                    }
                }
                case "all" -> {
                    bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                    bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                    if (bogre.getNeutralState() == NeutralState.DANCING) {
                        if (bogre.getNeutralGoal() != null) bogre.getNeutralGoal().enterIdle();
                    }
                    if (bogre.getAggressiveState() == AggressiveState.ROARING) {
                        if (bogre.getAttackGoal() != null) bogre.getAttackGoal().enterChasing();
                    }
                    bogre.resetAiTicks();
                }
            }
        }
    }

    public BogreRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public void setActiveRecipe(BogreRecipe recipe) {
        this.activeRecipe = recipe;
        if (recipe != null) {
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, BogreSkills
            .forType(recipe.type()).getDuration(bogre));

            bogre.getEntityData().set(BogreEntity.HAMMER_HITS, recipe.hammer_hits());

            if (recipe.hammerSound().isPresent()) {
                bogre.getEntityData().set(BogreEntity.HAMMER_SOUND,
                recipe.hammerSound().get().getLocation().toString());
                
            } else {
                bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
            }
        } else {
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, 130);
            bogre.getEntityData().set(BogreEntity.HAMMER_HITS, 1);
            bogre.getEntityData().set(BogreEntity.HAMMER_SOUND, "");
        }
    }

    public ItemEntity getDroppedIngredientItem() {
        return droppedIngredientItem;
    }

    public void setDroppedIngredientItem(ItemEntity droppedIngredientItem) {
        this.droppedIngredientItem = droppedIngredientItem;
    }

    public Player getDroppedIngredientPlayer() {
        return droppedIngredientPlayer;
    }

    public void setDroppedIngredientPlayer(Player droppedIngredientPlayer) {
        this.droppedIngredientPlayer = droppedIngredientPlayer;
    }

    public int getCookingItemThrowDelay() {
        return cookingItemThrowDelay;
    }

    public void setCookingItemThrowDelay(int cookingItemThrowDelay) {
        this.cookingItemThrowDelay = cookingItemThrowDelay;
    }

    public void decrementCookingItemThrowDelay() {
        if (this.cookingItemThrowDelay >= 0) {
            this.cookingItemThrowDelay--;
        }
    }

    public ItemStack getCookingIngredientInCauldron() {
        return cookingIngredientInCauldron;
    }

    public void setCookingIngredientInCauldron(ItemStack stack) {
        this.cookingIngredientInCauldron = stack != null ? stack : ItemStack.EMPTY;
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
}
