package com.jeremyseq.inhabitants.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DeafeningEffect extends MobEffect {
    protected DeafeningEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
    }
}
