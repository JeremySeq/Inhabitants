package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class FollowPodHolderGoal extends Goal {
    private final GazerEntity gazer;
    private Player target;

    public FollowPodHolderGoal(GazerEntity gazer) {
        this.gazer = gazer;
    }

    @Override
    public boolean canUse() {
        if (gazer.currentState != GazerEntity.GazerState.FOLLOWING_PLAYER) return false;
        if (gazer.podOwner == null) return false;

        target = gazer.level().getPlayerByUUID(gazer.podOwner);
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        if (target != null) {
            gazer.getNavigation().moveTo(target, 1.0D);
        }
    }
}