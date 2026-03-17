package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.entities.bogre.skill.*;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.debug.DevMode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Bogre Skilling Goal
 * - Handles the logic for Bogre skills
 * - Handles delivery
 */
public final class BogreSkillingGoal {
    public static final float HOSTILE_RANGE = 20.0f;
    public static final float FORGET_RANGE = 40.0f;
    public static final float DELIVER_DISTANCE = 3.5f;
    public static final int COOKING_START_OFFSET = 10;

    private BogreSkillingGoal() {}


    public static void aiStep(BogreEntity bogre) {
        if (bogre.getCraftingState() == BogreAi.SkillingState.DELIVERING) {
            handleDelivery(bogre);
            return;
        }

        BogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreAi.State.NEUTRAL);
            return;
        }
        
        if (bogre.getCraftingState() == BogreAi.SkillingState.NONE) {
            bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
        }

        if (bogre.getCraftingState() == BogreAi.SkillingState.PLACING_ITEM) {
            BogreSkills.forType(activeRecipe.type()).handlePlacingItem(bogre);
            return;
        }

        BogreSkills.forType(activeRecipe.type()).aiStep(bogre);
    }

    private static void handleDelivery(BogreEntity bogre) {
        Player player = bogre.getAi().getDroppedIngredientPlayer();
        if (player == null || !player.isAlive()) {
            player = bogre.level().getNearestPlayer(bogre, 10.0D);
            if (player == null) {
                exitItemDelivering(bogre);
                return;
            }
            bogre.getAi().setDroppedIngredientPlayer(player);
        }

        double distance = bogre.distanceToSqr(player);
        float stopDistance = 3.5f;

        BogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe != null) {
            if (activeRecipe.type() == BogreRecipe.Type.COOKING) {
                stopDistance = CookingSkill.dropResultOffset;
            } else if (activeRecipe.type() == BogreRecipe.Type.CARVING) {
                stopDistance = CarvingSkill.dropResultOffset;
            }
        }

        if (distance > stopDistance * stopDistance) {
            if (bogre.getNavigation().isDone() || bogre.tickCount % 10 == 0) {
                bogre.getNavigation().moveTo(player, 1.0D);
            }
            bogre.resetCookingTicks();
            return;
        }

        // Reached player: Delivery Phase
        bogre.getNavigation().stop();
        bogre.getLookControl().setLookAt(player, 30.0F, 30.0F);

        // Drop the item to the player
        exitItemDelivering(bogre);
    }

    public static void exitItemDelivering(BogreEntity bogre) {
        if (!bogre.getItemHeld().isEmpty()) {
            bogre.throwHeldItem();
        }
        finishSkilling(bogre);
    }

    private static void finishSkilling(BogreEntity bogre) {
        bogre.setAIState(BogreAi.State.NEUTRAL);
        bogre.setCraftingState(BogreAi.SkillingState.NONE);
        bogre.getAi().setActiveRecipe(null);
        bogre.getAi().setDroppedIngredientPlayer(null);
        bogre.getAi().setDroppedIngredientItem(null);
        bogre.getAi().setPathSet(false);
        bogre.getAi().resetStuckTicks();
        bogre.getAi().setCookingIngredientInCauldron(ItemStack.EMPTY);
    }


    public static void handleSkills(BogreEntity bogre) {
        BogreAi ai = bogre.getAi();
        List<Player> itemDroppers = bogre.level().getEntitiesOfClass(Player.class,
                bogre.getBoundingBox().inflate(FORGET_RANGE), (p -> !p.isSpectator()));

        for (Player player : itemDroppers) {
            float distance = player.distanceTo(bogre);
            
            if (distance > HOSTILE_RANGE || player.isCreative() ||
            !BogreUtil.isPlayerHoldingWeapon(player)) {

                List<ItemEntity> nearbyItems = bogre.level().getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(4),
                        item -> item.isAlive() &&
                        BogreRecipeManager.isCookingIngredient(item.getItem().getItem()));

                for (ItemEntity ingredient : nearbyItems) {
                    if (bogre.hasLineOfSight(ingredient)) {
                        Optional<BogreRecipe> recipe = BogreRecipeManager
                                .getCookingRecipe(ingredient.getItem().getItem());
                        if (recipe.isPresent()) {
                            if (bogre.cauldronPos == null || !bogre.isValidCauldron(bogre.cauldronPos)) {
                                continue;
                            }

                            ai.enterSkilling();
                            ingredient.setExtendedLifetime();
                            ai.setDroppedIngredientItem(ingredient);
                            ai.setDroppedIngredientPlayer(player);
                            ai.setActiveRecipe(recipe.get());
                            bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                            bogre.resetCookingTicks();
                            return;
                        }
                    }
                }
            }
        }

        // --- Carving ---
        
        List<BlockPos> carveableBlocks = BogreDetectionHelper.findCarvableBlocks(bogre, (int) BogreAi.ROAR_RANGE);

        if (carveableBlocks != null && !carveableBlocks.isEmpty()) {
            Optional<BogreRecipe> recipe = BogreRecipeManager
                    .getCarvingRecipe(bogre.level().getBlockState(carveableBlocks.get(0)).getBlock());
                if (recipe.isPresent()) {
                    ai.enterSkilling();
                    ai.setActiveRecipe(recipe.get());
                    ai.setDroppedIngredientPlayer(null); 
                    bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                    CarvingSkill.setCarveTicks(bogre, 0);
                    return;
                }
        }

        // --- Transformation ---

        ItemEntity transformationItem = BogreDetectionHelper
                .findTransformationItem(bogre, (int) TransformationSkill.detectionRadius);

        if (transformationItem != null) {
            Optional<BogreRecipe> recipe = BogreRecipeManager
                    .getTransformationRecipe(transformationItem.getItem().getItem());
                if (recipe.isPresent()) {
                    ai.enterSkilling();
                    ai.setActiveRecipe(recipe.get());
                    ai.setDroppedIngredientPlayer(null); 
                    bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                    TransformationSkill.setTransformationTicks(bogre, 0);
                    return;
                }
        }
    }
    
    public static boolean handleThrowingResult(BogreEntity bogre) {
        return BogreUtil.handleThrowingResult(bogre);
    }

    public static void throwHeldItem(BogreEntity bogre) {
        BogreUtil.throwHeldItem(bogre);
    }

    public static BogreCauldronEntity getCauldronEntity(BogreEntity bogre) {
        return bogre.getCauldronEntity();
    }

    public static ItemEntity findTransformationItem(BogreEntity bogre, int range) {
        return BogreDetectionHelper.findTransformationItem(bogre, range);
    }

    public static List<BlockPos> findCarvableBlocks(BogreEntity bogre, int radius) {
        return BogreDetectionHelper.findCarvableBlocks(bogre, radius);
    }

    public static BlockPos getAveragePosition(List<BlockPos> positions) {
        return BogreUtil.getAveragePosition(positions);
    }
}
