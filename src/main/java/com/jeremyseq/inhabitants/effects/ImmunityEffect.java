package com.jeremyseq.inhabitants.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ImmunityEffect extends MobEffect {
    public ImmunityEffect() {
        super(MobEffectCategory.NEUTRAL, 0x664f82);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}