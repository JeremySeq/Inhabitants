package com.jeremyseq.inhabitants.entities.dryfang;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class FollowPackGoal extends Goal {
    private final DryfangEntity dryfang;
    private final double speed;
    private final double minDistance; // stop distance
    private final double maxDistance; // start following if beyond this

    public FollowPackGoal(DryfangEntity dryfang, double speed, double minDistance, double maxDistance) {
        this.dryfang = dryfang;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Entity leader = dryfang.getPackLeaderEntity();
        if (leader == null) return false;
        if (dryfang.isLeader()) return false; // leader shouldn't follow itself
        double dist = dryfang.distanceToSqr(leader);
        return dist > (maxDistance * maxDistance) && leader.isAlive();
    }

    @Override
    public void stop() {
        this.dryfang.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        Entity leader = dryfang.getPackLeaderEntity();
        if (leader == null) return false;
        double dist = dryfang.distanceToSqr(leader);
        return dist > (minDistance * minDistance) && leader.isAlive();
    }

    @Override
    public void tick() {
        Entity leader = dryfang.getPackLeaderEntity();
        if (leader == null) return;
        if (!dryfang.getNavigation().isDone()) return;
        dryfang.getNavigation().moveTo(leader, speed);
    }
}
