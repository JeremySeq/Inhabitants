package com.jeremyseq.inhabitants.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class WarpedClamPearlAmbienceParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private static final int TOTAL_FRAMES = 2;
    private static final int ANIMATION_DURATION = 20;

    protected WarpedClamPearlAmbienceParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        hasPhysics = false;
        lifetime = 80;
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
        this.quadSize *= .5f;
        this.sprites = sprites;
        setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240 | (240 << 16);
    }

    @Override public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 1.0F - (age / (float) lifetime);

        // sprite animation
        float animProgress = ((float) age % (float) ANIMATION_DURATION) / (float) ANIMATION_DURATION;
        int frame = (int)(animProgress * TOTAL_FRAMES);

        if (frame >= TOTAL_FRAMES) {
            frame = 0;
        }

        // +1 because 1st frame is at index 1 not 0
        this.setSprite(sprites.get(frame+1, TOTAL_FRAMES));
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
            return new WarpedClamPearlAmbienceParticle(level, x, y, z, mx, my, mz, sprites);
        }
    }
}
