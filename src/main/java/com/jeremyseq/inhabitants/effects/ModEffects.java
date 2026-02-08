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

    public static final RegistryObject<MobEffect> CONCUSSION =
            EFFECTS.register("concussion", ConcussionEffect::new);

    public static final RegistryObject<MobEffect> MONSTER_DISGUISE =
            EFFECTS.register("monster_disguise", MonsterDisguiseEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
