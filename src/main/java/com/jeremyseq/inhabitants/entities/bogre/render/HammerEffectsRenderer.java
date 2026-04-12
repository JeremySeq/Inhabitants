package com.jeremyseq.inhabitants.entities.bogre.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HammerEffectsRenderer {

    public static void spawnCarvingParticles(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                20,
                0.3,
                0.3,
                0.3,
                0.1);
        } else if (level.isClientSide) {
            for (int i = 0; i < 20; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5 + (level.random.nextFloat() - 0.5) * 0.6,
                pos.getY() + 0.5 + (level.random.nextFloat() - 0.5) * 0.6,
                pos.getZ() + 0.5 + (level.random.nextFloat() - 0.5) * 0.6,
                (level.random.nextFloat() - 0.5) * 0.2,
                0.1 + level.random.nextFloat() * 0.2,
                (level.random.nextFloat() - 0.5) * 0.2);
            }
        }
    }

    public static void spawnTransformationParticles(Level level, Vec3 pos) {
        if (level instanceof ServerLevel serverLevel) {
            
            serverLevel.sendParticles(ParticleTypes.POOF,
            pos.x, pos.y + 0.2, pos.z,
            1, 0.02, 0.02, 0.02, 0.01);

        } else if (level.isClientSide) {
            for (int i = 0; i < 1; i++) {
                level.addParticle(ParticleTypes.POOF,
                pos.x + (level.random.nextFloat() - 0.5) * 0.05,
                pos.y + 0.2 + (level.random.nextFloat() - 0.5) * 0.05,
                pos.z + (level.random.nextFloat() - 0.5) * 0.05,
                (level.random.nextFloat() - 0.5) * 0.01,
                0.01 + level.random.nextFloat() * 0.01,
                (level.random.nextFloat() - 0.5) * 0.01);
            }
        }
    }
}