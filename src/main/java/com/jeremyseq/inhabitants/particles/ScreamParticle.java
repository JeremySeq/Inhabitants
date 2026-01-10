package com.jeremyseq.inhabitants.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ScreamParticle extends TextureSheetParticle {
    private final double screamSpeed;
    private final double yawRad;

    protected ScreamParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, double screamSpeed, double yawRad) {
        super(level, x, y, z);
        this.yawRad = yawRad + Math.PI / 2;
        this.screamSpeed = screamSpeed;
        pickSprite(sprites);
        hasPhysics = false;
        lifetime = 20;
        quadSize = 1.25f;
        xd = yd = zd = 0;
    }

    @Override public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 220 | (220 << 16);
    }

    @Override
    public void render(VertexConsumer buf, Camera cam, float pt) {
        Vec3 camPos = cam.getPosition();
        float cx = (float)(x - camPos.x());
        float cy = (float)(y - camPos.y()) + 0.01F;
        float cz = (float)(z - camPos.z());

        float cos = Mth.cos((float) this.yawRad);
        float sin = Mth.sin((float) this.yawRad);
        float half = quadSize * 0.5F;

        // quad corners before rotation, centered on (0,0) in XZ plane
        float x0 = -half, z0 = -half;
        float x1 = -half, z1 = half;
        float x2 = half,  z2 = half;
        float x3 = half,  z3 = -half;

        // rotate each corner around Y axis by yawRad
        float rx0 = x0 * cos - z0 * sin;
        float rz0 = x0 * sin + z0 * cos;

        float rx1 = x1 * cos - z1 * sin;
        float rz1 = x1 * sin + z1 * cos;

        float rx2 = x2 * cos - z2 * sin;
        float rz2 = x2 * sin + z2 * cos;

        float rx3 = x3 * cos - z3 * sin;
        float rz3 = x3 * sin + z3 * cos;

        int light = getLightColor(pt);
        float u0 = getU0(), v0 = getV0();
        float u1 = getU1(), v1 = getV1();
        float a = alpha;

        // send vertices to buffer
        buf.vertex(cx + rx0, cy, cz + rz0).uv(u0, v0).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + rx1, cy, cz + rz1).uv(u0, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + rx2, cy, cz + rz2).uv(u1, v1).color(1, 1, 1, a).uv2(light).endVertex();
        buf.vertex(cx + rx3, cy, cz + rz3).uv(u1, v0).color(1, 1, 1, a).uv2(light).endVertex();
    }

    @Override
    public void tick() {
        super.tick();

        // movement and scale speed per tick
        double speed = .5 * screamSpeed;
        this.quadSize += (float) speed;

        // forward vec from yaw
        double fx = Mth.cos((float) (yawRad - Math.PI / 2));
        double fz = Mth.sin((float) (yawRad - Math.PI / 2));

        // move forward
        this.x += fx * speed;
        this.z += fz * speed;

        // fade out
        this.alpha = 1.0F - (age / (float) lifetime);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /**
         * Uses `my` (ySpeed) as yaw angle to rotate the particle.
         * @param mx scream speed
         * @param my yaw angle in radians
         */
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
            return new ScreamParticle(level, x, y, z, sprites, mx, my);
        }
    }
}