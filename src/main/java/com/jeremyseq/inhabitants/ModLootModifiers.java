package com.jeremyseq.inhabitants;

import com.jeremyseq.inhabitants.entities.abyssfish.AbyssfishLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Inhabitants.MODID);

    public static final RegistryObject<Codec<AbyssfishLootModifier>> ABYSSFISH = LOOT_MODIFIERS.register("abyssfish_loot_modifier", () -> AbyssfishLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }
}
