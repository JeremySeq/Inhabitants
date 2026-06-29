package com.jeremyseq.inhabitants.compat;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.jeremyseq.inhabitants.Inhabitants.MODID;

public class ModBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>>
            BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(
                    ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    MODID);

    public static final RegistryObject<Codec<ImpalerForlornHollowsBiomeModifier>>
            IMPALER_FORLORN_HOLLOWS =
            BIOME_MODIFIER_SERIALIZERS.register(
                    "impaler_forlorn_hollows",
                    () -> Codec.unit(ImpalerForlornHollowsBiomeModifier::new));

    public static void register(IEventBus eventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
