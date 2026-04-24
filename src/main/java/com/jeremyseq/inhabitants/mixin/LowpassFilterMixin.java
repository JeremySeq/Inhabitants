package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.events.ModClientEvents;
import com.jeremyseq.inhabitants.audio.InhabitantsAudio;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;

import net.minecraft.util.Mth;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class LowpassFilterMixin {

    @Mixin(Channel.class)
    public static class AudioChannel {
        @Shadow private int source;
        
        // pitch shifting
        @ModifyVariable(
            method = "setPitch",
            at = @At("HEAD"),
            argsOnly = true
        )
        private float inhabitants$modifyPitch(float pitch) {
            if (ModClientEvents.muffleLerp > 0.0F) {
                return pitch * Mth.lerp(ModClientEvents.muffleLerp, 1.0F, 0.75F);
            }
            return pitch;
        }

        // lowpass filter
        @Inject(
            method = {"setVolume",
            "setPitch",
            "play",
            "attachBufferStream",
            "attachStaticBuffer",
            "updateStream"},
            at = @At("HEAD")
        )
        private void inhabitants$updateMuffleFilter(CallbackInfo ci) {
            if (!InhabitantsAudio.efxSupported ||
                InhabitantsAudio.lowpassFilterId == -1) return;
            
            if (ModClientEvents.muffleLerp > 0.0F) {
                AL10.alSourcei(this.source, EXTEfx.AL_DIRECT_FILTER,
                    InhabitantsAudio.lowpassFilterId);
            }
        }
    }

    // initializes and cleanup OpenAL EFX
    @Mixin(Library.class)
    public static class AudioLibrary {
        @Inject(
            method = "init",
            at = @At("RETURN")
        )
        private void inhabitants$initOpenALefx(String pDeviceSpecifier,
            boolean pEnableHrtf, CallbackInfo ci) {
            InhabitantsAudio.init();
        }

        @Inject(
            method = "cleanup",
            at = @At("RETURN")
        )
        private void inhabitants$cleanupOpenALefx(CallbackInfo ci) {
            InhabitantsAudio.cleanup();
        }
    }
}
