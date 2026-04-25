package com.jeremyseq.inhabitants.entities.concher.render;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;
import com.jeremyseq.inhabitants.entities.concher.ai.ConcherAi;
import com.jeremyseq.inhabitants.entities.concher.ai.ConcherPathfinding;

import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class ConcherAnimationHandler {
    private static final String CONTROLLER_DEFAULTS = "defaults_controller";

    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_MOVING = "walk";
    private static final String ANIM_SWIM = "swimming";
    private static final String ANIM_PRE_WALK = "pre-walk";

    public static void registerControllers(
        ConcherEntity concher,
        AnimatableManager.ControllerRegistrar registrar
    ) {
        registrar.add(
            new AnimationController<>(
                concher,
                CONTROLLER_DEFAULTS,
                0,
                ConcherAnimationHandler::defaults
            )
            .transitionLength(3)
        );
    }

    private static PlayState defaults(AnimationState<ConcherEntity> animationState) {
        ConcherEntity concher = animationState.getAnimatable();
        AnimationController<ConcherEntity> controller = animationState.getController();
        
        ConcherAi.State state = concher.getAIState();
        boolean isPausing = false;
        if (concher.getMoveControl() instanceof ConcherPathfinding.ConcherMoveControl moveControl) {
            isPausing = moveControl.isPausing();
        }
        
        if (state == ConcherAi.State.WANDERING) {
            if (concher.getStage() == 0 && concher.isInWater()) {
                controller.setAnimationSpeed(1.0);
                controller.setAnimation(
                    RawAnimation.begin()
                        .then(ANIM_SWIM, Animation.LoopType.LOOP)
                );
            } else {
                if (isPausing) {
                    controller.setAnimationSpeed(concher.getAI().getAnimationSpeedModifier());
                    controller.setAnimation(
                        RawAnimation.begin()
                            .then(ANIM_PRE_WALK, Animation.LoopType.PLAY_ONCE)
                    );
                } else {
                    controller.setAnimationSpeed(concher.getAI().getAnimationSpeedModifier());
                    controller.setAnimation(
                        RawAnimation.begin()
                            .then(ANIM_MOVING, Animation.LoopType.PLAY_ONCE)
                    );
                }
            }
        } else {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(
                RawAnimation.begin()
                .then(ANIM_IDLE, Animation.LoopType.LOOP)
            );
        }

        return PlayState.CONTINUE;
    }
}