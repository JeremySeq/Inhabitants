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
import java.util.Objects;

public class BestiaryOverlay {
    private static final List<String> fullLines = new ArrayList<>();
    private static final List<String> visibleLines = new ArrayList<>();

    private static int lineIndex = 0;
    private static int charIndex = 0;
    private static int tickCounter = 0;
    private static int ticksPerChar = 2; // LOWER = FASTER, speed of typing effect

    private static int pauseTicks = 0;
    private static final int PAUSE_AFTER_PERIOD = 15;
    private static final int PAUSE_BETWEEN_LINES = 20;
    private static final int PAUSE_AFTER_COMMA = 10;

    private static boolean show = false;
    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/gui/speech_bubble.png");

    public static void showText(List<String> newLines) {
        fullLines.clear();
        fullLines.addAll(newLines);

        visibleLines.clear();
        visibleLines.add("");
        lineIndex = 0;
        charIndex = 0;

        show = true;
    }

    public static void hide() {
        show = false;
        fullLines.clear();

        visibleLines.clear();
    }

    public static void tick() {
        if (!show || lineIndex >= fullLines.size()) return;

        // If we are currently in a pause state, decrement and wait
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }

        tickCounter++;
        if (tickCounter >= ticksPerChar) {
            tickCounter = 0;

            String currentLine = fullLines.get(lineIndex);

            if (charIndex < currentLine.length()) {
                visibleLines.set(0, currentLine.substring(0, charIndex+1));

                // Pause after a period or other punctuation
                char currentChar = currentLine.charAt(charIndex);
                if (currentChar == '.' || currentChar == '!' || currentChar == '?' || currentChar == ';' || currentChar == ':') {
                    pauseTicks = PAUSE_AFTER_PERIOD;
                } else if (currentChar == ',' || currentChar == '-' || currentChar == '—')
                    pauseTicks = PAUSE_AFTER_COMMA;
                charIndex++;

            } else if (lineIndex + 1 < fullLines.size()) {
                lineIndex++;
                charIndex = 0;
                visibleLines.set(0, "");

                // pause between lines
                pauseTicks = PAUSE_BETWEEN_LINES;
            } else {
                BestiaryOverlay.hide();
            }
        }
    }

    public static final IGuiOverlay BESTIARY_OVERLAY = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        if (!show || fullLines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font fontRenderer = mc.font;

        List<String> linesToRender = new ArrayList<>();

        Font font = Minecraft.getInstance().font;

        // split the line into an array so it wraps
        int maxWidth = 200; // Change as needed to fit your UI
        for (String line : visibleLines) {
            linesToRender.addAll(wrapText(line, maxWidth, font));
        }

        // dont render if there is no text
        if ((linesToRender.size() == 1 && Objects.equals(linesToRender.get(0), "")) || linesToRender.isEmpty()) {
            return;
        }

        int textWidth = 0;
        for (String line : linesToRender)
            textWidth = Math.max(textWidth, fontRenderer.width(line));

        int lineHeight = fontRenderer.lineHeight;
        int padding = 1;

        int bubbleWidth = textWidth + padding * 2;
        int bubbleHeight = linesToRender.size() * lineHeight + padding * 2;

        // Translate to center the entire bubble
        int bubbleX = (screenWidth - bubbleWidth) / 2;
        int bubbleY = (screenHeight - bubbleHeight) / 2;

        poseStack.pose().pushPose();
        poseStack.pose().translate(bubbleX, bubbleY, 0);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        BubbleRenderer.renderBubble(poseStack.pose(), 0, 0, bubbleWidth, bubbleHeight, BUBBLE_TEXTURE, padding);

        for (int i = 0; i < linesToRender.size(); i++) {
            String line = linesToRender.get(i);

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

    private static List<String> wrapText(String text, int maxWidth, Font font) {
        List<String> wrappedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(testLine) > maxWidth) {
                if (!currentLine.isEmpty()) {
                    wrappedLines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            } else {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            wrappedLines.add(currentLine.toString());
        }
        return wrappedLines;
    }

}