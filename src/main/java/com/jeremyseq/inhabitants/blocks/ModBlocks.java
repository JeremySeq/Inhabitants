package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final RegistryObject<Block> CHISELED_ICE = BLOCKS.register("chiseled_ice",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).friction(0.989F)
                    .randomTicks().strength(2.8F).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistryObject<Item> CHISELED_ICE_ITEM = registerBlockItem("chiseled_ice", CHISELED_ICE);

    public static final RegistryObject<Block> ICE_BRICKS = BLOCKS.register("ice_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).friction(0.989F)
                    .randomTicks().strength(2.8F).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistryObject<Item> ICE_BRICKS_ITEM = registerBlockItem("ice_bricks", ICE_BRICKS);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}