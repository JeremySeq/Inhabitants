package com.jeremyseq.inhabitants.gui.components;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

public class RenderUtil {

    private static final int tooltipBgColor = 0xF0100010;
    private static final int tooltipBorderTop = 0x505000FF;
    private static final int tooltipBorderBot = 0x5028007F;

    private static final int tooltipPadding = 3;
    private static final int tooltipBorderWidth = 1;

    private static final ChatFormatting tooltipTitleColor = ChatFormatting.YELLOW;
    private static final ChatFormatting tooltipBodyColor = ChatFormatting.WHITE;
    private static final ChatFormatting tooltipHintColor = ChatFormatting.GRAY;
    private static final ChatFormatting tooltipItalic = ChatFormatting.ITALIC;
    
    public static void renderTooltipBackground(GuiGraphics guiGraphics, GuiLayout baseLayout) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, baseLayout.zIndex);
        
        GuiLayout core = baseLayout.createChild().padding(-tooltipPadding);
        drawBox(guiGraphics, core, tooltipBgColor);
        
        drawBox(guiGraphics, core.createChild()
        .height(tooltipBorderWidth)
        .margin(-tooltipBorderWidth, 0, 0, 0)
        .align("center", "top"), tooltipBgColor);

        drawBox(guiGraphics, core.createChild()
        .height(tooltipBorderWidth)
        .margin(0, 0, -tooltipBorderWidth, 0)
        .align("center", "bottom"), tooltipBgColor);

        drawBox(guiGraphics, core.createChild()
        .width(tooltipBorderWidth)
        .margin(0, 0, 0, -tooltipBorderWidth)
        .align("left", "center"), tooltipBgColor);

        drawBox(guiGraphics, core.createChild()
        .width(tooltipBorderWidth)
        .margin(0, -tooltipBorderWidth, 0, 0)
        .align("right", "center"), tooltipBgColor);
        
        drawBox(guiGraphics, core.createChild()
        .height(tooltipBorderWidth)
        .align("center", "top"), tooltipBorderTop);

        drawBox(guiGraphics, core.createChild()
        .height(tooltipBorderWidth)
        .align("center", "bottom"), tooltipBorderBot);
        
        int sideBorderHeight = core.height - tooltipBorderWidth * 2;
        drawBox(guiGraphics, core.createChild()
        .width(tooltipBorderWidth)
        .height(sideBorderHeight)
        .margin(tooltipBorderWidth, 0, 0, 0)
        .align("left", "top"), tooltipBorderTop);

        drawBox(guiGraphics, core.createChild()
        .width(tooltipBorderWidth)
        .height(sideBorderHeight)
        .margin(tooltipBorderWidth, 0, 0, 0)
        .align("right", "top"), tooltipBorderBot);
        
        guiGraphics.pose().popPose();
    }

    private static void drawBox(GuiGraphics guiGraphics, GuiLayout box, int color) {
        guiGraphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, color);
    }
    
    public static void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        guiGraphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
    
    public static void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, GuiLayout layout) {
        renderGhostItem(guiGraphics, stack, layout.x, layout.y);
    }
    
    public static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        guiGraphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }
    
    public static void blit(GuiGraphics guiGraphics, ResourceLocation texture, GuiLayout layout) {
        blit(guiGraphics, texture, layout.x, layout.y, layout.width, layout.height);
    }
    
    public static void blit(GuiGraphics guiGraphics, ModGuiTextures.SpriteRegion region, int x, int y) {
        guiGraphics.blit(region.texture,
            x, y,
            region.u, region.v,
            region.width, region.height,
            region.texWidth, region.texHeight);
    }
    
    public static void blit(GuiGraphics guiGraphics, ModGuiTextures.SpriteRegion region, GuiLayout layout) {
        guiGraphics.blit(region.texture,
            layout.x, layout.y,
            region.u, region.v,
            layout.width, layout.height,
            region.texWidth, region.texHeight);
    }
    
    public static boolean isHovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    public static MutableComponent getLabel(String key) {
        return Component.translatable(key);
    }
    
    public static Component getLabel(String key, ChatFormatting... styles) {
        return getLabel(key).withStyle(styles);
    }

    public static Component getHint(String key) {
        return getLabel(key, tooltipHintColor, tooltipItalic);
    }
    
    public static Component getTitle(String key) {
        return getLabel(key, tooltipTitleColor);
    }
    
    public static MutableComponent styleTooltipTitle(Component component) {
        return component.copy().withStyle(tooltipTitleColor);
    }
    
    public static MutableComponent style(Component component, ChatFormatting... styles) {
        return component.copy().withStyle(styles);
    }
}
