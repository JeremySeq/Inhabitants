package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class FollowPodHolderGoal extends Goal {
    GazerEntity gazer;

    public FollowPodHolderGoal(GazerEntity gazer) {
        this.gazer = gazer;
    }

    @Override
    public boolean canUse() {
        return gazer.playingEnterPod;
    }

    @Override
    public void tick() {
        Player player = gazer.level().getPlayerByUUID(gazer.podOwner);
        if (player != null) {
            gazer.getNavigation().moveTo(player, 1.0);
        }
    }
}
