package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreCraftingManager;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TransformationSkill extends BogreSkills.Skill {

    @Override
    public int getDuration() {
        return 100;
    }

    @Override
    public BogreRecipe.Type getType() {
        return BogreRecipe.Type.TRANSFORMATION;
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

        BlockPos center = carvePositions.get(0);
        ItemEntity nearestBrokenDisc = BogreCraftingManager.findBrokenDisc(bogre, (int) BogreEntity.ROAR_RANGE);

        if (nearestBrokenDisc != null) {
            double distance = bogre.distanceTo(nearestBrokenDisc);
            if (distance > 3.5) {
                Vec3 discPos = nearestBrokenDisc.position();
                Vec3 dir = bogre.position().subtract(discPos).normalize();
                Vec3 approachPos = discPos.add(dir.scale(2.5));
                PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
                preciseNav.preciseMoveTo(approachPos, 1.0D);
                bogre.resetCarveTicks();
                bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                return;
            }
        }

        bogre.getNavigation().stop();
        
        if (nearestBrokenDisc != null) {
            bogre.getLookControl().setLookAt(nearestBrokenDisc.getX(), nearestBrokenDisc.getY(), nearestBrokenDisc.getZ(), 100f, 100f);
        } else {
            Vec3 discCenter = Vec3.atCenterOf(center);
            bogre.getLookControl().setLookAt(discCenter.x, discCenter.y + 0.5, discCenter.z, 100f, 100f);
        }

        if (bogre.getCarveTicks() == 0) {
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);
        }

        // validate disc is still present and valid
        if (nearestBrokenDisc == null || !nearestBrokenDisc.isAlive()) {
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
            finishSkill(bogre);
            return;
        }
        
        bogre.incrementCarveTicks();

        if (bogre.getCarveTicks() >= getDuration()) {
            if (nearestBrokenDisc.isAlive()) {
                nearestBrokenDisc.discard();
                bogre.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F);
            }
            
            if (activeRecipe.result() != null && !activeRecipe.result().isEmpty()) {

                ItemEntity resultItem = new ItemEntity(bogre.level(),
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                activeRecipe.result().copy());

                resultItem.setDefaultPickUpDelay();
                bogre.level().addFreshEntity(resultItem);
            }
            
            bogre.getNavigation().stop();
            bogre.setCarvePositions(List.of());
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
            finishSkill(bogre);
        }
    }
}
