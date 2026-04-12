package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

public class BogreSoundHandler {

    public static SoundEvent getAmbientSound() {
        return ModSoundEvents.BOGRE_IDLE.get();
    }

    public static SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ModSoundEvents.BOGRE_HURT.get();
    }

    public static SoundEvent getDeathSound() {
        return ModSoundEvents.BOGRE_DEATH.get();
    }

    public static void playStepSound(BogreEntity bogre, @NotNull BlockPos bPos, @NotNull BlockState bState) {
        bogre.playSound(bState.getSoundType().getStepSound(), bogre.isSprinting() ? 2f : 1.25f, 0.9f);
    }

    public static void playRoarSound(BogreEntity bogre) {
        bogre.playSound(ModSoundEvents.BOGRE_ROAR.get(), 2.0F, 0.9F + bogre.getRandom().nextFloat() * 0.2F);
    }

    public static void playAttackSound(BogreEntity bogre) {
        bogre.playSound(ModSoundEvents.BOGRE_ATTACK.get(), 1.0f, 1.0f);
    }

    public static void playHammerKnockSound(BogreEntity bogre) {
        bogre.playSound(ModSoundEvents.BOGRE_HAMMER_KNOCK.get(), 1.0f, 0.8f + bogre.getRandom().nextFloat() * 0.4f);
    }
}