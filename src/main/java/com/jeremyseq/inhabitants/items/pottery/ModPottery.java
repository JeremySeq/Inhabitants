package com.jeremyseq.inhabitants.items.pottery;

import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class ModPottery {

    public static final SherdEntry SHERD_SPOTLIGHT = SherdEntry.of("spotlight");

    public static void registerPatterns(BiConsumer<ResourceLocation, String> registry) {
        SherdEntry.SHERDS.values().forEach(entry -> registry.accept(entry.sherdPattern.location(), entry.sherdPattern.location().getPath()));
    }
}