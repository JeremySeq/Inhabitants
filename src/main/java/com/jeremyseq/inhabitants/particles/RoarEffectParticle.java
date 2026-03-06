package com.jeremyseq.inhabitants.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class RoarEffectParticle extends TextureSheetParticle {
    private final float maxRadius;
    private final float spinSpeed;

    protected RoarEffectParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.friction = 1.0F;
        this.gravity = 0.0F;
        
        this.quadSize = 0.08f;
        this.maxRadius = 0.3f;
        this.lifetime = 10;
        
        this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
        this.oRoll = this.roll;
        this.spinSpeed = (this.random.nextFloat() - 0.5f) * 0.05f;
        
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = 0.8f;
        
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        
        this.roll += this.spinSpeed;
        
        float progress = (float) this.age / (float) this.lifetime;
        
        this.quadSize = 0.08f + (this.maxRadius * progress);
        this.alpha = 0.8f * (1.0f - progress);

        if (this.alpha < 0.01f) {
            this.remove();
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; 
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType typeIn, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RoarEffectParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}
