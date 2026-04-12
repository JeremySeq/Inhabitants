package com.jeremyseq.inhabitants.audio;

import com.jeremyseq.inhabitants.Inhabitants;
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

    public static final RegistryObject<SoundEvent> BOGRE_ATTACK = registerSoundEvent("bogre.attack");
    public static final RegistryObject<SoundEvent> BOGRE_DEATH = registerSoundEvent("bogre.death");
    public static final RegistryObject<SoundEvent> BOGRE_HURT = registerSoundEvent("bogre.hurt");
    public static final RegistryObject<SoundEvent> BOGRE_IDLE = registerSoundEvent("bogre.idle");
    public static final RegistryObject<SoundEvent> BOGRE_ROAR = registerSoundEvent("bogre.roar");
    public static final RegistryObject<SoundEvent> SHOCKWAVE = registerSoundEvent("shockwave");
    public static final RegistryObject<SoundEvent> CAULDRON_GUI_OPEN = registerSoundEvent("bogre.cauldron_gui_open");

    public static final RegistryObject<SoundEvent> IMPALER_ATTACK = registerSoundEvent("impaler.attack");
    public static final RegistryObject<SoundEvent> IMPALER_DEATH = registerSoundEvent("impaler.death");
    public static final RegistryObject<SoundEvent> IMPALER_HURT = registerSoundEvent("impaler.hurt");
    public static final RegistryObject<SoundEvent> IMPALER_IDLE = registerSoundEvent("impaler.idle");
    public static final RegistryObject<SoundEvent> IMPALER_SCREAM = registerSoundEvent("impaler.scream");
    public static final RegistryObject<SoundEvent> IMPALER_SPIKES = registerSoundEvent("impaler.spikes");
    public static final RegistryObject<SoundEvent> IMPALER_CONCUSSION = registerSoundEvent("impaler.concussion");

    public static final RegistryObject<SoundEvent> BOGRE_SONG = registerSoundEvent("disc.bogre");
    public static final RegistryObject<SoundEvent> BOGRE_HAMMER_KNOCK = registerSoundEvent("bogre.hammer_knock");
    public static final RegistryObject<SoundEvent> BOGRE_CARVING_FINISH = registerSoundEvent("bogre.carving_finish");
    public static final RegistryObject<SoundEvent> BOGRE_DEPOSIT_IN_CHEST = registerSoundEvent("bogre.deposit_in_chest");

    public static final RegistryObject<SoundEvent> BOGRE_COOKING_START = registerSoundEvent("bogre.cooking_start");
    public static final RegistryObject<SoundEvent> BOGRE_COOKING_LOOP = registerSoundEvent("bogre.cooking_loop");

    public static final RegistryObject<SoundEvent> REVERSE_GROWTH = registerSoundEvent("effects.reverse_growth");
    public static final RegistryObject<SoundEvent> IMMATERIAL_ENTER_WALL = registerSoundEvent("immaterial.enter_wall");
    public static final RegistryObject<SoundEvent> IMMATERIAL_EXIT_WALL = registerSoundEvent("immaterial.exit_wall");
    public static final RegistryObject<SoundEvent> IMMATERIAL_INSIDE = registerSoundEvent("immaterial.inside");
    public static final RegistryObject<SoundEvent> CONCUSSION_BUZZ = registerSoundEvent("concussion.buzz");
    
    public static final RegistryObject<SoundEvent> JAVELIN_AIMING = registerSoundEvent("item.javelin_aiming");
    public static final RegistryObject<SoundEvent> JAVELIN_BOUNCE = registerSoundEvent("item.javelin_bounce");
    public static final RegistryObject<SoundEvent> JAVELIN_LAUNCHING = registerSoundEvent("item.javelin_launch");
    public static final RegistryObject<SoundEvent> JAVELIN_ON_BLOCK_HIT = registerSoundEvent("item.javelin_on_block_hit");
    public static final RegistryObject<SoundEvent> JAVELIN_ON_ENTITY_HIT = registerSoundEvent("item.javelin_on_entity_hit");

    public static final RegistryObject<SoundEvent> DRILL_LOOP = registerSoundEvent("item.spike_drill.drilling_loop");
    public static final RegistryObject<SoundEvent> DRILL_STOPPED = registerSoundEvent("item.spike_drill.drilling_stopped");
    public static final RegistryObject<SoundEvent> DRILL_START = registerSoundEvent("item.spike_drill.drilling_start");
    public static final RegistryObject<SoundEvent> DRILL_DIG = registerSoundEvent("item.spike_drill.drilling_dig");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}