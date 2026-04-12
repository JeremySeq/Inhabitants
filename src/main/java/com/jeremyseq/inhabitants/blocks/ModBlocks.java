package com.jeremyseq.inhabitants.blocks;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.blocks.impaler_head.*;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

import java.util.function.Supplier;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Inhabitants.MODID);

    public static final RegistryObject<Block> INVISIBLE_CAULDRON_BLOCK = registerBlock(
            "invisible_cauldron_block",
            () -> new InvisibleCauldronBlock(Block.Properties.of().noLootTable().strength(-1.0F, 3600000.0F).noOcclusion())
    );

    public static final RegistryObject<Block> IMPALER_HEAD = registerBlock(
            "impaler_head",
            () -> new ImpalerHeadBlock(Block.Properties.of().strength(1.5F, 1.5F)
            .noOcclusion().instrument(NoteBlockInstrument.CUSTOM_HEAD))
    );

    public static final RegistryObject<Block> DRIPSTONE_IMPALER_HEAD = registerBlock(
            "dripstone_impaler_head",
            () -> new ImpalerHeadBlock(Block.Properties.of().strength(1.5F, 1.5F)
            .noOcclusion().instrument(NoteBlockInstrument.CUSTOM_HEAD))
    );

    public static final RegistryObject<Block> IMPALER_WALL_HEAD = registerBlock(
            "impaler_wall_head",
            () -> new ImpalerWallHeadBlock(Block.Properties.of().strength(1.5F, 1.5F)
            .noOcclusion().instrument(NoteBlockInstrument.CUSTOM_HEAD))
    );

    public static final RegistryObject<Block> DRIPSTONE_IMPALER_WALL_HEAD = registerBlock(
            "dripstone_impaler_wall_head",
            () -> new ImpalerWallHeadBlock(Block.Properties.of().strength(1.5F, 1.5F)
            .noOcclusion().instrument(NoteBlockInstrument.CUSTOM_HEAD))
    );

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