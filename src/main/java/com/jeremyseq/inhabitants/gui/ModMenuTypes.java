package com.jeremyseq.inhabitants.gui;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "inhabitants");

    public static final RegistryObject<MenuType<ZingerChestMenu>> ZINGER_CHEST =
            MENU_TYPES.register("zinger_chest",
                    () -> new MenuType<>(ZingerChestMenu::new, FeatureFlags.VANILLA_SET));
}
