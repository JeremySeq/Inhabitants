package com.jeremyseq.inhabitants.blocks.entity;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Inhabitants.MODID);

    public static final RegistryObject<BlockEntityType<GazerPodBlockEntity>> GAZER_POD_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("gazer_pod_block_entity",
                    () -> BlockEntityType.Builder.of(GazerPodBlockEntity::new,
                            ModBlocks.GAZER_POD_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
