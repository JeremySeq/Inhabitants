package com.jeremyseq.inhabitants.gui;

import com.jeremyseq.inhabitants.gui.cauldron.CauldronMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "inhabitants");

    public static final RegistryObject<MenuType<CauldronMenu>> CAULDRON_MENU =
        registerMenuType(CauldronMenu::new, "cauldron_menu");

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>>
        registerMenuType(IContainerFactory<T> factory, String name) {
        
        return MENU_TYPES.register(name, () -> IForgeMenuType.create(factory));
    }
}
