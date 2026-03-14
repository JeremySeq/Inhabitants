package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreCraftingManager;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;

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

public class CookingSkill extends BogreSkills.Skill {

    @Override
    public int getDuration() {
        return 160;
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
    public void aiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getActiveRecipe();
        if (activeRecipe == null) {
            finishSkill(bogre);
            return;
        }

        if (BogreCraftingManager.handleThrowingResult(bogre)) {
            return;
        }

        if (!bogre.getItemHeld().isEmpty()) {
            BogreCauldronEntity bogreCauldron = BogreCraftingManager.getCauldronEntity(bogre);
            if (bogreCauldron == null) {
                finishSkill(bogre);
                return;
            }
            Direction direction = bogreCauldron.getDirection();
            Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

            final float forwardDist = 2.25f;
            final float leftDist = .9f;

            Vec3i forwardI = direction.getNormal();
            Vec3i leftI = dirLeft.getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
            Vec3 left = new Vec3(leftI.getX(), leftI.getY(), leftI.getZ()).scale(leftDist);

            Vec3 targetCenter = Vec3.atBottomCenterOf(bogre.cauldronPos).add(forward).add(left);

            double distSqr = bogre.distanceToSqr(targetCenter);
            PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
            Vec3 currentPos = bogre.position();

            if (bogre.getLastPos() != null) {
                double movedSq = currentPos.distanceToSqr(bogre.getLastPos());
                if (movedSq < 0.0025 && distSqr > 0.3) {
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
                return;
            }

            if (!bogre.isPathSet() && distSqr > .3) {
                preciseNav.preciseMoveTo(targetCenter, 1.0D);
                bogre.setPathSet(true);
                return;
            }

            if (distSqr > .3) return;

            bogre.setPathSet(false);
            bogre.getNavigation().stop();

            bogre.lookAt(EntityAnchorArgument.Anchor.FEET, bogre.cauldronPos.getCenter());
            bogre.lookAt(EntityAnchorArgument.Anchor.EYES, bogre.cauldronPos.getCenter());
            
            if (bogre.getCookingTicks() == BogreEntity.DROP_FISH_OFFSET) {
                bogre.triggerAnim("grab", "grab");
            } else if (bogre.getCookingTicks() == 25) {
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, true);
                bogreCauldron.setCooking(true);
            }

            bogre.incrementCookingTicks();

            if (bogre.getCookingTicks() >= getDuration()) {
                bogre.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0F, 0.8F);
                if (bogre.getDroppedIngredientPlayer() != null) {
                    bogre.getTamedPlayers().add(bogre.getDroppedIngredientPlayer().getUUID());
                }
                bogreCauldron.setCooking(false);

                float stewChance = activeRecipe.suspiciousStewChance();

                if (bogre.level().random.nextFloat() < stewChance) {
                    ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
                    MobEffectInstance[] effects = new MobEffectInstance[] {
                            new MobEffectInstance(MobEffects.SATURATION, 7),
                            new MobEffectInstance(MobEffects.NIGHT_VISION, 80),
                            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80),
                            new MobEffectInstance(MobEffects.BLINDNESS, 160),
                            new MobEffectInstance(MobEffects.WEAKNESS, 180),
                            new MobEffectInstance(MobEffects.REGENERATION, 160),
                            new MobEffectInstance(MobEffects.JUMP, 80),
                            new MobEffectInstance(MobEffects.POISON, 240),
                            new MobEffectInstance(MobEffects.WITHER, 160)
                    };

                    MobEffectInstance chosen = effects[bogre.level().random.nextInt(effects.length)];
                    SuspiciousStewItem.saveMobEffect(stew, chosen.getEffect(), chosen.getDuration());
                    bogre.setItemHeld(stew);
                } else {
                    bogre.setItemHeld(activeRecipe.result().copy());
                }
                bogre.setChowderThrowDelay(20);
                bogre.resetCookingTicks();
            }
            return;
        }

        ItemEntity droppedIngredientItem = bogre.getDroppedIngredientItem();
        if (droppedIngredientItem == null || !droppedIngredientItem.isAlive()) {
            finishSkill(bogre);
            return;
        }

        double distance = bogre.distanceTo(droppedIngredientItem);
        if (distance > 2.5) {
            if (bogre.getNavigation().isDone() || bogre.tickCount % 20 == 0) {
                PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
                Vec3 itemPos = droppedIngredientItem.position();
                Vec3 dir = bogre.position().subtract(itemPos).normalize();
                Vec3 approachPos = itemPos.add(dir.scale(1.5));
                preciseNav.preciseMoveTo(approachPos, 1.0D);
            }
            return;
        }

        if (bogre.getItemHeld().isEmpty()) {
            bogre.getNavigation().stop();
            bogre.triggerAnim("grab", "grab");
            ItemStack ingredientStack = droppedIngredientItem.getItem();
            
            if (ingredientStack.getCount() > 1) {
                ingredientStack.shrink(1);
            } else {
                droppedIngredientItem.discard();
            }

            bogre.setItemHeld(new ItemStack(ingredientStack.getItem(), 1));
            bogre.setDroppedIngredientItem(null);
            return;
        }
    }
}
