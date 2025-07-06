package com.jeremyseq.inhabitants;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Inhabitants.MODID);

    public static final RegistryObject<SimpleParticleType> IMPALER_SCREAM =
            PARTICLES.register("impaler_scream",
                    () -> new SimpleParticleType(false));
}