package com.jeremyseq.inhabitants.entities.concher.ai;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;

public class ConcherAi {
    private final ConcherEntity concher;
    
    private boolean hasShell;
    private float currentWeight;
    private int blinkTimer = 0;

    public enum State {
        IDLE,
        WANDERING,
        PANIC, // for non-shell concher is fleeing, for shell concher is hiding inside its shell
        SLEEPING
    }

    public enum SleepingState {
        GOING_TO_SLEEP,
        SLEEPING,
        WAKING_UP
    }

    public enum GrowthStage {
        STAGE_0, // no-shell
        STAGE_1, // small-shell
        STAGE_2, // medium-shell
        STAGE_3  // large-shell
    }

    public ConcherAi(ConcherEntity concher) {
        this.concher = concher;
    }

    public void registerGoals() {
        
    }

    public void aiStep() {
        this.hasShell = concher.getStage() > 0;
        this.handleBlinking();
    }

    private void handleBlinking() {
        this.blinkTimer--;
        
        if (this.blinkTimer <= 0) {
            this.blinkTimer = concher.getRandom().nextInt(100) + 50; 
            concher.getEntityData().set(ConcherEntity.BLINKING, true);
            // Inhabitants.LOGGER.info("[ConcherAi]: Concher is blinking");

        } else if (concher.getEntityData().get(ConcherEntity.BLINKING) &&
            this.blinkTimer % 10 == 0) {
            
            concher.getEntityData().set(ConcherEntity.BLINKING, false);
        }
    }

    public State getState() {
        return State.values()[concher.getEntityData().get(ConcherEntity.AI_STATE)];
    }

    public void setState(State state) {
        concher.getEntityData().set(ConcherEntity.AI_STATE, state.ordinal());
    }

    public SleepingState getSleepingState() {
        return SleepingState.values()[concher.getEntityData().get(ConcherEntity.SLEEPING_STATE)];
    }

    public void setSleepingState(SleepingState state) {
        concher.getEntityData().set(ConcherEntity.SLEEPING_STATE, state.ordinal());
    }

    public boolean hasShell() {
        return hasShell;
    }

    private float getWeightOfStage(GrowthStage stage) {
        // TODO: weights of stages
        return 0f;
    }
}