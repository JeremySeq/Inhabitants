package com.jeremyseq.inhabitants;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeDispenserBehavior;
import com.jeremyseq.inhabitants.items.ModCreativeModeTabs;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.potions.ModPotions;
import com.jeremyseq.inhabitants.potions.SimpleBrewingRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Inhabitants.MODID)
public class Inhabitants
{
    public static final String MODID = "inhabitants";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Inhabitants(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModEntities.REGISTRY.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                BrewingRecipeRegistry.addRecipe(new SimpleBrewingRecipe(
                        Items.POTION, Potions.AWKWARD, ModItems.RAW_ABYSSFISH.get(), ModPotions.ADAPTATION_POTION.get()
                ));
                BrewingRecipeRegistry.addRecipe(new SimpleBrewingRecipe(
                        Items.POTION, ModPotions.ADAPTATION_POTION.get(), Items.REDSTONE, ModPotions.LONG_ADAPTATION_POTION.get()
                ));
                DispenserBlock.registerBehavior(ModItems.IMPALER_SPIKE.get(), new ImpalerSpikeDispenserBehavior());
            });
        }
    }
}
