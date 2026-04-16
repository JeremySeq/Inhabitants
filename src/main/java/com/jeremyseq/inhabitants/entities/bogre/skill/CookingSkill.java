package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.recipe.*;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreSkillingGoal;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogrePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

public class CookingSkill extends BogreSkills.Skill {
    private final float howFarFromCauldron = 2.5f; // Distance from cauldron to start cooking (MOVING_TO_TARGET state)
    public static float dropResultOffset = 3.5f; // Distance from cauldron to drop result (DELIVERING state)

    // Chest durations
    public static final int chestOpeningDuration = 10;
    public static final int chestDepositingDuration = 12;
    public static final int chestClosingDuration = 8;

    @Override
    public int getAnimationDuration(Animation animation) {
        return switch (animation) {
            case START -> 37; // 55 ticks at 1.5x speed = almost 37 ticks
            case LOOP -> 0; // Handled by activeRecipe.timeTicks()
            case END -> 20;
        };
    }

    @Override
    public int getDuration(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe instanceof CookingRecipe cooking) {
            return cooking.time_ticks();
        }
        return 160;
    }

    @Override
    public IBogreRecipe.Type getType() {
        return IBogreRecipe.Type.COOKING;
    }

    @Override
    public boolean canPerform(BogreEntity bogre) {
        return bogre.cauldronPos != null;
    }
    
    @Override
    public void cancel(BogreEntity bogre) {
        finishSkill(bogre);
    }

    @Override
    public void aiStep(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            finishSkill(bogre);
            return;
        }

        BogreCauldronEntity cauldron = bogre.getCauldronEntity();
        if (cauldron == null || !cauldron.hasHeatSource()) {
             finishSkill(bogre);
             return;
        }

        BogreAi.SkillingState state = bogre.getCraftingState();

        if (state == BogreAi.SkillingState.COOKING) {
            handleSkilling(bogre);
            return;
        }

        // Bogre only cooks items already in the cauldron Gui
        if (cauldron != null && cauldron.isReadyToCook()) {
            if (state == BogreAi.SkillingState.MOVING_TO_TARGET) {
                handleMovement(bogre);
            }
            return;
        }

        finishSkill(bogre);
    }

    @Override
    public void handleMovement(BogreEntity bogre) {
        Vec3 targetCenter = getCauldronTargetPosition(bogre);
        if (targetCenter == null) {
            finishSkill(bogre);
            return;
        }

        handleStuck(bogre, targetCenter);

        BogrePathNavigation preciseNav = (BogrePathNavigation) bogre.getNavigation();

        if (preciseNav.moveToCauldronPrecise(targetCenter, 0.15f)) {
            bogre.setCraftingState(BogreAi.SkillingState.COOKING);
            bogre.resetCookingTicks();
            
            Vec3 target = Vec3.atCenterOf(bogre.cauldronPos);
            double dx = target.x - bogre.getX();
            double dz = target.z - bogre.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            bogre.setYRot(yaw);
            bogre.setYHeadRot(yaw);
            bogre.yBodyRot = yaw;
        }
    }

    private void handleStuck(BogreEntity bogre, Vec3 targetCenter) {
        Vec3 currentPos = bogre.position();
        double distance = bogre.position().distanceTo(targetCenter);

        if (bogre.getLastPos() != null) {
            double movedSq = currentPos.distanceToSqr(bogre.getLastPos());
            if (movedSq < 0.0025 && distance > howFarFromCauldron) {
                bogre.incrementStuckTicks();
            } else {
                bogre.resetStuckTicks();
            }
        }

        bogre.setLastPos(currentPos);

        if (bogre.getStuckTicks() > 80 && bogre.entrancePos != null) {
            Vec3 tp = Vec3.atCenterOf(bogre.entrancePos);
            bogre.setPos(tp.x, tp.y, tp.z);
            bogre.getNavigation().stop();
            bogre.resetStuckTicks();
            bogre.setPathSet(false);
        }
    }

    @Override
    public void handleSkilling(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            finishSkill(bogre);
            return;
        }

        bogre.lookAt(EntityAnchorArgument.Anchor.FEET, bogre.cauldronPos.getCenter());
        bogre.lookAt(EntityAnchorArgument.Anchor.EYES, bogre.cauldronPos.getCenter());
        
        BogreCauldronEntity bogreCauldron_final = bogre.getCauldronEntity();
        if (bogreCauldron_final == null) {
            finishSkill(bogre);
            return;
        }
        
        int startDuration = getAnimationDuration(Animation.START);

        if (bogre.getCookingTicks() == 0) {
            if (!bogreCauldron_final.isReadyToCook()) {
                finishSkill(bogre);
                return;
            }
            
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 0); // Start
            bogre.getEntityData().set(BogreEntity.COOKING_ANIM, true);
            bogreCauldron_final.setCooking(true);
            bogreCauldron_final.setCookingProgress(0);

        } else if (bogre.getCookingTicks() == startDuration) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 1); // Loop

        } else if (bogre.getCookingTicks() ==
        getDuration(bogre) - BogreSkillingGoal.COOKING_START_OFFSET - getAnimationDuration(Animation.END)) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 2); // End
            bogreCauldron_final.setCookingProgress(0);
            
            //notify cauldron is no longer cooking
            ItemStack result = bogreCauldron_final.finishCooking();
            if (!result.isEmpty()) {
                bogre.getAi().setStoredSkillResult(result);
            }

            bogreCauldron_final.setCooking(false);

        } else if (bogre.getCookingTicks() >= getDuration(bogre) - BogreSkillingGoal.COOKING_START_OFFSET) {
            //grab the item from the container slot 4
            ItemStack result = bogre.getAi().getStoredSkillResult();
            if (!result.isEmpty()) {
                bogre.setItemHeld(result);
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                BogreAi.playAnimation(bogre, "grab");
                
                bogre.setCraftingState(BogreAi.SkillingState.DELIVERING);
                bogre.getAi().setStoredSkillResult(ItemStack.EMPTY);
                bogre.setCookingTicks(0);
            } else {
                // if nothing was extracted, just finish the skill
                finishSkill(bogre);
            }
            return;
        }

        if (bogre.getEntityData().get(BogreEntity.ANIMATION_PHASE) == 1) {
            int currentLoopTicks = bogre.getCookingTicks() - startDuration;
            
            int loopDuration =
                getDuration(bogre) -
                BogreSkillingGoal.COOKING_START_OFFSET -
                getAnimationDuration(Animation.END) -
                startDuration;

            int progress = 1 + (int)((currentLoopTicks / (float)loopDuration) * 159);
            bogreCauldron_final.setCookingProgress(progress);
        }

        bogre.incrementCookingTicks();
    }

    public static void handleCauldron(BogreEntity bogre) {
        if (bogre.cauldronPos == null || !bogre.isValidCauldron(bogre.cauldronPos)) {
            bogre.cauldronPos = BogreDetectionHelper.findNearestCauldron(bogre, 10).orElse(null);
        }

        if (bogre.cauldronPos != null) {
            BogreCauldronEntity bogreCauldron = bogre.getCauldronEntity();
            if (bogreCauldron == null) {
                if (bogre.getAIState() != BogreAi.State.SKILLING) {
                    if (bogre.getNeutralGoal() != null) bogre.getNeutralGoal().enterIdle();
                }
                return;
            }
            Direction direction = bogreCauldron.getDirection();
            Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

            final float forwardDist = 4;
            final float rightDist = 11;

            Vec3i forwardI = direction.getNormal();
            Vec3i rightI = dirLeft.getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
            Vec3 right = new Vec3(rightI.getX(), rightI.getY(), rightI.getZ()).scale(rightDist);

            Vec3 targetCenter = Vec3.atBottomCenterOf(bogre.cauldronPos).add(forward).subtract(right);
            bogre.entrancePos = BlockPos.containing(targetCenter.x, targetCenter.y, targetCenter.z);
        }
    }

    private Vec3 getCauldronTargetPosition(BogreEntity bogre) {
        if (bogre.cauldronPos == null) return null;
        BogreCauldronEntity bogreCauldron = bogre.getCauldronEntity();
        if (bogreCauldron != null) {
            Vec3i forwardI = bogreCauldron.getDirection().getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ());
            return Vec3.atCenterOf(bogre.cauldronPos).add(forward.scale(howFarFromCauldron));
        }
        return Vec3.atCenterOf(bogre.cauldronPos).add(0, 0, -howFarFromCauldron); // fallback
    }
}
