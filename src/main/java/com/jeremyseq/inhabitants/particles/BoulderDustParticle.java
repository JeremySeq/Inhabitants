package com.jeremyseq.inhabitants.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class BoulderDustParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private static final int TOTAL_FRAMES = 8;
    private static int ANIMATION_DURATION = 25;

    protected BoulderDustParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;

        // random lifetime
        this.lifetime = (int)(10 + random.nextFloat() * 25); // 10-35 ticks
        ANIMATION_DURATION = this.lifetime;

        // random size
        this.quadSize *= (3.5f + random.nextFloat() * 1.5f); // 3.5-5.0

        // drift
        this.xd = xSpeed;
        this.yd = ySpeed + (0.01f + random.nextFloat() * 0.01f); // slight upward drift
        this.zd = zSpeed;

        // random rotation
        this.roll = random.nextFloat() * (float)Math.PI * 2f;
        this.oRoll = roll;

        // tint variation
        float tint = 0.85f + random.nextFloat() * 0.15f;
        this.rCol = tint;
        this.gCol = tint;
        this.bCol = tint;

        setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        // make dust look softer
        return super.getLightColor(partialTick);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // frame animation
        float animProgress = age / (float) ANIMATION_DURATION;
        int frame = (int)(animProgress * TOTAL_FRAMES);

        if (frame >= TOTAL_FRAMES) {
            frame = TOTAL_FRAMES - 1;
        }

        this.setSprite(sprites.get(frame, TOTAL_FRAMES));

        // drag
        double drag = 0.86;
        this.xd *= drag;
        this.yd *= drag;
        this.zd *= drag;

        // rotation
        this.oRoll = this.roll;
        this.roll += 0.02f;

        // eased fade out
        float fadeProgress = age / (float) ANIMATION_DURATION;
        if (fadeProgress > 1f) fadeProgress = 1f;

        this.alpha = 1f - (fadeProgress * fadeProgress * fadeProgress);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double mx, double my, double mz) {
            return new BoulderDustParticle(level, x, y, z, mx, my, mz, sprites);
        }
    }
}
