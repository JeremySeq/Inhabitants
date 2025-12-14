package com.jeremyseq.inhabitants.entities.apex;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class ApexStunParticle extends TextureSheetParticle {

    protected ApexStunParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        hasPhysics = false;
        lifetime = ApexEntity.STUN_PARTICLE_FREQUENCY;
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
        this.quadSize *= 0.5f;
        pickSprite(sprites);
//        setSprite(sprites.get(level.random));
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240 | (240 << 16);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
            return new ApexStunParticle(level, x, y, z, mx, my, mz, sprites);
        }
    }
}
