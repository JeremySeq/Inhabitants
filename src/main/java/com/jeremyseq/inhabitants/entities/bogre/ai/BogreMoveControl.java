package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.util.Mth;

public class BogreMoveControl extends MoveControl {
    
    public static float neutralLerpStrength = 0.1f;
    public static float aggressiveLerpStrength = 0.22f;
    public static float skillingLerpStrength = 0.35f;

    public BogreMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    protected float rotlerp(float pAngle, float pTargetAngle, float pMaxSpeed) {
        float angle = Mth.wrapDegrees(pTargetAngle - pAngle);
        float lerp = neutralLerpStrength;
        
        if (mob instanceof BogreEntity bogre) {
            if (bogre.getAIState() == BogreAi.State.AGGRESSIVE) {
                lerp = (bogre.getAggressiveState() == BogreAi.AggressiveState.ATTACKING) ? 1.0f : aggressiveLerpStrength;
            } else if (bogre.getAIState() == BogreAi.State.SKILLING) {
                lerp = skillingLerpStrength;
            }
        }
        
        return pAngle + angle * lerp;
    }
}
