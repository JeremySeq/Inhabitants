package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class ImpalerScreamGoal extends Goal {
    private final ImpalerEntity mob;
    private int screamTimer = 0;

    public ImpalerScreamGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && mob.screamCooldown == 0
                && mob.getTarget().distanceToSqr(mob) <= 25;
    }

    @Override
    public boolean canContinueToUse() {
        return screamTimer <= 20;
    }

    @Override
    public void start() {
        screamTimer = 0;
        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }
        mob.triggerAnim("scream", "scream");
    }

    @Override
    public void stop() {
        mob.screamCooldown = ImpalerEntity.SCREAM_COOLDOWN;
    }

    @Override
    public void tick() {
        screamTimer++;

        if (screamTimer == 6) {
            // trigger client stuff
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, false);
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, true);

            EntityUtil.shockwave(mob, 10, 10);
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 10f, 0.9F);
            // give nearby players the concussion effect
            double radius = 15.0D;
            List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius));
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 200, 0));
            }
        }
    }
}
