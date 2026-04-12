package com.jeremyseq.inhabitants.audio;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ModTickableSounds {

    public static class DrillLoop extends AbstractTickableSoundInstance {
        private final Player player;
        public static DrillLoop currentSound = null;

        public DrillLoop(Player player) {
            super(ModSoundEvents.DRILL_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.25f;
            this.pitch = 1.0f;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (!player.isAlive() ||
                    !player.isUsingItem() ||
                    !(player.getUseItem().getItem() instanceof SpikeDrillItem)) {
                this.stopLoop();
            }
        }

        public void stopLoop() {
            this.stop();
            if (currentSound == this) {
                currentSound = null;
            }
        }
    }

    public static class ConcussionBuzz extends AbstractTickableSoundInstance {
        private final Player player;

        public ConcussionBuzz(Player player) {
            super(ModSoundEvents.CONCUSSION_BUZZ.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 3.0F;
            this.pitch = 1.25F;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (!player.isAlive() ||
                    !player.hasEffect(ModEffects.CONCUSSION.get())) {
                this.stop();
            }
        }
    }

    public static class ImmaterialInsideLoop extends AbstractTickableSoundInstance {
        private final Player player;

        public ImmaterialInsideLoop(Player player) {
            super(ModSoundEvents.IMMATERIAL_INSIDE.get(),
                SoundSource.AMBIENT,
                SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 5.0F;
            this.pitch = 1.0F;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (!player.isAlive() ||
                !player.hasEffect(ModEffects.IMMATERIAL.get())) {
                this.stop();
            }
        }

        public void stopSound() {
            this.stop();
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }
    }
}
