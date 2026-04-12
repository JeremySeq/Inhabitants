package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.recipe.IBogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;

import java.util.List;

public final class BogreSkills {

    public static final CookingSkill COOKING = new CookingSkill();
    public static final CarvingSkill CARVING = new CarvingSkill();
    public static final TransformationSkill TRANSFORMATION = new TransformationSkill();

    public static final List<Skill> ALL = List.of(COOKING, CARVING, TRANSFORMATION);

    private BogreSkills() {}

    public static Skill forType(IBogreRecipe.Type type) {
        return ALL.stream()
        .filter(s -> s.getType() == type)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no skill for type: " + type));
    }

    public static void cancelCurrentSkill(BogreEntity bogre) {
        if (bogre.getAi().getActiveRecipe() == null) return;
        Skill skill = forType(bogre.getAi().getActiveRecipe().getBogreRecipeType());
        skill.cancel(bogre);
    }

    public static abstract class Skill {
        public enum Animation { START, LOOP, END }

        public abstract int getAnimationDuration(Animation animation);
        public abstract IBogreRecipe.Type getType();
        public abstract void aiStep(BogreEntity bogre);

        public abstract void handleMovement(BogreEntity bogre);
        public abstract void handleSkilling(BogreEntity bogre);
        
        public abstract boolean canPerform(BogreEntity bogre);
        public abstract int getDuration(BogreEntity bogre);
        public abstract void cancel(BogreEntity bogre);
        
        public void keyframeTriggered(BogreEntity bogre, String name) {}
        

        protected void finishSkill(BogreEntity bogre) {
            bogre.setAIState(BogreAi.State.NEUTRAL);
            bogre.setCraftingState(BogreAi.SkillingState.NONE);
            bogre.getAi().setActiveRecipe(null);
            bogre.getAi().setPathSet(false);
            bogre.getAi().resetStuckTicks();
            bogre.getAi().setSkillingMoveSide(0);

            // stop animations
            bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);

            if (bogre.level().isClientSide) {
                bogre.isHammerHidden = false;
                bogre.hammerHideTicks = 0;
                bogre.clientSkillHits = 0;
            }
        }
    }
}
