package com.jeremyseq.inhabitants.entities.catcher;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public class CatcherAmbushGoal extends Goal {
    private final CatcherEntity catcher;
    private int emergeTicks;

    public CatcherAmbushGoal(CatcherEntity catcher) {
        this.catcher = catcher;
    }

    @Override
    public boolean canUse() {
        if (catcher.getState() == CatcherEntity.State.AMBUSH) return true; // handle relaunch session case
        if (catcher.getState() != CatcherEntity.State.BURROWED) return false;
        Player player = catcher.level().getNearestPlayer(catcher, 5.0D);
        return player != null && catcher.hasLineOfSight(player);
    }

    @Override
    public void start() {
        catcher.setState(CatcherEntity.State.AMBUSH);
        catcher.triggerAnim("ground_change", "emerging");
        catcher.setNoGravity(false);
        emergeTicks = 20; // 1 second emerge anim delay
    }

    @Override
    public void tick() {
        // stay in place while emerging
        catcher.setDeltaMovement(0, 0, 0);

        if (catcher.level() instanceof ServerLevel serverLevel) {

            // spawn particles
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    catcher.getX(), catcher.getY() + 0.1D, catcher.getZ(),
                    5,
                    (catcher.getRandom().nextDouble() - 0.5D) * 0.1D,
                    0.1D,
                    (catcher.getRandom().nextDouble() - 0.5D) * 0.1D,
                    0.0D
            );

            // play sound
            if (emergeTicks >= 10) {
                serverLevel.playSound(null, catcher.blockPosition(), SoundEvents.SAND_STEP, catcher.getSoundSource(), 1.0F, 1.0F);
            }
        }

        emergeTicks--;
        if (emergeTicks <= 0) {
            // finished emerging, now attack
            catcher.setTarget(catcher.level().getNearestPlayer(catcher, 10.0D)); // maybe unecessary
            catcher.setState(CatcherEntity.State.IDLE);
            // let melee goal take over
        }
    }

    @Override
    public boolean canContinueToUse() {
        // stops after emerge animation is done
        return emergeTicks > 0;
    }
}
