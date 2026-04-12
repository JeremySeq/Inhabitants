package com.jeremyseq.inhabitants.particles;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Inhabitants.MODID);

    public static final RegistryObject<SimpleParticleType> IMPALER_SCREAM =
            PARTICLES.register("impaler_scream",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WARPED_CLAM_PEARL_AMBIENCE =
            PARTICLES.register("warped_clam_pearl_ambience",
                    () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WARPED_CLAM_PEARL_INDICATOR =
            PARTICLES.register("warped_clam_pearl_indicator",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> IMPALER_SPIKE_RAISE =
            PARTICLES.register("impaler_spike_raise",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> ROAR_EFFECT =
            PARTICLES.register("roar_effect",
                    () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SONIC_WAVE =
            PARTICLES.register("sonic_wave",
                    () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SHOCKWAVE =
            PARTICLES.register("shockwave",
                    () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> ABRACADABRA =
            PARTICLES.register("abracadabra",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> IMPALER_HEAD_ATTACK =
            PARTICLES.register("impaler_head_attack",
                    () -> new SimpleParticleType(false));

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(ModParticles.IMPALER_SCREAM.get(), ScreamParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.WARPED_CLAM_PEARL_AMBIENCE.get(), WarpedClamPearlAmbienceParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.WARPED_CLAM_PEARL_INDICATOR.get(), WarpedClamPearlIndicatorParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.IMPALER_SPIKE_RAISE.get(), ImpalerSpikeRaiseParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.ROAR_EFFECT.get(), RoarEffectParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.SONIC_WAVE.get(), SonicWaveParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.SHOCKWAVE.get(), ShockwaveParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.ABRACADABRA.get(), AbracadabraParticle.Factory::new);
    }
}