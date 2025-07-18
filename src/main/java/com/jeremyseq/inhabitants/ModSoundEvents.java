package com.jeremyseq.inhabitants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Inhabitants.MODID);

    public static final RegistryObject<SoundEvent> WARPED_CLAM_CLOSING = registerSoundEvent("warped_clam.closing");
    public static final RegistryObject<SoundEvent> WARPED_CLAM_OPENING = registerSoundEvent("warped_clam.opening");
    public static final RegistryObject<SoundEvent> WARPED_CLAM_CLOSED_DAMAGE = registerSoundEvent("warped_clam.closed_damage");
    public static final RegistryObject<SoundEvent> WARPED_CLAM_OPENED_DAMAGE = registerSoundEvent("warped_clam.opened_damage");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}