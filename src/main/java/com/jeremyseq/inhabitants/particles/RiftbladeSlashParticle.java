package com.jeremyseq.inhabitants.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RiftbladeSlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private static final int TOTAL_FRAMES = 8;
    private static final int ANIMATION_DURATION = 10;

    private final double yawRad;

    protected RiftbladeSlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, double pitchRad, double yawRad, double scale) {
        super(level, x, y, z);
        this.yawRad = yawRad + Math.PI / 2;
        this.sprites = sprites;
        setSpriteFromAge(sprites);
        hasPhysics = false;
        lifetime = 10;
        quadSize = (float) scale;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();

        // sprite animation
        float animProgress = ((float) age % (float) ANIMATION_DURATION) / (float) ANIMATION_DURATION;
        int frame = (int)(animProgress * TOTAL_FRAMES);

        if (frame >= TOTAL_FRAMES) {
            frame = 0;
        }

        // +1 because 1st frame is at index 1 not 0
        this.setSprite(sprites.get(frame+1, TOTAL_FRAMES));
    }

    @Override
    public int getLightColor(float partialTick) {
        return 220 | (220 << 16);
    }

    @Override
    public void render(VertexConsumer buf, Camera cam, float pt) {
        Vec3 camPos = cam.getPosition();
        float cx = (float)(x - camPos.x());
        float cy = (float)(y - camPos.y()) - 0.1F;
        float cz = (float)(z - camPos.z());

        float cosY = Mth.cos((float) this.yawRad);
        float sinY = Mth.sin((float) this.yawRad);

        float half = quadSize * 0.5F;

        // quad corners before rotation, centered on (0,0) in XZ plane
        float x0 = -half, z0 = -half;
        float x1 = -half, z1 = half;
        float x2 = half,  z2 = half;
        float x3 = half,  z3 = -half;

        // rotate each corner around Y axis by yawRad
        float yx0 = x0 * cosY - z0 * sinY;
        float yz0 = x0 * sinY + z0 * cosY;

        float yx1 = x1 * cosY - z1 * sinY;
        float yz1 = x1 * sinY + z1 * cosY;

        float yx2 = x2 * cosY - z2 * sinY;
        float yz2 = x2 * sinY + z2 * cosY;

        float yx3 = x3 * cosY - z3 * sinY;
        float yz3 = x3 * sinY + z3 * cosY;

        int light = getLightColor(pt);
        float u0 = getU0(), v0 = getV0();
        float u1 = getU1(), v1 = getV1();
        float a = alpha;

        // send vertices to buffer
        buf.vertex(cx + yx0, cy, cz + yz0).uv(u0, v0).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx1, cy, cz + yz1).uv(u0, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx2, cy, cz + yz2).uv(u1, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx3, cy, cz + yz3).uv(u1, v0).color(1, 1, 1, a).uv2(light).endVertex();

        // flipped quad for back side
        buf.vertex(cx + yx3, cy, cz + yz3).uv(u1, v0).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx2, cy, cz + yz2).uv(u1, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx1, cy, cz + yz1).uv(u0, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + yx0, cy, cz + yz0).uv(u0, v0).color(1, 1, 1, a).uv2(light).endVertex();
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /**
         * @param mx pitch angle in radians (unused)
         * @param my yaw angle in radians
         * @param mz scale
         */
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
            return new RiftbladeSlashParticle(level, x, y, z, sprites, mx, my, mz);
        }
    }
}
