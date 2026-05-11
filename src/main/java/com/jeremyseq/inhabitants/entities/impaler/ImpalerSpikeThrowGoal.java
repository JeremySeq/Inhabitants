package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.impaler.spike.ImpalerSpikeProjectile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ImpalerSpikeThrowGoal extends Goal {
    private final ImpalerEntity mob;
    private int timer = 0;
    private static final int DURATION = 40;
    private static final int THROW_TICK = 15;
    private static final int COOLDOWN_TICKS = 100;
    private long lastUsedTick = 0;

    public ImpalerSpikeThrowGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && mob.getTarget().distanceToSqr(mob) <= 36 && mob.getTarget().distanceToSqr(mob) >= 16 && mob.level().getGameTime() - lastUsedTick >= COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        return timer <= DURATION;
    }

    @Override
    public void start() {
        timer = 0;
        mob.triggerAnim("spike throw", "spike throw");
    }

    @Override
    public void stop() {
        lastUsedTick = mob.level().getGameTime();
        super.stop();
    }

    @Override
    public void tick() {
        timer++;

        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);

        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }

        if (timer == THROW_TICK) {
            throwSpikes();
        }
    }

    private void throwSpikes() {
//        if (mob.getTarget() == null) return;

        // throw line of spikes towards target
        for (int i = 0; i<3; i++) {
            ImpalerSpikeProjectile spike = new ImpalerSpikeProjectile(ModEntities.IMPALER_SPIKE_PROJECTILE.get(), mob, mob.level());

            // get offsets for spike position based on mob's rotation
            double angle = Math.toRadians(mob.getYRot() + 90);
            double radius = 1;
            double offsetX = Math.cos(angle) * radius + (double) i /2;
            double offsetZ = Math.sin(angle) * radius + (double) i /2;

            spike.setPos(mob.getX() + offsetX, mob.getY(0.5), mob.getZ() + offsetZ);

            // shoot directly outwards
            double dx = Math.cos(angle) * 1.5;
            double dy = 0;
            double dz = Math.sin(angle) * 1.5;

            double speed = 1.0;
            spike.setDeltaMovement(dx * speed, dy * speed, dz * speed);
            mob.level().addFreshEntity(spike);
        }
    }
}
