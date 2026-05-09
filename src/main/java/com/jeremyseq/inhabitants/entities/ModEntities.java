package com.jeremyseq.inhabitants.entities;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeProjectile;
import com.jeremyseq.inhabitants.entities.impaler.ImpalerEntity;
import com.jeremyseq.inhabitants.entities.nightmare.NightmareEntity;
import com.jeremyseq.inhabitants.entities.nightmare.slash_projectile.SlashProjectile;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamEntity;
import com.jeremyseq.inhabitants.entities.javelin.JavelinEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Inhabitants.MODID);

    public static final RegistryObject<EntityType<BogreEntity>> BOGRE =
            REGISTRY.register("bogre",
                    () -> EntityType.Builder.of(BogreEntity::new, MobCategory.MONSTER)
                            .sized(1.75f, 3.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "bogre").toString()));

    public static final RegistryObject<EntityType<BogreCauldronEntity>> BOGRE_CAULDRON =
            REGISTRY.register("bogre_cauldron",
                    () -> EntityType.Builder.of(BogreCauldronEntity::new, MobCategory.MISC)
                            .sized(2f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "bogre_cauldron").toString()));

    public static final RegistryObject<EntityType<WarpedClamEntity>> WARPED_CLAM =
            REGISTRY.register("warped_clam",
                    () -> EntityType.Builder.of(WarpedClamEntity::new, MobCategory.CREATURE)
                            .sized(1.5f, .4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "warped_clam").toString()));

    public static final RegistryObject<EntityType<ImpalerEntity>> IMPALER =
            REGISTRY.register("impaler",
                    () -> EntityType.Builder.of(ImpalerEntity::new, MobCategory.MONSTER)
                            .sized(1, 2.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "impaler").toString()));

    public static final RegistryObject<EntityType<ImpalerSpikeProjectile>> IMPALER_SPIKE_PROJECTILE =
            REGISTRY.register("impaler_spike",
                    () -> EntityType.Builder.<ImpalerSpikeProjectile>of(
                                    ImpalerSpikeProjectile::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "impaler_spike").toString()));

    public static final RegistryObject<EntityType<JavelinEntity>> JAVELIN =
            REGISTRY.register("javelin",
                    () -> EntityType.Builder.<JavelinEntity>of(JavelinEntity::new, MobCategory.MISC)
                            .sized(0.75f, 0.75f)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "javelin").toString()));

    public static final RegistryObject<EntityType<NightmareEntity>> NIGHTMARE =
            REGISTRY.register("nightmare",
                    () -> EntityType.Builder.<NightmareEntity>of(NightmareEntity::new, MobCategory.MONSTER)
                            .sized(1, 2.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "nightmare").toString()));

    public static final RegistryObject<EntityType<SlashProjectile>> SLASH_PROJECTILE =
            REGISTRY.register("slash_projectile",
                    () -> EntityType.Builder.<SlashProjectile>of(
                                    SlashProjectile::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "slash_projectile").toString()));


    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BOGRE.get(), BogreEntity.setAttributes());
        event.put(ModEntities.WARPED_CLAM.get(), WarpedClamEntity.setAttributes());
        event.put(ModEntities.IMPALER.get(), ImpalerEntity.setAttributes());
        event.put(ModEntities.NIGHTMARE.get(), NightmareEntity.setAttributes());
    }
}
