package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.skill.*;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.recipe.CarvingRecipe;
import com.jeremyseq.inhabitants.recipe.TransformationRecipe;
import com.jeremyseq.inhabitants.recipe.IBogreRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Bogre Skilling Goal
 * - Handles the logic for Bogre skills
 */
public final class BogreSkillingGoal {
    public static final float SKILLING_RANGE = 20.0f;
    public static final float BOGRE_RANGE = 40.0f;
    public static final float NEARBY_PLAYERS_RANGE = 10.0f;
    public static final int COOKING_START_OFFSET = 10;

    private BogreSkillingGoal() {}

    public static void aiStep(BogreEntity bogre) {
        if (bogre.getCraftingState() == BogreAi.SkillingState.DELIVERING) {
            BogreDeliveryGoal.handleDelivery(bogre);
            return;
        }

        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreAi.State.NEUTRAL);
            return;
        }

        if (bogre.getCraftingState() == BogreAi.SkillingState.NONE) {
            bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
        }

        BogreSkills.forType(activeRecipe.getBogreRecipeType()).aiStep(bogre);
    }

    public static void handleSkills(BogreEntity bogre) {
        BogreAi ai = bogre.getAi();

        List<BogreCauldronEntity> cauldrons = bogre.level().getEntitiesOfClass(
                BogreCauldronEntity.class,
                bogre.getBoundingBox().inflate(SKILLING_RANGE),
                cauldron -> cauldron.isAlive() && !cauldron.isCooking());

        for (BogreCauldronEntity cauldron : cauldrons) {
            if (cauldron.getItemCount() > 0 && cauldron.hasHeatSource() && 
                !cauldron.getItemHandler().getStackInSlot(4).isEmpty()) {
                
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = cauldron.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty())
                        items.add(stack.getItem());
                }

                Optional<CookingRecipe> recipe = BogreRecipeManager.getCookingRecipe(items);

                if (recipe.isPresent() &&
                    cauldron.getItemHandler().getStackInSlot(4).is(recipe.get().container())) {
                    
                    if (!isJobClaimed(bogre, cauldron.blockPosition(), -1, true, null) 
                        && isClosestIdleBogreToPosition(bogre, cauldron.position())) {
                        
                        if (bogre.cauldronPos == null ||
                                !bogre.isValidCauldron(bogre.cauldronPos)) {
                            bogre.cauldronPos = cauldron.blockPosition();
                        }

                        if (bogre.cauldronPos.equals(cauldron.blockPosition())) {
                            ai.enterSkilling();
                            ai.setActiveRecipe(recipe.get());
                            captureResultOwner(bogre, null); // for now, we will use nearest player
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
            BlockPos carvePos = carveableBlocks.get(0);
            Optional<CarvingRecipe> recipe = BogreRecipeManager.getCarvingRecipe(
                    bogre.level().getBlockState(carvePos).getBlock());
            
            if (recipe.isPresent() && !isJobClaimed(bogre, null, -1, false, carveableBlocks) 
                && isClosestIdleBogreToPosition(bogre, Vec3.atCenterOf(carvePos))) {
                
                ai.enterSkilling();
                ai.setActiveRecipe(recipe.get());
                captureResultOwner(bogre, null); // for now, we will use nearest player
                bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                CarvingSkill.setCarveTicks(bogre, 0);
                bogre.getEntityData().set(BogreEntity.TARGET_POS, carvePos);
                return;
            }
        }

        // --- Transformation ---

        ItemEntity transformationItem = BogreDetectionHelper
                .findTransformationItem(bogre, (int) TransformationSkill.detectionRadius);

        if (transformationItem != null) {
            Optional<TransformationRecipe> recipe = BogreRecipeManager
                    .getTransformationRecipe(transformationItem.getItem().getItem());
            
            if (recipe.isPresent() && !isJobClaimed(bogre, null, transformationItem.getId(), false, null) 
                && isClosestIdleBogreToPosition(bogre, transformationItem.position())) {
                
                ai.enterSkilling();
                ai.setActiveRecipe(recipe.get());
                captureResultOwner(bogre, transformationItem.getOwner());
                bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                TransformationSkill.setTransformationTicks(bogre, 0);
                bogre.getEntityData().set(BogreEntity.TARGET_ENTITY_ID, transformationItem.getId());
                return;
            }
        }
    }

    private static boolean isJobClaimed(BogreEntity bogre, BlockPos targetBlock,
    int targetEntityId, boolean isCauldron, List<BlockPos> carveableBlocks) {

        List<BogreEntity> nearbyBogres = bogre.level().getEntitiesOfClass(
                BogreEntity.class, bogre.getBoundingBox().inflate(BOGRE_RANGE));
                
        for (BogreEntity other : nearbyBogres) {
            if (other.getUUID().equals(bogre.getUUID())) continue;
            
            if (other.getAIState() == BogreAi.State.SKILLING) {
                if (isCauldron && targetBlock != null && targetBlock.equals(other.cauldronPos)) {
                    return true;
                }
                
                if (carveableBlocks != null && !carveableBlocks.isEmpty()) {
                    BlockPos p = other.getEntityData().get(BogreEntity.TARGET_POS);
                    if (p != null && carveableBlocks.contains(p)) return true;
                }
                
                int currentTargetId = other.getEntityData().get(BogreEntity.TARGET_ENTITY_ID);
                if (targetEntityId != -1 && targetEntityId == currentTargetId) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isClosestIdleBogreToPosition(BogreEntity bogre, Vec3 targetPos) {
        Optional<BogreEntity> closest = BogreDetectionHelper.findClosestBogre(
                bogre.level(),
                targetPos,
                SKILLING_RANGE,
                other -> other.getAIState() == BogreAi.State.NEUTRAL && !other.isRoaring()
        );

        return closest.isPresent() && closest.get().getUUID().equals(bogre.getUUID());
    }

    private static void captureResultOwner(BogreEntity bogre, Entity owner) {
        if (owner == null) {
            owner = bogre.level().getNearestPlayer(bogre, NEARBY_PLAYERS_RANGE);
        }
        if (owner != null) {
            bogre.getAi().setResultOwnerUUID(owner.getUUID());
        }
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
