package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.recipe.CarvingRecipe;
import com.jeremyseq.inhabitants.recipe.IBogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogrePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.render.HammerEffectsRenderer;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;
import com.jeremyseq.inhabitants.entities.bogre.render.BogreAnimationHandler;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreSkillingGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class CarvingSkill extends BogreSkills.Skill {
    public static float dropResultOffset = 3.5f; // Distance from Carving block to drop result (player) (DELIVERING state)
    public static final float MIN_DISTANCE = 2.0f;
    public static final float MAX_DISTANCE = 3.0f;
    private final Map<UUID, List<BlockPos>> lockedCarvingBlocks = new HashMap<>();

    @Override
    public int getAnimationDuration(Animation animation) {
        return switch (animation) {
            case START -> 15; // 20 ticks at 1.3x speed = almost 15 ticks
            case LOOP -> 10;
            case END -> 20;
        };
    }

    @Override
    public int getDuration(BogreEntity bogre) {
        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe instanceof CarvingRecipe carving) {
            // START + (10 * hits)
            return getAnimationDuration(Animation.START) + (getAnimationDuration(Animation.LOOP) * carving.hammer_hits());
        }
        return getAnimationDuration(Animation.START) + 70;
    }
    
    @Override
    public IBogreRecipe.Type getType() {
        return IBogreRecipe.Type.CARVING;
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
    protected void finishSkill(BogreEntity bogre) {
        clearCracks(bogre);
        super.finishSkill(bogre);
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
        } else if (bogre.getCraftingState() == BogreAi.SkillingState.CARVING) {
            handleSkilling(bogre);
        }
    }

    @Override
    public void handleMovement(BogreEntity bogre) {
        List<BlockPos> carvableBlocks = BogreSkillingGoal.findCarvableBlocks(bogre, 12);
        if (carvableBlocks.isEmpty()) {
            finishSkill(bogre);
            return;
        }
        
        BlockPos first = carvableBlocks.get(0);
        BlockPos last = carvableBlocks.get(carvableBlocks.size() - 1);
        Vec3 boneTarget = new Vec3(
            (first.getX() + last.getX()) / 2.0 + 0.5,
            (first.getY() + last.getY()) / 2.0 + 0.5,
            (first.getZ() + last.getZ()) / 2.0 + 0.5
        );

        BogrePathNavigation nav = (BogrePathNavigation) bogre.getNavigation();
        if (nav.moveToCarvingTarget(boneTarget, MIN_DISTANCE, MAX_DISTANCE, carvableBlocks)) {
            lockedCarvingBlocks.put(bogre.getUUID(), new ArrayList<>(carvableBlocks));
            bogre.setCraftingState(BogreAi.SkillingState.CARVING);
            setCarveTicks(bogre, 0);
            
            double dx = boneTarget.x - bogre.getX();
            double dz = boneTarget.z - bogre.getZ();
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

        List<BlockPos> carvableBlocks = lockedCarvingBlocks.getOrDefault(bogre.getUUID(), 
                BogreSkillingGoal.findCarvableBlocks(bogre, 5));
        
        if (carvableBlocks.isEmpty()) {
            finishSkill(bogre);
            return;
        }

        BlockPos center = BogreSkillingGoal.getAveragePosition(carvableBlocks);
        Vec3 boneTarget = Vec3.atCenterOf(center);

        int ticks = getCarveTicks(bogre);
        if (ticks == 0) {
            bogre.getEntityData().set(BogreEntity.SKILL_HITS, 0);
            bogre.getEntityData().set(BogreEntity.TARGET_POS, carvableBlocks.get(bogre.getRandom().nextInt(carvableBlocks.size())));
            bogre.getEntityData().set(BogreEntity.SKILL_DURATION, getDuration(bogre));
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 0); // Start
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);
        } else if (ticks == getAnimationDuration(Animation.START)) {
            bogre.getEntityData().set(BogreEntity.ANIMATION_PHASE, 1); // Loop
        }
        
        if (ticks > 0 && ticks % 10 == 0) {
             bogre.getEntityData().set(BogreEntity.TARGET_POS, carvableBlocks.get(bogre.getRandom().nextInt(carvableBlocks.size())));
        }

        BlockPos targetPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
        if (isTargetBlock(targetPos)) {
            Vec3 hitTarget = Vec3.atCenterOf(targetPos);
            bogre.getLookControl().setLookAt(hitTarget.x, hitTarget.y + 0.5, hitTarget.z, 100f, 100f);
        } else {
            bogre.getLookControl().setLookAt(boneTarget.x, boneTarget.y + 0.5, boneTarget.z, 100f, 100f);
        }

        incrementCarveTicks(bogre);
    }

    // Called by ModEvents.onBlockBreak
    public void onBlockBroken(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        List<UUID> toFinish = new ArrayList<>();
        for (Map.Entry<UUID, List<BlockPos>> entry : lockedCarvingBlocks.entrySet()) {
            if (entry.getValue().contains(pos)) {
                toFinish.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toFinish) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof BogreEntity bogre) {
                finishSkill(bogre);
            }
        }
    }

    // Called by animation keyframes
    @Override
    public void keyframeTriggered(BogreEntity bogre, String name) {
        if (name.equals("skill_finished")) {
            if (!bogre.level().isClientSide) {
                IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
                if (activeRecipe != null) {
                    bogre.setItemHeld(activeRecipe.result().copy());
                    List<BlockPos> blocks = lockedCarvingBlocks.getOrDefault(bogre.getUUID(), 
                            BogreDetectionHelper.findCarvableBlocks(bogre, 5));
                    for (BlockPos pos : blocks) {
                        bogre.level().destroyBlock(pos, false);
                    }
                    bogre.playSound(SoundEvents.BONE_BLOCK_BREAK, 1.0F, 0.8F);
                }

                clearCracks(bogre);

                bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                BogreAi.playAnimation(bogre, "grab");
                bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                bogre.setCraftingState(BogreAi.SkillingState.DELIVERING);
                bogre.resetCookingTicks();
            }
            return;
        }

        if (!name.equals("hammer_sound")) return;

        if (bogre.getCraftingState() == BogreAi.SkillingState.CARVING) {
            IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
            if (!bogre.level().isClientSide && activeRecipe == null) return;
            
            if (!bogre.level().isClientSide && activeRecipe instanceof CarvingRecipe carving) {
                int currentHits = bogre.getEntityData().get(BogreEntity.SKILL_HITS) + 1;
                bogre.getEntityData().set(BogreEntity.SKILL_HITS, currentHits);
                
                int hammerHits = carving.hammer_hits();
                if (hammerHits < 1) hammerHits = 1;
 
                List<BlockPos> blocks = lockedCarvingBlocks.getOrDefault(bogre.getUUID(), 
                        BogreDetectionHelper.findCarvableBlocks(bogre, 5));
                int progress = (int) Math.min(9, ((currentHits - 1) * 10f / (float) hammerHits));

                BlockPos targetPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
                if (isTargetBlock(targetPos) && bogre.level().getBlockState(targetPos).isAir()) {
                    if (!blocks.isEmpty()) {
                        targetPos = blocks.get(bogre.getRandom().nextInt(blocks.size()));
                        bogre.getEntityData().set(BogreEntity.TARGET_POS, targetPos);
                    }
                }

                if (isTargetBlock(targetPos)) {
                    HammerEffectsRenderer.spawnCarvingParticles(bogre.level(), targetPos, bogre.level().getBlockState(targetPos));
                    for (int i = 0; i < blocks.size(); i++) {
                        bogre.level().destroyBlockProgress(bogre.getId() + i, blocks.get(i), progress);
                    }
                }
                
                if (currentHits >= hammerHits) {
                    bogre.playSound(ModSoundEvents.BOGRE_CARVING_FINISH.get(), 1.0F, 1.0F);
                    keyframeTriggered(bogre, "skill_finished");
                }
            } else {
                bogre.clientSkillHits++;
                int hammerHits = bogre.getEntityData().get(BogreEntity.HAMMER_HITS);
                if (hammerHits < 1) hammerHits = 1;

                if (bogre.clientSkillHits >= hammerHits) {
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

                List<BlockPos> blocks = BogreDetectionHelper.findCarvableBlocks(bogre, 5);
                BlockPos targetPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
                if (isTargetBlock(targetPos)) {
                    HammerEffectsRenderer.spawnCarvingParticles(bogre.level(), targetPos, bogre.level().getBlockState(targetPos));
                }
            }
        }
    }

    public static int getCarveTicks(BogreEntity bogre) {
        return bogre.getAiTicks();
    }

    public static void setCarveTicks(BogreEntity bogre, int ticks) {
        bogre.setAiTicks(ticks);
    }

    public static void incrementCarveTicks(BogreEntity bogre) {
        bogre.incrementAiTicks();
    }

    public void clearCracks(BogreEntity bogre) {
        if (!bogre.level().isClientSide) {
            List<BlockPos> blocks = lockedCarvingBlocks.remove(bogre.getUUID());
            if (blocks == null) {
                blocks = BogreDetectionHelper.findCarvableBlocks(bogre, 12);
            }
            
            for (int i = 0; i < 60; i++) {
                int id = bogre.getId() + i;
                if (blocks != null && i < blocks.size()) {
                    bogre.level().destroyBlockProgress(id, blocks.get(i), -1);
                }

                bogre.level().destroyBlockProgress(id, bogre.blockPosition(), -1);
            }
        }
    }

    private boolean isTargetBlock(BlockPos targetPos) {
        return targetPos != null && !targetPos.equals(BlockPos.ZERO);
    }
}
