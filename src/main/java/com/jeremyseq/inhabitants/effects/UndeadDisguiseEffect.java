package com.jeremyseq.inhabitants.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class UndeadDisguiseEffect extends MobEffect {
    protected UndeadDisguiseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x834418);
    }

    public static double getEffectRadius() {
        return 10.0;
    }
}