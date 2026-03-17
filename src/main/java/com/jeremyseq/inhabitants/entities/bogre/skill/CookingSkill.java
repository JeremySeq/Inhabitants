package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreSkillingGoal;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogrePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.List;

public class CookingSkill extends BogreSkills.Skill {
    public static float howFarFromCauldron = 1.5f; // Distance from cauldron to start cooking (MOVING_TO_TARGET state)
    public static float howFarFromItem = 2.25f; // Distance from item to start cooking (MOVING_TO_TARGET state)
    public static float dropResultOffset = 3.5f; // Distance from cauldron to drop result (DELIVERING state)

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
        BogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe != null) {
            return BogreSkillingGoal.COOKING_START_OFFSET +
            getAnimationDuration(Animation.START) +
            activeRecipe.time_ticks() +
            getAnimationDuration(Animation.END);
        }
        return BogreSkillingGoal.COOKING_START_OFFSET +
        getAnimationDuration(Animation.START) +
        55 +
        getAnimationDuration(Animation.END);
    }

    @Override
    public BogreRecipe.Type getType() {
        return BogreRecipe.Type.COOKING;
    }

    @Override
    public boolean canPerform(BogreEntity bogre) {
        return bogre.cauldronPos != null;
    }
    
    @Override
    public void cancel(BogreEntity bogre) {
        // Future TODO: make Bogre angry if player took item from cauldron while Bogre was cooking
        finishSkill(bogre);
    }

    @Override
    public void aiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            finishSkill(bogre);
            return;
        }

        if (BogreSkillingGoal.handleThrowingResult(bogre)) {
            return;
        }

        BogreAi.SkillingState state = bogre.getCraftingState();

        if (state == BogreAi.SkillingState.PLACING_ITEM || state == BogreAi.SkillingState.COOKING) {
            if (state == BogreAi.SkillingState.PLACING_ITEM) {
                handlePlacingItem(bogre);
            } else {
                handleSkilling(bogre);
            }
            return;
        }
        
        if (!bogre.getItemHeld().isEmpty()) {
            if (state == BogreAi.SkillingState.MOVING_TO_TARGET) {
                handleMovement(bogre);
            }
            return;
        }

        ItemEntity droppedIngredientItem = bogre.getAi().getDroppedIngredientItem();
        if (droppedIngredientItem == null || !droppedIngredientItem.isAlive()) {
            finishSkill(bogre);
            return;
        }

        double distance = bogre.distanceTo(droppedIngredientItem);
        if (distance > howFarFromItem) {
            if (bogre.getNavigation().isDone() || bogre.tickCount % 5 == 0) {
                BogrePathNavigation preciseNav = (BogrePathNavigation) bogre.getNavigation();
                Vec3 itemPos = droppedIngredientItem.position();
                Vec3 dir = bogre.position().subtract(itemPos).normalize();
                Vec3 approachPos = itemPos.add(dir.scale(1.5));
                preciseNav.preciseMoveTo(approachPos, 1.0D);
            }
            return;
        }

        if (bogre.getItemHeld().isEmpty()) {
            bogre.getNavigation().stop();
            bogre.triggerAnim("trigger_controller", "grab");
            ItemStack ingredientStack = droppedIngredientItem.getItem();
            
            if (ingredientStack.getCount() > 1) {
                ingredientStack.shrink(1);
                droppedIngredientItem.setItem(ingredientStack);
            } else {
                droppedIngredientItem.discard();
            }

            bogre.setItemHeld(new ItemStack(ingredientStack.getItem(), 1));
            bogre.getAi().setDroppedIngredientItem(null);
            bogre.resetCookingTicks();
            return;
        }
    }

    @Override
    public void handleMovement(BogreEntity bogre) {
        Vec3 targetCenter = getCauldronTargetPosition(bogre);
        if (targetCenter == null) {
            finishSkill(bogre);
            return;
        }

        handleStuck(bogre, targetCenter);

        double distance = bogre.position().distanceTo(targetCenter);
        BogrePathNavigation preciseNav = (BogrePathNavigation) bogre.getNavigation();

        if (distance > howFarFromCauldron) {
            if (!bogre.isPathSet() || bogre.tickCount % 20 == 0) {
                preciseNav.preciseMoveTo(targetCenter, 1.0D);
                bogre.setPathSet(true);
            }

            bogre.resetCookingTicks();
            return;
        }

        bogre.setPathSet(false);
        bogre.getNavigation().stop();
        bogre.setCraftingState(BogreAi.SkillingState.PLACING_ITEM);
        bogre.resetCookingTicks();
        bogre.getEntityData().set(BogreEntity.SKILL_DURATION, BogreSkillingGoal.COOKING_START_OFFSET);
    }

    @Override
    public void handlePlacingItem(BogreEntity bogre) {
        if (bogre.cauldronPos == null) {
            finishSkill(bogre);
            return;
        }

        bogre.lookAt(EntityAnchorArgument.Anchor.FEET, bogre.cauldronPos.getCenter());
        bogre.lookAt(EntityAnchorArgument.Anchor.EYES, bogre.cauldronPos.getCenter());

        if (bogre.getCookingTicks() == 0) {
            bogre.triggerAnim("trigger_controller", "grab");
        }
        
        if (bogre.getCookingTicks() == 4) {
            bogre.getAi().setCookingIngredientInCauldron(bogre.getItemHeld().copy());
            bogre.setItemHeld(ItemStack.EMPTY, false);
        }

        if (bogre.getCookingTicks() >= BogreSkillingGoal.COOKING_START_OFFSET) {
            bogre.setCraftingState(BogreAi.SkillingState.COOKING);
            bogre.resetCookingTicks(); // Start the actual cooking duration now
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, getDuration(bogre) - BogreSkillingGoal.COOKING_START_OFFSET);
        } else {
            bogre.incrementCookingTicks();
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

        if (bogre.getStuckTicks() > 40 && bogre.entrancePos != null) {
            Vec3 tp = Vec3.atCenterOf(bogre.entrancePos);
            bogre.setPos(tp.x, tp.y, tp.z);
            bogre.getNavigation().stop();
            bogre.resetStuckTicks();
            bogre.setPathSet(false);
        }
    }

    @Override
    public void handleSkilling(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
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
        
        if (bogre.getCookingTicks() == 0) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 0); // Start
            bogre.getEntityData().set(BogreEntity.COOKING_ANIM, true);
            bogreCauldron_final.setCooking(true);

        } else if (bogre.getCookingTicks() == getAnimationDuration(Animation.START)) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 1); // Loop

        } else if (bogre.getCookingTicks() ==
        getDuration(bogre) - BogreSkillingGoal.COOKING_START_OFFSET - getAnimationDuration(Animation.END)) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 2); // End


        } else if (bogre.getCookingTicks() >= getDuration(bogre) - BogreSkillingGoal.COOKING_START_OFFSET) {
            bogre.getAi().setCookingIngredientInCauldron(ItemStack.EMPTY);
            bogreCauldron_final.setCooking(false);
            
            ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
            if (activeRecipe.result().getItem() instanceof SuspiciousStewItem) {
                List<BogreRecipe.StewEffect> effects = activeRecipe.stewEffects();

                if (!effects.isEmpty()) {
                    BogreRecipe.StewEffect chosen = effects.get(bogre.getRandom().nextInt(effects.size()));
                    SuspiciousStewItem.saveMobEffect(stew, chosen.effect(), chosen.duration());
                    bogre.setItemHeld(stew);

                } else {
                    bogre.setItemHeld(activeRecipe.result().copy());
                }
            } else {
                bogre.setItemHeld(activeRecipe.result().copy());
            }
            
            bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
            bogre.triggerAnim("trigger_controller", "grab");
            
            bogre.setCraftingState(BogreAi.SkillingState.DELIVERING);
            bogre.setCookingTicks(0);
            return;
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

    private static Vec3 getCauldronTargetPosition(BogreEntity bogre) {
        BogreCauldronEntity bogreCauldron = bogre.getCauldronEntity();
        if (bogreCauldron == null) {
            return null;
        }

        Direction direction = bogreCauldron.getDirection();
        Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

        final float forwardDist = 2.2f;
        final float leftDist = howFarFromCauldron;

        Vec3i forwardI = direction.getNormal();
        Vec3i leftI = dirLeft.getNormal();
        Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
        Vec3 left = new Vec3(leftI.getX(), leftI.getY(), leftI.getZ()).scale(leftDist);

        return Vec3.atBottomCenterOf(bogre.cauldronPos).add(forward).add(left);
    }
}
