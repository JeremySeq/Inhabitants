package com.jeremyseq.inhabitants;

import com.jeremyseq.inhabitants.entities.impaler.ScreamParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Inhabitants.MODID);

    public static final RegistryObject<SimpleParticleType> IMPALER_SCREAM =
            PARTICLES.register("impaler_scream",
                    () -> new SimpleParticleType(false));

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(ModParticles.IMPALER_SCREAM.get(), ScreamParticle.Factory::new);
    }
}