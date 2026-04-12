package com.jeremyseq.inhabitants.gui;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import com.mojang.blaze3d.systems.RenderSystem;

public class DrillHeatOverlay {
    public static final ResourceLocation GREEN_BULB =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/gui/drill_heat/green.png");
    public static final ResourceLocation YELLOW_BULB =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/gui/drill_heat/yellow.png");
    public static final ResourceLocation RED_BULB =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/gui/drill_heat/red.png");

    public static final IGuiOverlay HUD_DRILL_HEAT = (
        gui,
        guiGraphics,
        partialTick,
        width,
        height
    ) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof SpikeDrillItem) {
            int temperature = SpikeDrillItem.getTemperature(stack);
            int maxTemperature = SpikeDrillItem.getTemperatureMax(stack);
            
            if (temperature >= maxTemperature * 0.25) {
                int selected = player.getInventory().selected;
                
                int x = (width / 2) - 91 + (selected * 20) + 2;
                int y = height - 20;

                ResourceLocation tex = GREEN_BULB;
                if (temperature >= maxTemperature * 0.75) tex = RED_BULB;
                else if (temperature >= maxTemperature * 0.50) tex = YELLOW_BULB;
                
                if (temperature >= maxTemperature) {
                    if (player.tickCount % 10 < 5) {
                        return;
                    }
                }

                RenderSystem.setShaderTexture(0, tex);
                guiGraphics.blit(
                    tex,
                    x, y,
                    6, 6,
                    0.0f, 0.0f,
                    16, 16,
                    16, 16
                );
            }
        }
    };
}
