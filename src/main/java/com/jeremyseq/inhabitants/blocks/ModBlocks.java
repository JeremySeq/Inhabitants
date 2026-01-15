package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Inhabitants.MODID);

    public static final RegistryObject<Block> INVISIBLE_CAULDRON_BLOCK = registerBlock(
            "invisible_cauldron_block",
            () -> new InvisibleCauldronBlock(Block.Properties.of().noLootTable().strength(-1.0F, 3600000.0F).noOcclusion())
    );

    public static final RegistryObject<Block> ANCIENT_STONE_PILLAR = BLOCKS.register("ancient_stone_pillar",
            () -> new AncientStonePillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)));
    public static final RegistryObject<Item> ANCIENT_STONE_PILLAR_ITEM =
            registerBlockItem("ancient_stone_pillar", ANCIENT_STONE_PILLAR);

    public static final RegistryObject<Block> ANCIENT_STONE = BLOCKS.register("ancient_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
    public static final RegistryObject<Item> ANCIENT_STONE_ITEM =
            registerBlockItem("ancient_stone", ANCIENT_STONE);

    public static final RegistryObject<Block> SCROLL = registerBlock(
            "scroll",
            () -> new ScrollBlock(Block.Properties.copy(Blocks.BLACK_BANNER).noOcclusion().noCollission())
    );

    public static final RegistryObject<Item> SCROLL_SMALL = registerScrollItem("scroll_small", SCROLL, "item.inhabitants.scroll_small");
    public static final RegistryObject<Item> SCROLL_MEDIUM = registerScrollItem("scroll_medium", SCROLL, "item.inhabitants.scroll_medium");
    public static final RegistryObject<Item> SCROLL_LARGE = registerScrollItem("scroll_large", SCROLL, "item.inhabitants.scroll_large");


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<Item> registerScrollItem(String name, RegistryObject<T> block, String langKey) {
        return ModItems.ITEMS.register(name, () -> new ScrollItem(block.get(), new Item.Properties(), langKey));
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}