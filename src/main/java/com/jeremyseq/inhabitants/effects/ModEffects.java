package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Inhabitants.MODID);

    public static final RegistryObject<MobEffect> IMMUNITY =
            EFFECTS.register("immunity", ImmunityEffect::new);

    public static final RegistryObject<MobEffect> ADAPTATION =
            EFFECTS.register("adaptation", AdaptationEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
