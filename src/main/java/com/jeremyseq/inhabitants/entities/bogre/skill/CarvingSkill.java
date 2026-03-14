package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreCraftingManager;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CarvingSkill extends BogreSkills.Skill {

    @Override
    public BogreRecipe.Type getType() {
        return BogreRecipe.Type.CARVING;
    }

    @Override
    public boolean canPerform(BogreEntity bogre) {
        return true; 
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

        List<BlockPos> carvePositions = bogre.getCarvePositions();
        if (carvePositions == null || carvePositions.isEmpty()) {
            finishSkill(bogre);
            return;
        }

        BlockPos center = BogreCraftingManager.getAveragePosition(carvePositions);
        double distance = bogre.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        if (Math.sqrt(distance) > 2.5) {
            if (bogre.getNavigation().isDone() || bogre.tickCount % 20 == 0) {
                PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
                Vec3 target = Vec3.atCenterOf(center);
                Vec3 dir = bogre.position().subtract(target).normalize();
                Vec3 approachPos = target.add(dir.scale(2.0));
                preciseNav.preciseMoveTo(approachPos, 1.0D);
            }
            bogre.resetCarveTicks();
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            return;
        }

        bogre.getNavigation().stop();
        
        Vec3 boneTarget = Vec3.atCenterOf(center);
        bogre.getLookControl().setLookAt(boneTarget.x, boneTarget.y + 0.5, boneTarget.z, 100f, 100f);

        if (bogre.getCarveTicks() == 0) {
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);
        }

        // validate blocks are still present
        if (activeRecipe.triggerBlock().isPresent()) {
            Block requiredBlock = activeRecipe.triggerBlock().get();
            for (BlockPos pos : carvePositions) {
                if (!bogre.level().getBlockState(pos).is(requiredBlock)) {
                    bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                    bogre.resetCarveTicks();
                    finishSkill(bogre);
                    return;
                }
            }
        }
        
        bogre.incrementCarveTicks();

        int timeTicks = activeRecipe.timeTicks();

        if (bogre.getCarveTicks() >= timeTicks) {
            bogre.setItemHeld(activeRecipe.result().copy());
            bogre.setChowderThrowDelay(20);
            
            for (BlockPos pos : carvePositions) {
                bogre.level().destroyBlock(pos, false);
            }
            bogre.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F);
            
            bogre.setCarvePositions(List.of());
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
        }
    }
}
