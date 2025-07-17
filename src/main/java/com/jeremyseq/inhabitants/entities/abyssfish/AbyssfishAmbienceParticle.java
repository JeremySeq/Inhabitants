package com.jeremyseq.inhabitants.entities.abyssfish;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class AbyssfishAmbienceParticle extends TextureSheetParticle {
    protected AbyssfishAmbienceParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        pickSprite(sprites);
        hasPhysics = false;
        lifetime = 40;
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
        this.quadSize *= .5f;
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240 | (240 << 16);
    }

    @Override public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick()
    {
        super.tick();
        this.alpha = 1.0F - (age / (float) lifetime);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
            return new AbyssfishAmbienceParticle(level, x, y, z, mx, my, mz, sprites);
        }
    }
}
