package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Inhabitants.MODID);

    public static final RegistryObject<MobEffect> CONCUSSION =
            EFFECTS.register("concussion", ConcussionEffect::new);

    public static final RegistryObject<MobEffect> UNDEAD_DISGUISE =
            EFFECTS.register("undead_disguise", UndeadDisguiseEffect::new);

    public static final RegistryObject<MobEffect> STICKY_LEGS =
            EFFECTS.register("sticky_legs", StickyLegsEffect::new);

    public static final RegistryObject<MobEffect> REVERSE_GROWTH =
            EFFECTS.register("reverse_growth", ReverseGrowthEffect::new);

    public static final RegistryObject<MobEffect> IMMATERIAL =
            EFFECTS.register("immaterial", ImmaterialEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
