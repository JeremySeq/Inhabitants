package com.jeremyseq.inhabitants.entities.dryfang;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Comparator;
import java.util.List;

public class DryfangAttackRivalGoal extends Goal {
    private final DryfangEntity dryfang;
    private DryfangEntity targetDryfang;
    private static final double PACK_RADIUS = 8.0D;

    public DryfangAttackRivalGoal(DryfangEntity dryfang) {
        this.dryfang = dryfang;
    }

    @Override
    public boolean canUse() {
        if (dryfang.getPackLeaderEntity() == null) {
            return false;
        }

        // attack nearest leader who is not your leader
        List<DryfangEntity> nearbyRivals = dryfang.level().getEntitiesOfClass(
                DryfangEntity.class,
                dryfang.getBoundingBox().inflate(PACK_RADIUS),
                e -> e.getPackLeaderId() != dryfang.getPackLeaderId() && e.getPackLeaderId() >= 0
        );

        List<DryfangEntity> sorted = nearbyRivals.stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(dryfang)))
                .toList();

        if (!sorted.isEmpty()) {
            targetDryfang = sorted.get(0);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetDryfang.getPackLeaderId() != dryfang.getPackLeaderId() && targetDryfang.getPackLeaderId() >= 0;
    }

    @Override
    public void start() {
        this.dryfang.setTarget(targetDryfang);
        super.start();
    }

    @Override
    public void stop() {
        this.dryfang.setTarget(null);
        this.targetDryfang = null;
        super.stop();
    }
}
