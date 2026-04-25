package com.jeremyseq.inhabitants.entities.concher.render;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;
import com.jeremyseq.inhabitants.entities.concher.ai.ConcherAi;

import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class ConcherAnimationHandler {
    private static final String CONTROLLER_DEFAULTS = "defaults_controller";

    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_MOVING = "walk";

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
        
        if (concher.getAIState() == ConcherAi.State.WANDERING) {

            controller.setAnimation(
                RawAnimation.begin()
                .then(ANIM_MOVING, Animation.LoopType.LOOP)
            );
        } else {
            controller.setAnimation(
                RawAnimation.begin()
                .then(ANIM_IDLE, Animation.LoopType.LOOP)
            );
        }

        return PlayState.CONTINUE;
    }
}