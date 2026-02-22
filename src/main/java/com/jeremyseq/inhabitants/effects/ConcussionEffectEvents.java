package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, value = Dist.CLIENT)
public class ConcussionEffectEvents {

    /**
     * When the concussion effect is applied to the player, play a concussion sound.
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void addEffect(MobEffectEvent.Added addEffectEvent) {
        if (addEffectEvent.getEffectInstance().getEffect() == ModEffects.CONCUSSION.get()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSoundEvents.IMPALER_CONCUSSION.get(), 1.0f));
        }
    }

    /**
     * When sounds are played, if the player has the concussion effect, reduce their volume and pitch.
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.player.hasEffect(ModEffects.CONCUSSION.get())) {

            SoundInstance original = event.getSound();
            if (original == null) return;

            event.setSound(new ConcussionSoundInstance(original));
        }
    }

    /**
     * Modified sound instance that reduces volume and pitch for players with the concussion effect.
     */
    private static class ConcussionSoundInstance implements SoundInstance {

        private final SoundInstance original;
        private static final int FADE_TICKS = 40;

        public ConcussionSoundInstance(SoundInstance original) {
            this.original = original;
        }

        @Override
        public float getVolume() {
            return original.getVolume() * getEffectMultiplier(0.1f, 1.0f);
        }

        @Override
        public float getPitch() {
            return original.getPitch() * getEffectMultiplier(0.6f, 1.0f);
        }

        /**
         * Calculates a multiplier based on concussion effect duration.
         * Starts at startValue, fades to endValue over FADE_TICKS.
         */
        private float getEffectMultiplier(float startValue, float endValue) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return endValue;

            var effect = mc.player.getEffect(ModEffects.CONCUSSION.get());
            if (effect == null) return endValue;

            int remaining = effect.getDuration();
            if (remaining > FADE_TICKS) return startValue;

            float progress = 1.0f - (remaining / (float) FADE_TICKS);
            return startValue + progress * (endValue - startValue);
        }

        @Override public @NotNull ResourceLocation getLocation() {
            return original.getLocation();
        }

        @Override public @Nullable WeighedSoundEvents resolve(@NotNull SoundManager manager) {
            return original.resolve(manager);
        }

        @Override public @NotNull Sound getSound() {
            return original.getSound();
        }

        @Override public @NotNull SoundSource getSource() {
            return original.getSource();
        }

        @Override public boolean isLooping() {
            return original.isLooping();
        }

        @Override public boolean isRelative() {
            return original.isRelative();
        }

        @Override public int getDelay() {
            return original.getDelay();
        }

        @Override public double getX() {
            return original.getX();
        }

        @Override public double getY() {
            return original.getY();
        }

        @Override public double getZ() {
            return original.getZ();
        }

        @Override public @NotNull Attenuation getAttenuation() {
            return original.getAttenuation();
        }
    }
}