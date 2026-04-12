package com.jeremyseq.inhabitants;

import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.debug.DebugCommands;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeDispenserBehavior;
import com.jeremyseq.inhabitants.items.*;
import com.jeremyseq.inhabitants.loot_modifiers.ModLootModifiers;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.paintings.ModPaintings;
import com.jeremyseq.inhabitants.particles.ModParticles;
import com.jeremyseq.inhabitants.potions.ModPotions;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.gui.cauldron.CauldronScreen;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;
import com.jeremyseq.inhabitants.animation.FPVAnimationManager;
import com.jeremyseq.inhabitants.gui.*;
import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.entities.bogre.render.BogreRenderer;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronRenderer;
import com.jeremyseq.inhabitants.entities.impaler.ImpalerRenderer;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeRenderer;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamRenderer;
import com.jeremyseq.inhabitants.entities.javelin.JavelinRenderer;
import com.jeremyseq.inhabitants.blocks.impaler_head.ImpalerHeadRenderer;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.client.event.*;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

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
        ModEnchantments.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModEntities.REGISTRY.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        ModNetworking.register();
        ModPaintings.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(DebugCommands.class);

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ModItems.IMPALER_SPIKE.get(),
            new ImpalerSpikeDispenserBehavior());
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(new BogreRecipeManager());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenuTypes.CAULDRON_MENU.get(),
                CauldronScreen::new);
            });

            ItemProperties.register(Items.CROSSBOW, ResourceLocation.fromNamespaceAndPath(MODID,"spike_loaded"), (stack, level, entity, seed) -> {
                if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
                    if (CrossbowItem.containsChargedProjectile(stack, ModItems.IMPALER_SPIKE.get())) {
                        return 1.0F;
                    }
                }
                return 0.0F;
            });

            ItemProperties.register(ModItems.JAVELIN.get(),
                ResourceLocation.fromNamespaceAndPath(MODID, "aiming"), (stack, level, entity, seed) -> {
                    return entity != null &&
                    entity.isUsingItem() &&
                    entity.getUseItem() == stack ? 1.0F : 0.0F;
                });

            ItemProperties.register(ModItems.SPIKE_DRILL.get(),
                ResourceLocation.fromNamespaceAndPath(MODID, "drilling"), (stack, level, entity, seed) -> {
                    return entity != null &&
                    entity.isUsingItem() &&
                    entity.getUseItem() == stack ? 1.0F : 0.0F;
                });

            ItemProperties.register(ModItems.SPIKE_DRILL.get(),
                ResourceLocation.fromNamespaceAndPath(MODID, "drill_temperature"), (stack, level, entity, seed) -> {
                    int temperature = SpikeDrillItem.getTemperature(stack);
                    float ratio = (float) temperature / SpikeDrillItem.getTemperatureMax(stack);
                    
                    if (ratio >= 1.0F)  return 1.0F;
                    if (ratio >= 0.75F) return 0.75F;
                    if (ratio >= 0.50F) return 0.50F;
                    if (ratio >= 0.25F) return 0.25F;
                    return 0.0F;
                });
        }

        @SubscribeEvent
        public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
            event.register(new ModelResourceLocation(Inhabitants.MODID, "javelin_gui", "inventory"));
        }

        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(FPVAnimationManager.INSTANCE);
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("drill_heat", DrillHeatOverlay.HUD_DRILL_HEAT);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.BOGRE.get(), BogreRenderer::new);
            event.registerEntityRenderer(ModEntities.BOGRE_CAULDRON.get(), BogreCauldronRenderer::new);
            event.registerEntityRenderer(ModEntities.WARPED_CLAM.get(), WarpedClamRenderer::new);
            event.registerEntityRenderer(ModEntities.IMPALER.get(), ImpalerRenderer::new);
            event.registerEntityRenderer(ModEntities.IMPALER_SPIKE_PROJECTILE.get(), ImpalerSpikeRenderer::new);
            event.registerEntityRenderer(ModEntities.JAVELIN.get(), JavelinRenderer::new);

            event.registerBlockEntityRenderer(ModBlockEntities.IMPALER_HEAD_BLOCK_ENTITY.get(),
                    context -> new ImpalerHeadRenderer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BogreRecipeManager.sendBogreRecipesToPlayer(serverPlayer);
        }
    }
}
