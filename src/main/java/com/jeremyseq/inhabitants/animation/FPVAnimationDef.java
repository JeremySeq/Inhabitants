package com.jeremyseq.inhabitants.animation;

import java.util.List;

/**
 * loaded from JSON by FPVAnimationManager
 */
public record FPVAnimationDef(
        int durationTicks,
        String easing,
        LoopMode loopMode,
        List<Keyframe> keyframes,
        LoopTransform loopTransform,
        VibrateConfig vibrate,
        String continueTo,
        String exitTo
) {
    // a single keyframe, time is normalized 0.0–1.0 within the phase
    public record Keyframe(
            float time,
            float translateX,
            float translateY,
            float translateZ,
            float rotateX,
            float rotateY,
            float rotateZ,
            String triggerKeyframe
    ) {}
    
    public record LoopTransform(
            float translateX,
            float translateY,
            float translateZ,
            float rotateX,
            float rotateY,
            float rotateZ
    ) {}
    
    public record VibrateConfig(
            float amplitudeX,
            float amplitudeY,
            float freqX,
            float freqY
    ) {}

    public enum LoopMode {
        LOOP,
        PLAY_ONCE;

        public static LoopMode from(String s) {
            if (s == null) return LOOP;
            return switch (s.toLowerCase()) {
                case "play_once" -> PLAY_ONCE;
                default          -> LOOP;
            };
        }
    }

    public boolean isInfiniteLoop() {
        return durationTicks <= 0;
    }
}
