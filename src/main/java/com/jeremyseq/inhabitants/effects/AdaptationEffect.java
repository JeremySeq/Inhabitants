package com.jeremyseq.inhabitants.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AdaptationEffect extends MobEffect {
    public AdaptationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7CE9C3);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}