package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
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

    public static final RegistryObject<Block> GLACIERPINE_LOG = BLOCKS.register("glacierpine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Item> GLACIERPINE_LOG_ITEM = registerBlockItem("glacierpine_log", GLACIERPINE_LOG);

    public static final RegistryObject<Block> GLACIERPINE_WOOD = BLOCKS.register("glacierpine_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Item> GLACIERPINE_WOOD_ITEM = registerBlockItem("glacierpine_wood", GLACIERPINE_WOOD);

    public static final RegistryObject<Block> STRIPPED_GLACIERPINE_LOG = BLOCKS.register("stripped_glacierpine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Item> STRIPPED_GLACIERPINE_LOG_ITEM = registerBlockItem("stripped_glacierpine_log", STRIPPED_GLACIERPINE_LOG);

    public static final RegistryObject<Block> STRIPPED_GLACIERPINE_WOOD = BLOCKS.register("stripped_glacierpine_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Item> STRIPPED_GLACIERPINE_WOOD_ITEM = registerBlockItem("stripped_glacierpine_wood", STRIPPED_GLACIERPINE_WOOD);

    public static final RegistryObject<Block> GLACIERPINE_PLANKS = BLOCKS.register("glacierpine_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final RegistryObject<Item> GLACIERPINE_PLANKS_ITEM = registerBlockItem("glacierpine_planks", GLACIERPINE_PLANKS);

    public static final RegistryObject<Block> GLACIERPINE_STAIRS = BLOCKS.register("glacierpine_stairs",
            () -> new StairBlock(() -> GLACIERPINE_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())));

    public static final RegistryObject<Item> GLACIERPINE_STAIRS_ITEM =
            registerBlockItem("glacierpine_stairs", GLACIERPINE_STAIRS);

    public static final RegistryObject<Block> GLACIERPINE_SLAB = BLOCKS.register("glacierpine_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())));

    public static final RegistryObject<Item> GLACIERPINE_SLAB_ITEM =
            registerBlockItem("glacierpine_slab", GLACIERPINE_SLAB);

    public static final RegistryObject<Block> GLACIERPINE_FENCE = BLOCKS.register("glacierpine_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())));

    public static final RegistryObject<Item> GLACIERPINE_FENCE_ITEM =
            registerBlockItem("glacierpine_fence", GLACIERPINE_FENCE);

    public static final RegistryObject<Block> GLACIERPINE_FENCE_GATE = BLOCKS.register("glacierpine_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get()),
                    WoodType.OAK));

    public static final RegistryObject<Item> GLACIERPINE_FENCE_GATE_ITEM =
            registerBlockItem("glacierpine_fence_gate", GLACIERPINE_FENCE_GATE);

    public static final RegistryObject<Block> GLACIERPINE_BUTTON = BLOCKS.register("glacierpine_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())
                    .noCollission().strength(0.5F), net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, true));

    public static final RegistryObject<Item> GLACIERPINE_BUTTON_ITEM =
            registerBlockItem("glacierpine_button", GLACIERPINE_BUTTON);

    public static final RegistryObject<Block> GLACIERPINE_PRESSURE_PLATE = BLOCKS.register("glacierpine_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get()),
                    net.minecraft.world.level.block.state.properties.BlockSetType.OAK));

    public static final RegistryObject<Item> GLACIERPINE_PRESSURE_PLATE_ITEM =
            registerBlockItem("glacierpine_pressure_plate", GLACIERPINE_PRESSURE_PLATE);

//    public static final RegistryObject<Block> GLACIERPINE_DOOR = BLOCKS.register("glacierpine_door",
//            () -> new DoorBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())
//                    .noOcclusion(), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
//
//    public static final RegistryObject<Item> GLACIERPINE_DOOR_ITEM =
//            registerBlockItem("glacierpine_door", GLACIERPINE_DOOR);
//
//    public static final RegistryObject<Block> GLACIERPINE_TRAPDOOR = BLOCKS.register("glacierpine_trapdoor",
//            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(GLACIERPINE_PLANKS.get())
//                    .noOcclusion(), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
//
//    public static final RegistryObject<Item> GLACIERPINE_TRAPDOOR_ITEM =
//            registerBlockItem("glacierpine_trapdoor", GLACIERPINE_TRAPDOOR);

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