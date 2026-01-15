package com.jeremyseq.inhabitants.potions;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Inhabitants.MODID);

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}