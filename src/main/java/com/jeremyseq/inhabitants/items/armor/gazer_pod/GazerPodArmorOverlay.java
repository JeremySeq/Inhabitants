package com.jeremyseq.inhabitants.items.armor.gazer_pod;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class GazerPodArmorOverlay {
    private static final ResourceLocation HELMET_OVERLAY = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/misc/gazer_pod_overlay.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HELMET.id())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && helmet.getItem() == ModItems.GAZER_POD.get() && mc.getCameraEntity() == mc.player) {
                renderHelmetOverlay(event.getGuiGraphics());
            }
        }
    }

    private static void renderHelmetOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, HELMET_OVERLAY);

        guiGraphics.blit(HELMET_OVERLAY, 0, 0, -90, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
