package com.jeremyseq.inhabitants.potions;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Inhabitants.MODID);

    public static final RegistryObject<Potion> IMMUNITY_POTION =
            POTIONS.register("immunity", () ->
                    new Potion(new MobEffectInstance(ModEffects.IMMUNITY.get(), 20 * 60)));

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}