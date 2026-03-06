package com.jeremyseq.inhabitants.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShockwaveParticle extends TextureSheetParticle {
    private final float maxScale;

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z, int lifetime, double maxScale, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.friction = 0.96F;
        this.gravity = 0.0F;
        
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        this.quadSize = 0.1f;
        this.maxScale = (float) maxScale;
        this.lifetime = lifetime;
        this.alpha = 0.8f;
        
        this.rCol = 0.45f + (this.random.nextFloat() * 0.1f);
        this.gCol = 0.50f + (this.random.nextFloat() * 0.1f);
        this.bCol = 0.25f + (this.random.nextFloat() * 0.1f);
        
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        
        float progress = (float) this.age / (float) this.lifetime;
        
        this.quadSize = 0.1f + (this.maxScale * (float) Math.sin(progress * Math.PI / 2.0));
        
        if (progress < 0.8f) { 
            int particlesToSpawn = (int) (this.quadSize * 5.0f);
            for (int i = 0; i < particlesToSpawn; i++) {
                double angle = this.random.nextDouble() * 2.0 * Math.PI;
                double particleX = this.x + Math.cos(angle) * this.quadSize;
                double particleZ = this.z + Math.sin(angle) * this.quadSize;
                double particleY = this.y;
                
                double velocityX = Math.cos(angle) * 0.15;
                double velocityZ = Math.sin(angle) * 0.15;

                BlockPos pos = BlockPos.containing(particleX, particleY - 0.2, particleZ);
                BlockState state = this.level.getBlockState(pos);
                
                if (!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE) {
                    this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), particleX, particleY, particleZ, velocityX, 0.2D, velocityZ);
                    if (this.random.nextBoolean()) {
//                        this.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, particleX, particleY, particleZ, velocityX, 0.1D, velocityZ);
                    }
                } else {
//                    this.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, particleX, particleY, particleZ, velocityX, 0.05D, velocityZ);
                }
            }
        }
        
        if (progress > 0.5f) {
            this.alpha = 0.8f * (1.0f - ((progress - 0.5f) * 2.0f));
        }

        if (this.alpha < 0.01f) {
            this.remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float renderX = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float renderY = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float renderZ = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf rotation = new Quaternionf().rotationX((float) Math.PI / 2.0F);

        Vector3f[] vertices = new Vector3f[]{
            new Vector3f(-1.0F, -1.0F, 0.0F),
            new Vector3f(-1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float currentSize = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(rotation);
            vertex.mul(currentSize);
            vertex.add(renderX, renderY, renderZ);
        }

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        int lightColor = this.getLightColor(partialTicks);

        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .uv(maxU, maxV)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(lightColor).endVertex();

        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .uv(maxU, minV)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(lightColor).endVertex();

        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .uv(minU, minV)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(lightColor).endVertex();

        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .uv(minU, maxV)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(lightColor).endVertex();
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

        /**
         * @param xSpeed used to pass lifetime
         * @param ySpeed used to pass max scale
         * @param zSpeed unused
         */
        @Override
        public Particle createParticle(@NotNull SimpleParticleType typeIn, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ShockwaveParticle(level, x, y, z, (int) xSpeed, ySpeed, this.spriteSet);
        }
    }
}
