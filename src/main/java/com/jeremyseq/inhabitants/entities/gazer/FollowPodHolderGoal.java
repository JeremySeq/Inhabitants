package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

public class FollowPodHolderGoal extends Goal {
    GazerEntity gazer;
    private Player targetPlayer;
    private Path path;

    public FollowPodHolderGoal(GazerEntity gazer) {
        this.gazer = gazer;
    }

    @Override
    public boolean canUse() {
        if (!gazer.isEnteringPod() || gazer.getOwnerUUID() == null) return false;
        Player player = gazer.level().getPlayerByUUID(gazer.getOwnerUUID());
        if (player == null) return false;
        this.targetPlayer = player;
        return true;
    }

    @Override
    public void start() {
        if (targetPlayer != null) {
            PathNavigation navigation = gazer.getNavigation();
            this.path = navigation.createPath(targetPlayer, 0);
            if (this.path != null) {
                navigation.moveTo(this.path, 1.0);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return gazer.isEnteringPod() && targetPlayer != null && !gazer.getNavigation().isDone();
    }

    @Override
    public void stop() {
        gazer.getNavigation().stop();
        targetPlayer = null;
        path = null;
    }
}
