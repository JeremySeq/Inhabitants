package com.jeremyseq.inhabitants.entities.bogre.render;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreNeutralGoal;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;
import com.jeremyseq.inhabitants.networking.bogre.BogreSkillKeyframePacketC2S;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.networking.ModNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.registries.ForgeRegistries;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;

import java.util.*;

public class BogreAnimationHandler {
    // Controller
    private static final String CONTROLLER_HURT = "hurt";
    private static final String CONTROLLER_DEFAULTS = "defaults_controller";
    private static final String CONTROLLER_MAIN = "controller";
    private static final String CONTROLLER_TRIGGER = "trigger_controller";

    // Animation
    private static final String ANIM_TAKING_DAMAGE = "taking_damage";
    private static final String ANIM_DANCE_START = "dance start";
    private static final String ANIM_DANCE = "dance";
    private static final String ANIM_DANCE_END = "dance end";
    private static final String ANIM_RUN = "run";
    private static final String ANIM_WALK = "walk";
    private static final String ANIM_IDLE_RARE = "idle_rare";
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_COOKING_START = "cooking_start";
    private static final String ANIM_COOKING_LOOP = "cooking_loop";
    private static final String ANIM_COOKING_END = "cooking_end";
    private static final String ANIM_COOKING = "cooking";
    private static final String ANIM_HAMMER_START = "hammer_skill_start";
    private static final String ANIM_HAMMER_LOOP = "hammer_skill_loop";
    private static final String ANIM_HAMMER_END = "hammer_skill_end";
    private static final String ANIM_CARVING = "carving";
    private static final String ANIM_HURT = "hurt";
    private static final String ANIM_ATTACK = "attack";
    private static final String ANIM_ROAR = "roar";
    private static final String ANIM_GRAB = "grab";

    // Keyframe Instructions & Sounds
    private static final String KEYFRAME_ATTACK_TRIGGER = "attackTrigger";
    private static final String KEYFRAME_HAMMER_SOUND = "hammer_sound";
    private static final String KEYFRAME_ROAR = "roar";
    private static final String KEYFRAME_SKILL_FINISHED = "skill_finished";
    private static final String KEYFRAME_COOKING_START_SOUND = "cooking_start_sound";
    private static final String KEYFRAME_COOKING_LOOP_SOUND = "cooking_loop_sound";


    public static void registerControllers(BogreEntity bogre, AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(bogre, CONTROLLER_HURT, 0, state -> PlayState.STOP)
                .triggerableAnim(ANIM_HURT, RawAnimation.begin().then(ANIM_TAKING_DAMAGE, Animation.LoopType.PLAY_ONCE)));
        
        registrar.add(new AnimationController<>(bogre, CONTROLLER_DEFAULTS, 0, BogreAnimationHandler::defaults)
                .transitionLength(3));
        
        registrar.add(new AnimationController<>(bogre, CONTROLLER_MAIN, 0, BogreAnimationHandler::predicate)
                .setSoundKeyframeHandler(BogreAnimationHandler::soundKeyframeHandler));
        
        registrar.add(new AnimationController<>(bogre, CONTROLLER_TRIGGER, 0, BogreAnimationHandler::triggerPredicate)
                .triggerableAnim(ANIM_ATTACK, RawAnimation.begin().then(ANIM_ATTACK, Animation.LoopType.PLAY_ONCE))
                .triggerableAnim(ANIM_ROAR, RawAnimation.begin().then(ANIM_ROAR, Animation.LoopType.PLAY_ONCE))
                .triggerableAnim(ANIM_GRAB, RawAnimation.begin().then(ANIM_GRAB, Animation.LoopType.PLAY_ONCE))
                .setCustomInstructionKeyframeHandler(BogreAnimationHandler::customKeyframeHandler)
                .setSoundKeyframeHandler(BogreAnimationHandler::soundKeyframeHandler));
    }

    private static void customKeyframeHandler(CustomInstructionKeyframeEvent<BogreEntity> event) {
        BogreEntity bogre = event.getAnimatable();
        if (!bogre.level().isClientSide) return;

        String instructions = event.getKeyframeData().getInstructions().trim();

        if (instructions.contains(KEYFRAME_ATTACK_TRIGGER)) {
            
        }
    }


    public static Optional<Vec3> getBonePosition(BogreEntity bogre, String boneName) {
        EntityRenderer<? super BogreEntity> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bogre);

        if (renderer instanceof BogreRenderer bogreRenderer) {
            GeoBone bone = bogreRenderer.getGeoModel().getBone(boneName).orElse(null);
            if (bone != null) {
                return Optional.of(new Vec3(
                    bone.getWorldPosition().x,
                    bone.getWorldPosition().y,
                    bone.getWorldPosition().z
                ));
            }
        }
        return Optional.empty();
    }

    private static PlayState defaults(AnimationState<BogreEntity> animationState) {
        BogreEntity bogre = animationState.getAnimatable();
        AnimationController<BogreEntity> controller = animationState.getController();

        if (handleDancingAnimation(bogre, controller)) {
            return PlayState.CONTINUE;
        }

        handleMovementOrIdleAnimation(bogre, animationState, controller);
        return PlayState.CONTINUE;
    }

    private static boolean handleDancingAnimation(BogreEntity bogre, AnimationController<BogreEntity> controller) {
        boolean isDancing = bogre.getAIState() == BogreAi.State.NEUTRAL &&
        bogre.getNeutralState() == BogreAi.NeutralState.DANCING;
        
        if (isDancing && bogre.dancePhase == BogreNeutralGoal.DancePhase.NONE) {
            bogre.dancePhase = BogreNeutralGoal.DancePhase.START;
        } else if (!isDancing && bogre.dancePhase == BogreNeutralGoal.DancePhase.LOOP) {
            bogre.dancePhase = BogreNeutralGoal.DancePhase.END;
        }

        if (bogre.dancePhase != BogreNeutralGoal.DancePhase.NONE) {
            switch (bogre.dancePhase) {
                case START -> {
                    controller.setAnimation(RawAnimation.begin().then(ANIM_DANCE_START, Animation.LoopType.PLAY_ONCE));
                    if (controller.hasAnimationFinished()) {
                        bogre.dancePhase = BogreNeutralGoal.DancePhase.LOOP;
                        controller.forceAnimationReset();
                    }
                }
                case LOOP -> controller.setAnimation(RawAnimation.begin().then(ANIM_DANCE, Animation.LoopType.LOOP));
                case END -> {
                    controller.setAnimation(RawAnimation.begin().then(ANIM_DANCE_END, Animation.LoopType.PLAY_ONCE));
                    if (controller.hasAnimationFinished()) {
                        bogre.dancePhase = BogreNeutralGoal.DancePhase.NONE;
                        controller.forceAnimationReset();
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static void handleMovementOrIdleAnimation(BogreEntity bogre, AnimationState<BogreEntity> animationState,
    AnimationController<BogreEntity> controller) {

        if (animationState.isMoving()) {
            String moveAnim = bogre.isSprinting() ? ANIM_RUN : ANIM_WALK;
            controller.setAnimation(RawAnimation.begin().then(moveAnim, Animation.LoopType.LOOP));
        } else {
            if (bogre.getAIState() != BogreAi.State.AGGRESSIVE && bogre.randomChance) {
                controller.setAnimation(RawAnimation.begin().then(ANIM_IDLE_RARE, Animation.LoopType.PLAY_ONCE));
                if (controller.hasAnimationFinished()) {
                    bogre.randomChance = false;
                }
            } else {
                controller.setAnimation(RawAnimation.begin().then(ANIM_IDLE, Animation.LoopType.LOOP));
                
                if (bogre.getRandom().nextFloat() < 0.001f) {
                    bogre.randomChance = true;
                }
            }
        }
    }

    private static PlayState predicate(AnimationState<BogreEntity> animationState) {
        BogreEntity bogre = animationState.getAnimatable();
        AnimationController<BogreEntity> controller = animationState.getController();

        if (handleCookingAnimation(bogre, animationState, controller)) {
            return PlayState.CONTINUE;
        }

        if (handleCarvingAnimation(bogre, animationState, controller)) {
            return PlayState.CONTINUE;
        }

        controller.setAnimationSpeed(1.0f);
        controller.forceAnimationReset();
        return PlayState.STOP;
    }

    private static boolean handleCookingAnimation(BogreEntity bogre, AnimationState<BogreEntity> animationState,
    AnimationController<BogreEntity> controller) {
        if (bogre.getEntityData().get(BogreEntity.COOKING_ANIM)) {
            int phase = bogre.getEntityData().get(BogreEntity.ANIMATION_PHASE);
            
            if (phase == 0 || phase == 1) {
                animationState.setAndContinue(RawAnimation.begin()
                        .then(ANIM_COOKING_START, Animation.LoopType.PLAY_ONCE)
                        .then(ANIM_COOKING_LOOP, Animation.LoopType.LOOP));
            } else if (phase == 2) {
                animationState.setAndContinue(RawAnimation.begin().then(ANIM_COOKING_END, Animation.LoopType.PLAY_ONCE));
            } else {
                animationState.setAndContinue(RawAnimation.begin().then(ANIM_COOKING, Animation.LoopType.PLAY_ONCE));
            }
            
            if (controller.getCurrentAnimation() != null && 
                controller.getCurrentAnimation().animation().name().equals(ANIM_COOKING_START)) {
                controller.setAnimationSpeed(1.5f);
            } else {
                controller.setAnimationSpeed(1.0f);
            }

            if (phase == 2 && controller.hasAnimationFinished()) {
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                controller.forceAnimationReset();
            }
            return true;
        }
        return false;
    }

    private static boolean handleCarvingAnimation(BogreEntity bogre, AnimationState<BogreEntity> animationState,
    AnimationController<BogreEntity> controller) {
        if (bogre.getEntityData().get(BogreEntity.CARVING_ANIM)) {
            int phase = bogre.getEntityData().get(BogreEntity.ANIMATION_PHASE);

            if (phase == 0 || phase == 1) {
                animationState.setAndContinue(RawAnimation.begin()
                        .then(ANIM_HAMMER_START, Animation.LoopType.PLAY_ONCE)
                        .then(ANIM_HAMMER_LOOP, Animation.LoopType.LOOP));
            } else if (phase == 2) {
                animationState.setAndContinue(RawAnimation.begin().then(ANIM_HAMMER_END, Animation.LoopType.PLAY_ONCE));
            } else {
                animationState.setAndContinue(RawAnimation.begin().then(ANIM_CARVING, Animation.LoopType.PLAY_ONCE));
            }
            
            if (controller.getCurrentAnimation() != null && 
                controller.getCurrentAnimation().animation().name().equals(ANIM_HAMMER_START)) {
                controller.setAnimationSpeed(1.3f);
            } else {
                controller.setAnimationSpeed(1.0f);
            }

            if (phase == 2 && controller.hasAnimationFinished()) {
                bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                controller.forceAnimationReset();
                
                ModNetworking.sendToServer(new BogreSkillKeyframePacketC2S(bogre.getId(), KEYFRAME_SKILL_FINISHED));
            }
            return true;
        }
        return false;
    }

    private static PlayState triggerPredicate(AnimationState<BogreEntity> state) {
        AnimationController<BogreEntity> controller = state.getController();
        if (controller.getCurrentAnimation() != null) {
            String name = controller.getCurrentAnimation().animation().name();
            if (name.equals(ANIM_ROAR)) {
                controller.setAnimationSpeed(1.55f);
            } else {
                controller.setAnimationSpeed(1.0f);
            }
        }
        return PlayState.STOP;
    }

    private static void soundKeyframeHandler(SoundKeyframeEvent<BogreEntity> event) {
        playSound(event.getAnimatable(), event.getKeyframeData().getSound().trim());
    }

    private static void playSound(BogreEntity bogre, String soundName) {
        Player player = ClientUtils.getClientPlayer();
        if (player == null) return;

        // transformation/carving hammer sound
        if (soundName.equals(KEYFRAME_HAMMER_SOUND)) {
            String customSound = bogre.getEntityData().get(BogreEntity.HAMMER_SOUND);
            boolean soundPlayed = false;
            float pitch = 0.8F;

            if (bogre.getAIState() == BogreAi.State.SKILLING) {
                int expectedHits = Math.max(1, bogre.getEntityData().get(BogreEntity.HAMMER_HITS));
                int currentHit = bogre.clientSkillHits + 1;
                float progress = (float) currentHit / (float) expectedHits;
                pitch = 0.8F + (progress * 0.8F);
            } else {
                pitch = 0.8F + new Random().nextFloat() * 0.4F;
            }

            if (!customSound.isEmpty()) {
                SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(customSound));
                if (soundEvent != null) {
                    player.playSound(soundEvent, 1f, pitch);
                    soundPlayed = true;
                }
            }

            if (bogre.getAIState() == BogreAi.State.SKILLING) {
                ModNetworking.sendToServer(new BogreSkillKeyframePacketC2S(bogre.getId(), soundName));
                BogreAi.SkillingState state = bogre.getCraftingState();
                if (state == BogreAi.SkillingState.CARVING) {
                    BogreSkills.CARVING.keyframeTriggered(bogre, soundName);
                } else if (state == BogreAi.SkillingState.TRANSFORMATION) {
                    BogreSkills.TRANSFORMATION.keyframeTriggered(bogre, soundName);
                }
            } else if (!soundPlayed) {
                player.playSound(SoundEvents.ANVIL_LAND, .5f, pitch);
            }
        } else if (soundName.equals(KEYFRAME_ROAR)) {
            // roar sound
            bogre.playSound(ModSoundEvents.BOGRE_ROAR.get(), 1f, 1f);
        } else if (soundName.contains(KEYFRAME_COOKING_START_SOUND)) {
            // cooking start sound
            player.playSound(ModSoundEvents.BOGRE_COOKING_START.get(), 1.0f, 1.0f);
        } else if (soundName.contains(KEYFRAME_COOKING_LOOP_SOUND)) {
            // cooking loop sound
            player.playSound(ModSoundEvents.BOGRE_COOKING_LOOP.get(), 1.2f, 1.0f);
        }
    }
}