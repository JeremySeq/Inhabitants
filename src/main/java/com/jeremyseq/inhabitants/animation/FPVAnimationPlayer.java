package com.jeremyseq.inhabitants.animation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.common.MinecraftForge;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Quaternionf;

import java.util.List;

/**
 * Simply, MOJANG is lazy, and they hardcoded the fpv hand animations
 * so im prayinn to god for that suffering, and for the sake of Minecraft modding.
 * 
 * made with ANGER and love by bobotweaks
 * im joking, im not angry, i made this for fun
 * 
 * however, this is only for first-person view animations
 */
public class FPVAnimationPlayer {
    public static final FPVAnimationPlayer INSTANCE = new FPVAnimationPlayer();

    private static final float DEFAULT_TRANSITION_TICKS = 5.0F;

    private boolean wasUsingMain = false;
    private long releaseTimeMain = 0;
    private long loopStartMain   = 0;

    private boolean wasUsingOff  = false;
    private long releaseTimeOff  = 0;
    private long loopStartOff    = 0;

    private float lastStartProgressMain = -1f;
    private float lastLoopProgressMain  = -1f;
    private float lastEndProgressMain   = -1f;

    private float lastStartProgressOff = -1f;
    private float lastLoopProgressOff  = -1f;
    private float lastEndProgressOff   = -1f;

    private String overridePhaseMain = null;
    private long   overrideStartMain = -1;
    private float  lastOverrideProgressMain = -1f;

    private String overridePhaseOff = null;
    private long   overrideStartOff = -1;
    private float  lastOverrideProgressOff = -1f;

    private float speedMain = 1.0f;
    private float speedOff = 1.0f;

    private AnimationTriggerCallback triggerCallback = null;

    private final FPVAnimationManager animationManager = FPVAnimationManager.INSTANCE;

    @FunctionalInterface
    public interface AnimationTriggerCallback {
        void onTrigger(String id, Player player,
            HumanoidArm arm, ItemStack stack);
    }

    public void setTriggerCallback(AnimationTriggerCallback callback) {
        this.triggerCallback = callback;
    }

    public void playOverridePhase(HumanoidArm arm, String phase) {
        if (arm == HumanoidArm.RIGHT) {
            this.overridePhaseMain = phase;
            this.overrideStartMain = -1;
            this.lastOverrideProgressMain = -1f;
        } else {
            this.overridePhaseOff = phase;
            this.overrideStartOff = -1;
            this.lastOverrideProgressOff = -1f;
        }
    }
    public boolean apply(
            String animId,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            boolean applyBase
    ) {
        long ticks = player.tickCount;
        boolean isRight = arm == HumanoidArm.RIGHT;
        float speedMultiplier = isRight ? speedMain : speedOff;
        int i = isRight ? 1 : -1;
        String overridePhase = isRight ? overridePhaseMain : overridePhaseOff;

        if (overridePhase != null) {
            FPVAnimationDef overrideDef = animationManager.get(overridePhase);
            
            if (overrideDef == null && animId != null) {
                overrideDef = animationManager.get(animId + "_" + overridePhase);
            }
            if (overrideDef == null) {
                overrideDef = animationManager.get(overridePhase + "_generic");
            }
            
            if (overrideDef != null) {
                if (isRight) { if (overrideStartMain == -1) overrideStartMain = ticks; }
                else { if (overrideStartOff == -1) overrideStartOff = ticks; }

                long start = isRight ? overrideStartMain : overrideStartOff;
                float duration = overrideDef.durationTicks() > 0 ?
                    overrideDef.durationTicks() : DEFAULT_TRANSITION_TICKS;
                
                float progress = (ticks - start + partialTick) / duration;

                if (progress < 1.0F) {
                    float lastProgress = isRight ?
                        lastOverrideProgressMain : lastOverrideProgressOff;
                    
                    checkTriggers(
                        overrideDef,
                        player,
                        arm,
                        itemInHand,
                        lastProgress,
                        progress,
                        false
                    );
                    
                    if (isRight) lastOverrideProgressMain = progress;
                    else lastOverrideProgressOff = progress;

                    if (applyBase) applyVanillaBase(poseStack, i, equipProcess);
                    applyKeyframes(poseStack, overrideDef, i, progress);
                    return true;
                }
            }
            
            if (isRight) {
                overridePhaseMain = null;
                overrideStartMain = -1;
                lastOverrideProgressMain = -1f;
            } else {
                overridePhaseOff = null;
                overrideStartOff = -1;
                lastOverrideProgressOff = -1f;
            }
        }

        boolean isUsing = player.isUsingItem() &&
            player.getUseItem() == itemInHand;

        FPVAnimationDef startDef = animId != null ? animationManager.get(animId) : null;
        FPVAnimationDef loopDef  = null;
        FPVAnimationDef endDef   = null;

        if (startDef != null && startDef.continueTo() != null) {
            loopDef = animationManager.get(startDef.continueTo());
        }

        if (loopDef != null && loopDef.exitTo() != null) {
            endDef = animationManager.get(loopDef.exitTo());
        } else if (startDef != null && startDef.exitTo() != null) {
            endDef = animationManager.get(startDef.exitTo());
        }

        float startDuration = startDef != null &&
            startDef.durationTicks() > 0
            ? startDef.durationTicks()
            : DEFAULT_TRANSITION_TICKS;

        float endDuration = endDef != null &&
            endDef.durationTicks() > 0
            ? endDef.durationTicks()
            : DEFAULT_TRANSITION_TICKS;

        if (isUsing) {
            if (isRight) {
                if (!wasUsingMain) {
                    wasUsingMain = true;
                    loopStartMain = ticks;
                    releaseTimeMain = 0;
                    lastStartProgressMain = -1f;
                    lastLoopProgressMain = -1f;
                }
            } else {
                if (!wasUsingOff) {
                    wasUsingOff = true;
                    loopStartOff = ticks;
                    releaseTimeOff = 0;
                    lastStartProgressOff = -1f;
                    lastLoopProgressOff = -1f;
                }
            }
            
            if (applyBase) applyVanillaBase(poseStack, i, equipProcess);

            float ticksUsed = itemInHand.getUseDuration() -
                player.getUseItemRemainingTicks() +
                partialTick;
            
            if (ticksUsed < startDuration) {
                if (startDef != null) {
                    float progress = ticksUsed / startDuration;
                    float lastProgress = isRight ? lastStartProgressMain : lastStartProgressOff;

                    checkTriggers(
                        startDef,
                        player,
                        arm,
                        itemInHand,
                        lastProgress,
                        progress,
                        false
                    );
                    
                    if (isRight) lastStartProgressMain = progress;
                    else lastStartProgressOff = progress;

                    applyKeyframes(poseStack, startDef, i, progress);
                    return true;
                }
            }
            
            if (loopDef != null) {
                long loopStart = isRight ? loopStartMain : loopStartOff;
                float loopElapsed = (ticks - loopStart) + partialTick - startDuration;

                applyLoopPhase(
                    poseStack,
                    loopDef,
                    i,
                    loopElapsed,
                    partialTick,
                    player,
                    arm,
                    itemInHand,
                    isRight,
                    speedMultiplier
                );
                return true;
            }

            return false;

        } else {
            if (isRight && wasUsingMain) {
                wasUsingMain = false;
                releaseTimeMain = ticks;
                lastEndProgressMain = -1f;
            } else if (!isRight && wasUsingOff) {
                wasUsingOff = false;
                releaseTimeOff = ticks;
                lastEndProgressOff = -1f;
            }

            long releaseTime = isRight ? releaseTimeMain : releaseTimeOff;
            if (releaseTime <= 0) return false;
            float timeSinceRelease = (ticks - releaseTime) + partialTick;
            
            if (timeSinceRelease < endDuration) {
                if (applyBase) applyVanillaBase(poseStack, i, equipProcess);

                if (endDef != null) {
                    float progress = timeSinceRelease / endDuration;
                    float lastProgress = isRight ? lastEndProgressMain : lastEndProgressOff;
                    
                    checkTriggers(
                        endDef,
                        player,
                        arm,
                        itemInHand,
                        lastProgress,
                        progress,
                        false
                    );
                    
                    if (isRight) lastEndProgressMain = progress;
                    else lastEndProgressOff = progress;

                    applyKeyframes(poseStack, endDef, i, progress);
                } else if (startDef != null) {
                    float progress = 1.0F - (timeSinceRelease / endDuration);
                    float lastProgress = isRight ? lastEndProgressMain : lastEndProgressOff;
                    
                    if (isRight) lastEndProgressMain = progress;
                    else lastEndProgressOff = progress;

                    applyKeyframes(poseStack, startDef, i, progress);
                }
                return true;
            } else {
                if (isRight) releaseTimeMain = 0;
                else releaseTimeOff = 0;
            }
        }
        
        return false;
    }

    public void play(String phaseName, HumanoidArm arm) {
        if (arm == HumanoidArm.RIGHT) {
            overridePhaseMain = phaseName;
            overrideStartMain = -1;
            lastOverrideProgressMain = -1f;
        } else {
            overridePhaseOff = phaseName;
            overrideStartOff = -1;
            lastOverrideProgressOff = -1f;
        }
    }

    public void clearOverrides() {
        overridePhaseMain = null;
        overridePhaseOff  = null;
    }

    private static void applyVanillaBase(PoseStack poseStack, int i, float equipProcess) {
        poseStack.translate(i * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
    }

    private void applyLoopPhase(
        PoseStack poseStack,
        FPVAnimationDef def,
        int i,
        float loopElapsed,
        float partialTick,
        LocalPlayer player,
        HumanoidArm arm,
        ItemStack itemInHand,
        boolean isMain,
        float speedMultiplier
    ) {
        if (!def.keyframes().isEmpty()) {
            float duration = def.durationTicks() > 0 ? def.durationTicks() : 20f;
            boolean looping = def.loopMode() == FPVAnimationDef.LoopMode.LOOP;
            
            float progress = looping
                    ? ((loopElapsed * speedMultiplier) % duration) / duration
                    : Math.min(1.0F, (loopElapsed * speedMultiplier) / duration);

            float lastProgress = isMain ? lastLoopProgressMain : lastLoopProgressOff;
            
            checkTriggers(
                def,
                player,
                arm,
                itemInHand,
                lastProgress,
                progress,
                looping
            );
            
            if (isMain) lastLoopProgressMain = progress;
            else lastLoopProgressOff = progress;

            applyKeyframes(poseStack, def, i, progress);
        } else if (def.loopTransform() != null) {
            FPVAnimationDef.LoopTransform lt = def.loopTransform();
            
            poseStack.translate(i * lt.translateX(), lt.translateY(), lt.translateZ());
            
            Quaternionf q = new Quaternionf()
                .rotateLocalX((float) Math.toRadians(lt.rotateX()))
                .rotateLocalY((float) Math.toRadians(i * lt.rotateY()))
                .rotateLocalZ((float) Math.toRadians(lt.rotateZ()));
            
            poseStack.mulPose(q);
        }
        
        FPVAnimationDef.VibrateConfig vib = def.vibrate();
        if (vib != null) {
            float time = (loopElapsed * speedMultiplier) + partialTick;
            
            poseStack.translate(
                Mth.sin(time * vib.freqX()) * vib.amplitudeX(),
                Mth.cos(time * vib.freqY()) * vib.amplitudeY(),
                0f
            );
        }
    }

    private void applyKeyframes(
        PoseStack poseStack,
        FPVAnimationDef def,
        int i,
        float rawProgress
    ) {
        float progress = ease(def.easing(), Mth.clamp(rawProgress, 0f, 1f));
        List<FPVAnimationDef.Keyframe> kfs = def.keyframes();

        if (kfs.isEmpty()) return;

        FPVAnimationDef.Keyframe from = kfs.get(0);
        FPVAnimationDef.Keyframe to = kfs.get(kfs.size() - 1);

        for (int k = 0; k < kfs.size() - 1; k++) {
            if (progress >= kfs.get(k).time() && progress <= kfs.get(k + 1).time()) {
                from = kfs.get(k);
                to = kfs.get(k + 1);
                break;
            }
        }

        float span = to.time() - from.time();
        float local = span <= 0 ? 1f : (progress - from.time()) / span;

        float tx = Mth.lerp(local, from.translateX(), to.translateX());
        float ty = Mth.lerp(local, from.translateY(), to.translateY());
        float tz = Mth.lerp(local, from.translateZ(), to.translateZ());
        float rx = Mth.lerp(local, from.rotateX(), to.rotateX());
        float ry = Mth.lerp(local, from.rotateY(), to.rotateY());
        float rz = Mth.lerp(local, from.rotateZ(), to.rotateZ());

        poseStack.translate(i * tx, ty, tz);
        
        Quaternionf q = new Quaternionf()
            .rotateLocalX((float) Math.toRadians(rx))
            .rotateLocalY((float) Math.toRadians(i * ry))
            .rotateLocalZ((float) Math.toRadians(i * rz));
        
        poseStack.mulPose(q);
    }

    private static float ease(String type, float t) {
        if (type == null) return t;
        return switch (type.toLowerCase()) {
            case "linear" -> t;
            case "easeinsine" -> 1f - Mth.cos(t * (float) Math.PI / 2f);
            case "easeoutsine" -> Mth.sin(t * (float) Math.PI / 2f);
            case "easeinoutsine" -> -(Mth.cos((float) Math.PI * t) - 1f) / 2f;
            case "easeinquad" -> t * t;
            case "easeoutquad" -> 1f - (1f - t) * (1f - t);
            case "easeinoutquad" -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
            case "easeinback" -> { float c = 1.70158f; yield t * t * ((c + 1f) * t - c); }
            case "easeoutback" -> { float c = 1.70158f; yield 1f + (float) Math.pow(t - 1, 2) * ((c + 1f) * (t - 1f) + c); }
            default -> t;
        };
    }

    private void checkTriggers(
        FPVAnimationDef def,
        LocalPlayer player,
        HumanoidArm arm,
        ItemStack item,
        float oldP,
        float newP,
        boolean looping
    ) {
        float effectiveOldP = oldP < 0 ? -0.0001f : oldP;

        for (FPVAnimationDef.Keyframe kf : def.keyframes()) {
            if (kf.triggerKeyframe() == null) continue;

            boolean triggered = false;
            if (looping && newP < effectiveOldP) {
                triggered = (kf.time() > effectiveOldP && kf.time() <= 1.0f) ||
                    (kf.time() >= 0.0f && kf.time() <= newP);
            } else {
                triggered = kf.time() > effectiveOldP &&
                    kf.time() <= newP;
            }

            if (triggered) {
                if (kf.triggerKeyframe() != null &&
                    triggerCallback != null) {
                    triggerCallback.onTrigger(kf.triggerKeyframe(), player, arm, item);
                }
            }
        }
    }

    public void setSpeed(HumanoidArm arm, float speed) {
        if (arm == HumanoidArm.RIGHT) this.speedMain = speed;
        else this.speedOff = speed;
    }

    public float getSpeed(HumanoidArm arm) {
        return (arm == HumanoidArm.RIGHT) ? this.speedMain : this.speedOff;
    }
}
