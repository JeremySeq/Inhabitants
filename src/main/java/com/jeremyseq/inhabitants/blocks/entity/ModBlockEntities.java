package com.jeremyseq.inhabitants.blocks.entity;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.blocks.impaler_head.ImpalerHeadBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.*;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Inhabitants.MODID);

    public static final RegistryObject<BlockEntityType<ImpalerHeadBlockEntity>> IMPALER_HEAD_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("impaler_head_block_entity",
                () -> BlockEntityType.Builder.of(ImpalerHeadBlockEntity::new,
                ModBlocks.IMPALER_HEAD.get(), ModBlocks.DRIPSTONE_IMPALER_HEAD.get(),
                ModBlocks.IMPALER_WALL_HEAD.get(), ModBlocks.DRIPSTONE_IMPALER_WALL_HEAD.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

}
