package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class ScreamGoal extends Goal {
    private final ImpalerEntity mob;
    private int screamTimer = 0;

    public ScreamGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !mob.screamed && mob.getHealth() <= mob.getAttributeValue(Attributes.MAX_HEALTH)/2;
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
        mob.screamed = true;
    }

    @Override
    public void tick() {
        screamTimer++;

        if (screamTimer == 6) {
            // trigger client stuff
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, false);
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, true);

            Inhabitants.LOGGER.debug("TRIGGER SCREAM");
            EntityUtil.shockwave(mob, 10, 10);
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 10f, 0.9F);
            // give nearby players the concussion effect
            double radius = 15.0D;
            List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius));
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 300, 0));
            }
        }
    }
}
