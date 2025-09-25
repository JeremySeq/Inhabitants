package com.jeremyseq.inhabitants.entities.wishfish;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WishfishFleeGoal extends Goal {
    private final WishfishEntity fish;
    private LivingEntity threat;
    private final double fleeSpeed;

    public WishfishFleeGoal(WishfishEntity fish, double fleeSpeed) {
        this.fish = fish;
        this.fleeSpeed = fleeSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.threat = this.fish.getLastHurtByMob();
        return this.threat != null && this.threat.isAlive();
    }

    @Override
    public void start() {
        this.moveAway();
        this.fish.setSprinting(true);
    }

    @Override
    public void tick() {
        this.moveAway();
    }

    private void moveAway() {
        if (threat == null) return;

        double dx = fish.getX() - threat.getX();
        double dy = fish.getY() - threat.getY();
        double dz = fish.getZ() - threat.getZ();

        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0.0001) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        double targetX = fish.getX() + dx * 5;
        double targetY = fish.getY() + dy * 2;
        double targetZ = fish.getZ() + dz * 5;

        fish.getNavigation().moveTo(targetX, targetY, targetZ, fleeSpeed);
    }

    @Override
    public boolean canContinueToUse() {
        return this.threat != null && this.threat.isAlive() && this.fish.distanceTo(this.threat) < 12.0;
    }

    @Override
    public void stop() {
        this.threat = null;
        this.fish.setSprinting(false);
    }
}
