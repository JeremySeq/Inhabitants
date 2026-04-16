package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.recipe.TransformationRecipe;
import com.jeremyseq.inhabitants.recipe.IBogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.ai.*;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreSkillingGoal;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogrePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.render.HammerEffectsRenderer;
import com.jeremyseq.inhabitants.entities.bogre.render.BogreAnimationHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

public class TransformationSkill extends BogreSkills.Skill {
    public static final float detectionRadius = 20.0f;
    public static final float MIN_DISTANCE = 1.5f;
    public static final float MAX_DISTANCE = 2.0f;

    @Override
    public int getAnimationDuration(Animation animation) {
        return switch (animation) {
            case START -> 15; // 20 ticks at 1.3x speed = almost 15 ticks
            case LOOP -> 0;
            case END -> 20;
        };
    }

    @Override
    public int getDuration(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe instanceof TransformationRecipe transformation) {
            // START + (10 * hits)
            return getAnimationDuration(Animation.START) + (10 * transformation.hammer_hits());
        }
        return getAnimationDuration(Animation.START) + 70;
    }

    @Override
    public IBogreRecipe.Type getType() {
        return IBogreRecipe.Type.TRANSFORMATION;
    }

    @Override
    public boolean canPerform(BogreEntity bogre) {
        return true;
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

        if (bogre.getCraftingState() == BogreAi.SkillingState.MOVING_TO_TARGET) {
            handleMovement(bogre);
        } else if (bogre.getCraftingState() == BogreAi.SkillingState.TRANSFORMATION) {
            handleSkilling(bogre);
        }
    }

    @Override
    public void handleMovement(BogreEntity bogre) {
        ItemEntity nearestTransformationItem =
        BogreSkillingGoal.findTransformationItem(bogre, (int) detectionRadius);

        if (nearestTransformationItem == null || !nearestTransformationItem.isAlive()) {
            finishSkill(bogre);
            return;
        }

        Vec3 itemPos = nearestTransformationItem.position();
        double dx = bogre.getX() - itemPos.x;
        double dz = bogre.getZ() - itemPos.z;
        Vec3 dir;

        if (dx * dx + dz * dz < 0.01) {
            dir = bogre.getForward();
        } else {
            dir = new Vec3(dx, 0, dz).normalize();
        }
        
        float targetDist = (MIN_DISTANCE + MAX_DISTANCE) / 2.0f;
        Vec3 moveTarget = new Vec3(itemPos.x + dir.x * targetDist, itemPos.y, itemPos.z + dir.z * targetDist);

        double distance = bogre.distanceTo(nearestTransformationItem);

        if (bogre.getNavigation().isDone() ||
        bogre.tickCount % 5 == 0) {
            BogrePathNavigation preciseNav = (BogrePathNavigation) bogre.getNavigation();
            preciseNav.preciseMoveTo(moveTarget, 1.0D);
        }
        
        if (distance >= MIN_DISTANCE &&
        distance <= MAX_DISTANCE && Math.abs(bogre.getY() - itemPos.y) < 1.5) {
            bogre.getNavigation().stop();
            bogre.setCraftingState(BogreAi.SkillingState.TRANSFORMATION);
            setTransformationTicks(bogre, 0);
            
            dx = itemPos.x - bogre.getX();
            dz = itemPos.z - bogre.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            bogre.setYRot(yaw);
            bogre.setYHeadRot(yaw);
            bogre.yBodyRot = yaw;
        }
    }

    @Override
    public void handleSkilling(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            finishSkill(bogre);
            return;
        }

        // validate transformation item is still present and valid
        ItemEntity nearestTransformationItem = BogreSkillingGoal.findTransformationItem(bogre, (int) BogreAi.ROAR_RANGE);
        if (nearestTransformationItem == null || !nearestTransformationItem.isAlive()) {
            finishSkill(bogre);
            return;
        }

        bogre.getLookControl().setLookAt(
            nearestTransformationItem.getX(),
            nearestTransformationItem.getY(),
            nearestTransformationItem.getZ(),
            100f, 100f
        );

        int ticks = getTransformationTicks(bogre);
        if (ticks == 0) {
            bogre.getEntityData().set(BogreEntity.SKILL_HITS, 0);
            bogre.getEntityData().set(BogreEntity.TARGET_POS, nearestTransformationItem.blockPosition());
            bogre.getEntityData().set(BogreEntity.TARGET_ENTITY_ID, nearestTransformationItem.getId());
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, getDuration(bogre));
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 0); // Start
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);

            // special flag for music disc transformation visuals
            if (activeRecipe instanceof TransformationRecipe transformation && 
                transformation.ingredient().toString().equals("music_disc_11")) {
                bogre.getEntityData().set(BogreEntity.IS_TRANSFORMING_DISC, true);
                nearestTransformationItem.setInvisible(true);
            }
        } else if (ticks == getAnimationDuration(Animation.START)) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 1); // Loop
        }

        incrementTransformationTicks(bogre);
    }

    @Override
    public void keyframeTriggered(BogreEntity bogre, String name) {
        if (name.equals("skill_finished")) {
            if (!bogre.level().isClientSide) {
                IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
                ItemEntity nearestTransformationItem = BogreSkillingGoal.findTransformationItem(bogre, (int) BogreAi.ROAR_RANGE);
                if (activeRecipe != null && nearestTransformationItem != null) {
                    Vec3 dropPos = nearestTransformationItem.position();
                    nearestTransformationItem.discard();
                    if (activeRecipe.result() != null && !activeRecipe.result().isEmpty()) {

                        ItemEntity itemEntity = new
                        ItemEntity(bogre.level(),
                        dropPos.x, dropPos.y + 0.5, dropPos.z,
                        activeRecipe.result().copy());

                        itemEntity.setDeltaMovement(
                            (bogre.getRandom().nextDouble() * 0.2 - 0.1),
                            0.45,
                            (bogre.getRandom().nextDouble() * 0.2 - 0.1)
                        );

                        itemEntity.setDefaultPickUpDelay();
                        bogre.level().addFreshEntity(itemEntity);
                        bogre.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.2F);
                    }
                }

                bogre.getNavigation().stop();
                bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                setTransformationTicks(bogre, 0);
                bogre.resetCookingTicks();
                bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                bogre.getEntityData().set(BogreEntity.TARGET_ENTITY_ID, -1);
                bogre.getEntityData().set(BogreEntity.IS_TRANSFORMING_DISC, false);
                finishSkill(bogre);
            }
            return;
        }

        if (!name.equals("hammer_sound")) return;

        if (bogre.getCraftingState() == BogreAi.SkillingState.TRANSFORMATION) {
            IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
            if (!bogre.level().isClientSide && activeRecipe == null) return;
            
            if (!bogre.level().isClientSide && activeRecipe instanceof TransformationRecipe transformation) {
                int currentHits = bogre.getEntityData().get(BogreEntity.SKILL_HITS) + 1;
                bogre.getEntityData().set(BogreEntity.SKILL_HITS, currentHits);
                
                int expectedHits = transformation.hammer_hits();
                if (expectedHits < 1) expectedHits = 1;

                ItemEntity nearestTransformationItem = BogreSkillingGoal.findTransformationItem(bogre, (int) BogreAi.ROAR_RANGE);
                Vec3 precisePos = null;
                if (nearestTransformationItem != null) {
                    precisePos = nearestTransformationItem.position();
                }

                if (precisePos == null) {
                    BlockPos targetPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
                    precisePos = Vec3.atCenterOf(targetPos);
                }

                HammerEffectsRenderer.spawnTransformationParticles(bogre.level(), precisePos);
                
                if (currentHits >= expectedHits && nearestTransformationItem != null) {
                    keyframeTriggered(bogre, "skill_finished");
                }
            } else {
                bogre.clientSkillHits++;
                int expectedHits = bogre.getEntityData().get(BogreEntity.HAMMER_HITS);
                if (expectedHits < 1) expectedHits = 1;

                if (bogre.clientSkillHits >= expectedHits) {
                    bogre.hammerHideTicks = 10;
                    bogre.isHammerHidden = true;
                    bogre.clientSkillHits = 0;
                    
                    BogreAnimationHandler.getBonePosition(bogre, "hammer").ifPresent(pos -> {
                        for (int i = 0; i < 7; ++i) {
                            double d0 = bogre.getRandom().nextGaussian() * 0.02D;
                            double d1 = bogre.getRandom().nextGaussian() * 0.02D;
                            double d2 = bogre.getRandom().nextGaussian() * 0.02D;
                            bogre.level().addParticle(ParticleTypes.POOF, 
                                pos.x, pos.y, pos.z, d0, d1, d2);
                        }
                    });
                }

                int targetId = bogre.getEntityData().get(BogreEntity.TARGET_ENTITY_ID);
                Vec3 precisePos = null;
                if (targetId != -1) {
                    Entity targetEntity = bogre.level().getEntity(targetId);
                    if (targetEntity != null) {
                        precisePos = targetEntity.position();
                    }
                }
                
                if (precisePos == null) {
                    BlockPos targetPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
                    precisePos = Vec3.atCenterOf(targetPos);
                }
                
                HammerEffectsRenderer.spawnTransformationParticles(bogre.level(), precisePos);
            }
        }
    }

    public static int getTransformationTicks(BogreEntity bogre) {
        return bogre.getAiTicks();
    }

    public static void setTransformationTicks(BogreEntity bogre, int ticks) {
        bogre.setAiTicks(ticks);
    }

    public static void incrementTransformationTicks(BogreEntity bogre) {
        bogre.incrementAiTicks();
    }
}
