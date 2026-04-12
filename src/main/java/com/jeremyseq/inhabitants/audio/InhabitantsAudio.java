package com.jeremyseq.inhabitants.audio;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.util.Mth;

import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

/*
* Manages OpenAL audio effects
* - initializes and controls a lowpass filter using the OpenAL EFX extension
*/
public class InhabitantsAudio {

        public static int lowpassFilterId = -1;
        public static boolean efxSupported = false;
        
        public static void init() {
            long context = ALC10.alcGetCurrentContext();
            if (context == 0L) {
                Inhabitants.LOGGER.warn("Inhabitants: No OpenAL context available");
                return;
            }

            long device = ALC10.alcGetContextsDevice(context);
            if (device == 0L) {
                Inhabitants.LOGGER.warn("Inhabitants: No OpenAL device available");
                return;
            }

            efxSupported = ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX");

            if (efxSupported) {
                lowpassFilterId = EXTEfx.alGenFilters();

                EXTEfx.alFilteri(lowpassFilterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                EXTEfx.alFilterf(lowpassFilterId, EXTEfx.AL_LOWPASS_GAIN, 0.3F);
                EXTEfx.alFilterf(lowpassFilterId, EXTEfx.AL_LOWPASS_GAINHF, 0.01F);
            } else {
                Inhabitants.LOGGER.warn("Inhabitants: OpenAL EXT_EFX is not supported");
            }
        }

        public static void updateFilter(float value) {
            if (efxSupported && lowpassFilterId != -1) {
                float gain = Mth.lerp(value, 1.0F, 0.3F);
                float gainHF = Mth.lerp(value, 1.0F, 0.01F);

                EXTEfx.alFilterf(lowpassFilterId, EXTEfx.AL_LOWPASS_GAIN, gain);
                EXTEfx.alFilterf(lowpassFilterId, EXTEfx.AL_LOWPASS_GAINHF, gainHF);
            }
        }

        public static void cleanup() {
            if (efxSupported && lowpassFilterId != -1) {
                EXTEfx.alDeleteFilters(lowpassFilterId);
                lowpassFilterId = -1;
            }
        }
    }