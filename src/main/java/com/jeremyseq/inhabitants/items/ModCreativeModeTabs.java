package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.awt.*;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Inhabitants.MODID);

    public static final RegistryObject<CreativeModeTab> INHABITANTS_TAB = CREATIVE_MODE_TABS.register("inhabitants_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.DEAD_BOGRE_ICON.get()))
                    .withLabelColor(new Color(0x2C7866).getRGB())
                    .title(Component.translatable("creativetab.inhabitants_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        ItemStack gazer_pod = new ItemStack(ModItems.GAZER_POD.get());
                        gazer_pod.getOrCreateTag().putBoolean("HasGazer", true);

                        pOutput.accept(ModItems.BOGRE_SPAWN_EGG.get());
                        pOutput.accept(ModItems.FISH_SNOT_CHOWDER.get());
                        pOutput.accept(ModItems.BRACER_OF_MIGHT.get());
                        pOutput.accept(ModItems.GIANT_BONE.get());
                        pOutput.accept(ModItems.WARPED_CLAM_ITEM.get());
                        pOutput.accept(ModItems.ABYSSFISH_SPAWN_EGG.get());
                        pOutput.accept(ModItems.RAW_ABYSSFISH.get());
                        pOutput.accept(ModItems.COOKED_ABYSSFISH.get());
                        pOutput.accept(ModItems.ABYSSFISH_BUCKET.get());
                        pOutput.accept(ModItems.IMPALER_SPAWN_EGG.get());
                        pOutput.accept(ModItems.IMPALER_SPIKE.get());
                        pOutput.accept(ModItems.ANCIENT_FLUTE.get());
                        pOutput.accept(ModItems.ANCIENT_FLUTE.get());
                        pOutput.accept(ModItems.WISHFISH_SPAWN_EGG.get());
                        pOutput.accept(ModItems.WISHFISH.get());
                        pOutput.accept(ModItems.WISHFISH_BUCKET.get());
                        pOutput.accept(gazer_pod);
                        pOutput.accept(ModItems.GAZER_SPAWN_EGG.get());
                        pOutput.accept(ModItems.APEX_SPAWN_EGG.get());
                        pOutput.accept(ModItems.APEX_HORN.get());
                        pOutput.accept(ModBlocks.ICE_BRICKS_ITEM.get());
                        pOutput.accept(ModBlocks.CHISELED_ICE_ITEM.get());
                        pOutput.accept(ModBlocks.ICE_BRICK_STAIRS_ITEM.get());
                        pOutput.accept(ModBlocks.ICE_BRICK_SLAB_ITEM.get());
                        pOutput.accept(ModBlocks.ICE_BRICK_WALL_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_SAPLING_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_LOG_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_WOOD_ITEM.get());
                        pOutput.accept(ModBlocks.STRIPPED_GLACIERPINE_LOG_ITEM.get());
                        pOutput.accept(ModBlocks.STRIPPED_GLACIERPINE_WOOD_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_PLANKS_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_STAIRS_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_SLAB_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_FENCE_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_FENCE_GATE_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_PRESSURE_PLATE_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_BUTTON_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_DOOR_ITEM.get());
                        pOutput.accept(ModBlocks.GLACIERPINE_TRAPDOOR_ITEM.get());
                        pOutput.accept(ModBlocks.WATERBERRY_BUSH_ITEM.get());
                        pOutput.accept(ModItems.WATERBERRY.get());
                        pOutput.accept(ModItems.CATCHER_SPAWN_EGG.get());
                        pOutput.accept(ModItems.CHITIN.get());
                        pOutput.accept(ModItems.CHITIN_CHESTPLATE.get());
                        pOutput.accept(ModItems.CHITIN_UPGRADE_SMITHING_TEMPLATE.get());
                        pOutput.accept(ModItems.CHITIN_SHIELD.get());
                    })
                    .build());

    public static void addItemsToCreativeModeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.ICE_BRICKS_ITEM.get());
            event.accept(ModBlocks.CHISELED_ICE_ITEM.get());
            event.accept(ModBlocks.ICE_BRICK_STAIRS_ITEM.get());
            event.accept(ModBlocks.ICE_BRICK_SLAB_ITEM.get());
            event.accept(ModBlocks.ICE_BRICK_WALL_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_LOG_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_WOOD_ITEM.get());
            event.accept(ModBlocks.STRIPPED_GLACIERPINE_LOG_ITEM.get());
            event.accept(ModBlocks.STRIPPED_GLACIERPINE_WOOD_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_PLANKS_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_STAIRS_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_SLAB_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_FENCE_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_FENCE_GATE_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_PRESSURE_PLATE_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_BUTTON_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_DOOR_ITEM.get());
            event.accept(ModBlocks.GLACIERPINE_TRAPDOOR_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.GLACIERPINE_LOG_ITEM.get());
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
        eventBus.addListener(ModCreativeModeTabs::addItemsToCreativeModeTabs);
    }
}