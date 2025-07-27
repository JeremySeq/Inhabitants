package com.jeremyseq.inhabitants.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class BubbleRenderer {
    public static void renderBubble(PoseStack poseStack, int x, int y, int width, int height, ResourceLocation texture, int padding) {
        var consumer = Tesselator.getInstance().getBuilder();
        consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(3.0F, 3.0F);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int totalWidth = width + padding * 2;
        int totalHeight = height + padding * 2;

        int left = x - padding;
        int top = y - padding;
        int right = left + totalWidth;
        int bottom = top + totalHeight;
        int cornerSize = 5;

        // LEFT
        blit(poseStack, consumer, left - cornerSize, top - cornerSize, cornerSize, cornerSize, 0.0F, 0.0F, 5, 5, 32, 32); // TOP
        blit(poseStack, consumer, left - cornerSize, top, cornerSize, height + padding * 2, 0.0F, 6.0F, 5, 1, 32, 32); // MID
        blit(poseStack, consumer, left - cornerSize, bottom, cornerSize, cornerSize, 0.0F, 8.0F, 5, 5, 32, 32); // BOTTOM

        // MIDDLE
        blit(poseStack, consumer, left, top - cornerSize, totalWidth, cornerSize, 6.0F, 0.0F, 5, 5, 32, 32); // TOP
        blit(poseStack, consumer, left, top, totalWidth, height + padding * 2, 6.0F, 6.0F, 5, 1, 32, 32); // MID
        blit(poseStack, consumer, left, bottom, totalWidth, cornerSize, 6.0F, 8.0F, 5, 5, 32, 32); // BOTTOM

        // RIGHT
        blit(poseStack, consumer, right, top - cornerSize, cornerSize, cornerSize, 12.0F, 0.0F, 5, 5, 32, 32); // TOP
        blit(poseStack, consumer, right, top, cornerSize, height + padding * 2, 12.0F, 6.0F, 5, 1, 32, 32); // MID
        blit(poseStack, consumer, right, bottom, cornerSize, cornerSize, 12.0F, 8.0F, 5, 5, 32, 32); // BOTTOM

        BufferUploader.drawWithShader(consumer.end());
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.disableBlend();
    }

    private static void blit(PoseStack poseStack, VertexConsumer consumer, int x, int y, int width, int height,
                             float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        var matrix = poseStack.last().pose();

        var x2 = x + width;
        var y2 = y + height;
        var minU = uOffset / textureWidth;
        var maxU = (uOffset + uWidth) / textureWidth;
        var minV = vOffset / textureHeight;
        var maxV = (vOffset + vHeight) / textureHeight;

        consumer.vertex(matrix, (float) x, (float) y, 0f)
                .uv(minU, minV)
                .color(-1)
                .endVertex();

        consumer.vertex(matrix, (float) x, (float) y2, 0f)
                .uv(minU, maxV)
                .color(-1)
                .endVertex();

        consumer.vertex(matrix, (float) x2, (float) y2, 0f)
                .uv(maxU, maxV)
                .color(-1)
                .endVertex();

        consumer.vertex(matrix, (float) x2, (float) y, 0f)
                .uv(maxU, minV)
                .color(-1)
                .endVertex();
    }
}
