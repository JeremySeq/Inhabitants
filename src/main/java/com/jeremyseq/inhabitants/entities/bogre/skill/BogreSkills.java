package com.jeremyseq.inhabitants.entities.bogre.skill;

import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import java.util.List;

public final class BogreSkills {

    public static final CookingSkill COOKING = new CookingSkill();
    public static final CarvingSkill CARVING = new CarvingSkill();
    public static final TransformationSkill TRANSFORMATION = new TransformationSkill();

    public static final List<Skill> ALL = List.of(COOKING, CARVING, TRANSFORMATION);

    private BogreSkills() {}

    public static Skill forType(BogreRecipe.Type type) {
        return ALL.stream()
        .filter(s -> s.getType() == type)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no skiill for type: " + type));
    }

    public static abstract class Skill {
        public abstract BogreRecipe.Type getType();
        public abstract void aiStep(BogreEntity bogre);
        public abstract boolean canPerform(BogreEntity bogre);
        public abstract int getDuration();

        protected void finishSkill(BogreEntity bogre) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setCraftingState(BogreEntity.CraftingState.NONE);
            bogre.setActiveRecipe(null);
            bogre.setPathSet(false);
            bogre.resetStuckTicks();
        }
    }
}
