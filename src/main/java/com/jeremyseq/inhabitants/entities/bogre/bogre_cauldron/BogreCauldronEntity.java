package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.blocks.ModBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class BogreCauldronEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private boolean placedBlock = false;
    private float health;

    public BogreCauldronEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.health = 5f;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide) {
            if (level().getBlockState(this.blockPosition()).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                level().removeBlock(this.blockPosition(), false);
            }
        }
    }


    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.health -= amount;

            // play hit sound
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.0F);

            // show crit particles
            serverLevel.sendParticles(ParticleTypes.CRIT, getX() + 0.5, getY() + 0.5, getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);

            if (this.health <= 0) {
                // play break sound
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                // show block breaking particles (simulating stone break)
                serverLevel.levelEvent(2001, this.blockPosition(), Block.getId(Blocks.STONE.defaultBlockState()));

                this.remove(RemovalReason.KILLED);
            }
        }
        return true;
    }


    @Override
    public void tick() {
        super.tick();

        if (!placedBlock && !level().isClientSide) {
            level().setBlock(this.blockPosition(), ModBlocks.INVISIBLE_CAULDRON_BLOCK.get().defaultBlockState(), 3);
            placedBlock = true;
        }

        if (level().isClientSide) {
            double x = getX();
            double y = getY();
            double z = getZ();

            if (level().random.nextFloat() < 0.05f) {
                // Pick one of the four sides of the square
                int side = level().random.nextInt(4);
                double px = x;
                double pz = z;
                double dx = 0;
                double dz = 0;

                // Offset along side
                double offset = -0.9 + level().random.nextDouble() * 1.8;

                switch (side) {
                    case 0 -> { px += offset; pz -= 1.0; dz = -1; } // North
                    case 1 -> { px += offset; pz += 1.0; dz = 1;  } // South
                    case 2 -> { px -= 1.0; pz += offset; dx = -1; } // West
                    case 3 -> { px += 1.0; pz += offset; dx = 1;  } // East
                }

                double py = y + 0.05;

                // Add randomness to motion away from the center
                double vx = dx * (0.05 + level().random.nextDouble() * 0.03);
                double vz = dz * (0.05 + level().random.nextDouble() * 0.03);
                // Pop upward motion
                double vy = 0.1 + level().random.nextDouble() * 0.05;

                // Particle: same as lava pop
                level().addParticle(ParticleTypes.LAVA, px, py, pz, vx, vy, vz);

                // light smoke above
                if (level().random.nextBoolean()) {
                    level().addParticle(ParticleTypes.SMOKE, px, py + 0.1, pz, 0, 0.01, 0);
                }
            }


            double px = x - 1 + level().random.nextDouble() * 2.0;
            double pz = z - 1 + level().random.nextDouble() * 2.0;
            double py = y + 1.5;

            level().addParticle(ParticleTypes.EFFECT, px, py, pz, 0, 0.05, 0);

            level().playLocalSound(px, py, pz, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.4f, 0.8f + level().random.nextFloat() * 0.4f, false);

        }
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<BogreCauldronEntity> animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().then("animation", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
