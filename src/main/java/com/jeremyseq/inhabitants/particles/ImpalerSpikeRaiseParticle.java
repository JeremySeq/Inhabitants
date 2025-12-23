package com.jeremyseq.inhabitants.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class ImpalerSpikeRaiseParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float outwardRoll;

    private static final int TOTAL_FRAMES = 3;
    private static final int TICKS_PER_FRAME = 3;
    private static final int LIFETIME = TOTAL_FRAMES * TICKS_PER_FRAME;

    protected ImpalerSpikeRaiseParticle(
            ClientLevel level,
            double x, double y, double z,
            double dirX, double dirY, double dirZ,
            SpriteSet sprites
    ) {
        super(level, x, y, z, 0, 0, 0);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.lifetime = LIFETIME;

        this.quadSize *= 1.8f;
        this.alpha = 1.0f;

        // CHATGPT-AHHH CODE

        /*
         * World-space outward angle (XZ plane).
         * Texture is assumed to point UP (+Y).
         */
        float outwardAngle = (float) Math.atan2(-dirX, dirZ);

        /*
         * Camera yaw (in radians).
         * Roll is applied AFTER billboarding, so we must compensate.
         */
        Minecraft mc = Minecraft.getInstance();
        float cameraYaw = (float) Math.toRadians(mc.gameRenderer.getMainCamera().getYRot());

        // final roll that visually points outward from mob center
        this.outwardRoll = outwardAngle - cameraYaw;

        this.roll = outwardRoll;
        this.oRoll = outwardRoll;

        this.setSprite(sprites.get(0, TOTAL_FRAMES));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 220 | (220 << 16);
    }

    @Override
    public void tick() {
        super.tick();

        this.xd = this.yd = this.zd = 0;

        int animAge = Math.min(this.age, TOTAL_FRAMES * TICKS_PER_FRAME - 1);
        int frame = animAge / TICKS_PER_FRAME;

        this.setSprite(this.sprites.get(frame, TOTAL_FRAMES));

        // lock orientation so camera movement doesn't affect roll
        this.oRoll = this.roll;
        this.roll = outwardRoll;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                @NotNull SimpleParticleType type,
                @NotNull ClientLevel level,
                double x, double y, double z,
                double mx, double my, double mz
        ) {
            return new ImpalerSpikeRaiseParticle(level, x, y, z, mx, my, mz, sprites);
        }
    }

    public static void spawnSpikeBurst(ClientLevel level, LivingEntity entity, int count) {
        double radius = entity.getBbWidth() * 0.85;

        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.5;
        double cz = entity.getZ();

        for (int i = 0; i < count; i++) {
            double t = (double) i / count;
            double phi = Math.acos(1 - 2 * t);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;

            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);

            double px = cx + dx * radius;
            double py = cy + dy * radius;
            double pz = cz + dz * radius;

            level.addParticle(
                    ModParticles.IMPALER_SPIKE_RAISE.get(),
                    px, py, pz,
                    dx, dy, dz
            );
        }
    }

}
