package com.jeremyseq.inhabitants.paintings;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPaintings {
    public static final DeferredRegister<PaintingVariant> PAINTINGS =
            DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, Inhabitants.MODID);

    public static final RegistryObject<PaintingVariant> BONK =
            PAINTINGS.register("bonk", () -> new PaintingVariant(64, 48));

    public static final RegistryObject<PaintingVariant> MY_PRECIOUS =
            PAINTINGS.register("my_precious", () -> new PaintingVariant(48, 48));

    public static final RegistryObject<PaintingVariant> ENDERMANS_LAST_DAY =
            PAINTINGS.register("endermans_last_day", () -> new PaintingVariant(32, 32));

    public static void register(IEventBus eventBus) {
        PAINTINGS.register(eventBus);
    }
}
