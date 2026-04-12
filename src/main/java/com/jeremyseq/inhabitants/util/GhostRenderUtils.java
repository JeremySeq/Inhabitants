package com.jeremyseq.inhabitants.util;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix4f;
import org.joml.Matrix3f;

public class GhostRenderUtils {
    
    public static class GhostBufferSource implements MultiBufferSource {
        private final MultiBufferSource original;
        private final float alpha;

        public GhostBufferSource(MultiBufferSource original, float alpha) {
            this.original = original;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType pRenderType) {
            return new GhostVertexConsumer(original.getBuffer(pRenderType), alpha);
        }
    }
    
    public static class GhostVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        public GhostVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, (int)(a * alpha)); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }

        @Override public VertexConsumer vertex(double x, double y, double z) { return delegate.vertex(x, y, z); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return delegate.color(r, g, b, (int)(a * alpha)); }
        @Override public VertexConsumer uv(float u, float v) { return delegate.uv(u, v); }
        @Override public VertexConsumer overlayCoords(int u, int v) { return delegate.overlayCoords(u, v); }
        @Override public VertexConsumer uv2(int u, int v) { return delegate.uv2(u, v); }
        @Override public VertexConsumer normal(float x, float y, float z) { return delegate.normal(x, y, z); }
        @Override public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) { return delegate.vertex(matrix, x, y, z); }
        @Override public VertexConsumer normal(Matrix3f matrix, float x, float y, float z) { return delegate.normal(matrix, x, y, z); }
        @Override public VertexConsumer color(float r, float g, float b, float a) { return delegate.color(r, g, b, a * alpha); }
        @Override public VertexConsumer uv2(int light) { return delegate.uv2(light); }
        @Override public VertexConsumer overlayCoords(int overlay) { return delegate.overlayCoords(overlay); }
    }
}
