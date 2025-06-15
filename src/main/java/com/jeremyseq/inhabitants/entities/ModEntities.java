package com.jeremyseq.inhabitants.entities;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.BogreRenderer;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronRenderer;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamEntity;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Inhabitants.MODID);

    public static final RegistryObject<EntityType<BogreEntity>> BOGRE =
            REGISTRY.register("bogre",
                    () -> EntityType.Builder.of(BogreEntity::new, MobCategory.MONSTER)
                            .sized(2.7f, 3.9f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "bogre").toString()));

    public static final RegistryObject<EntityType<BogreCauldronEntity>> BOGRE_CAULDRON =
            REGISTRY.register("bogre_cauldron",
                    () -> EntityType.Builder.of(BogreCauldronEntity::new, MobCategory.MONSTER)
                            .sized(2f, 2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "bogre_cauldron").toString()));

    public static final RegistryObject<EntityType<WarpedClamEntity>> WARPED_CLAM =
            REGISTRY.register("warped_clam",
                    () -> EntityType.Builder.of(WarpedClamEntity::new, MobCategory.CREATURE)
                            .sized(1.5f, .4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "warped_clam").toString()));

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BOGRE.get(), BogreEntity.setAttributes());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.BOGRE.get(), BogreRenderer::new);
        EntityRenderers.register(ModEntities.BOGRE_CAULDRON.get(), BogreCauldronRenderer::new);
        EntityRenderers.register(ModEntities.WARPED_CLAM.get(), WarpedClamRenderer::new);
    }
}
