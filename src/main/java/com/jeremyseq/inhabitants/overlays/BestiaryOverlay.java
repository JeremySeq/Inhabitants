package com.jeremyseq.inhabitants.overlays;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BestiaryOverlay {
    private static final List<String> lines = new ArrayList<>();
    private static boolean show = false;
    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/gui/speech_bubble.png");

    public static void showText(List<String> newLines) {
        lines.clear();
        lines.addAll(newLines);
        show = true;
    }

    public static void hide() {
        show = false;
        lines.clear();
    }

    public static final IGuiOverlay BESTIARY_OVERLAY = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        if (!show || lines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font fontRenderer = mc.font;

        int textWidth = 0;

        for (String line : lines)
            textWidth = Math.max(textWidth, fontRenderer.width(line));

        int lineHeight = fontRenderer.lineHeight;
        int padding = 1;

        int bubbleWidth = textWidth + padding * 2;
        int bubbleHeight = lines.size() * lineHeight + padding * 2;

        // Translate to center the entire bubble
        int bubbleX = (screenWidth - bubbleWidth) / 2;
        int bubbleY = (screenHeight - bubbleHeight) / 2;

        poseStack.pose().pushPose();
        poseStack.pose().translate(bubbleX, bubbleY, 0);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        BubbleRenderer.renderBubble(poseStack.pose(), 0, 0, bubbleWidth, bubbleHeight, BUBBLE_TEXTURE, padding);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            fontRenderer.drawInBatch(
                    line,
                    (bubbleWidth - fontRenderer.width(line)) / 2f,
                    padding + i * lineHeight,
                    Color.BLACK.getRGB(),
                    false,
                    poseStack.pose().last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880 // light level
            );
        }

        bufferSource.endBatch();
        poseStack.pose().popPose();
    };
}