package com.jeremyseq.inhabitants;

import com.jeremyseq.inhabitants.entities.abyssfish.AbyssfishAmbienceParticle;
import com.jeremyseq.inhabitants.entities.apex.ApexStunParticle;
import com.jeremyseq.inhabitants.entities.impaler.RageParticle;
import com.jeremyseq.inhabitants.entities.impaler.ScreamParticle;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamPearlAmbienceParticle;
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

    public static final RegistryObject<SimpleParticleType> IMPALER_RAGE =
            PARTICLES.register("impaler_rage",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> ABYSSFISH_AMBIENCE =
            PARTICLES.register("abyssfish_ambience",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WARPED_CLAM_PEARL_AMBIENCE =
            PARTICLES.register("warped_clam_pearl_ambience",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> APEX_STUN =
            PARTICLES.register("apex_stun",
                    () -> new SimpleParticleType(false));


    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(ModParticles.IMPALER_SCREAM.get(), ScreamParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.IMPALER_RAGE.get(), RageParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.ABYSSFISH_AMBIENCE.get(), AbyssfishAmbienceParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.WARPED_CLAM_PEARL_AMBIENCE.get(), WarpedClamPearlAmbienceParticle.Factory::new);
        evt.registerSpriteSet(ModParticles.APEX_STUN.get(), ApexStunParticle.Factory::new);
    }
}