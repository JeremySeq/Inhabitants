package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

import java.util.Optional;

public class BulltoadBreedGoal extends BreedGoal {
    public BulltoadBreedGoal(BulltoadEntity pAnimal, double pSpeedModifier) {
        super(pAnimal, pSpeedModifier);
    }

    @Override
    protected void breed() {
        if (partner == null) return;
        final BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(animal, partner, null);
        final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        if (cancelled) {
            animal.setAge(6000);
            partner.setAge(6000);
            animal.resetLove();
            partner.resetLove();
            return;
        }

        ((BulltoadEntity) animal).setBreedStage(1);

        Optional.ofNullable(animal.getLoveCause()).or(() -> Optional.ofNullable(partner.getLoveCause())).ifPresent((p_277486_) -> {
            p_277486_.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(p_277486_, animal, partner, null);
        });
        animal.setAge(6000);
        partner.setAge(6000);
        animal.resetLove();
        partner.resetLove();
        this.level.broadcastEntityEvent(animal, (byte)18);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
        }
    }
}
