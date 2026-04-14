package com.jeremyseq.inhabitants.potions;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Inhabitants.MODID);

    public static final RegistryObject<Potion> PANIC_POTION =
            POTIONS.register("panic", () ->
                    new Potion(new MobEffectInstance(ModEffects.PANIC.get(), 300)));

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }

    public static void registerBrewingRecipes() {
        BrewingRecipeRegistry.addRecipe(new SimpleBrewingRecipe(
                Items.POTION, Potions.AWKWARD, ModItems.DREAD_CLOTH.get(), ModPotions.PANIC_POTION.get()
        ));
        BrewingRecipeRegistry.addRecipe(new SimpleBrewingRecipe(
                Items.SPLASH_POTION, Potions.AWKWARD, ModItems.DREAD_CLOTH.get(), ModPotions.PANIC_POTION.get()
        ));
        BrewingRecipeRegistry.addRecipe(new SimpleBrewingRecipe(
                Items.LINGERING_POTION, Potions.AWKWARD, ModItems.DREAD_CLOTH.get(), ModPotions.PANIC_POTION.get()
        ));
    }
}