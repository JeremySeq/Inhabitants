package com.jeremyseq.inhabitants.entities.boulder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.List;

public class BoulderAreaAttackGoal extends Goal {
    private final PathfinderMob mob;
    private final double range;
    private final int cooldown;
    private final int animationLength;
    private int cooldownTimer = 0;
    private int animationTimer = 0;
    private boolean attacking = false;
    private boolean hitDone = false;
    private final int doDamageTick;

    public BoulderAreaAttackGoal(PathfinderMob mob, double range, int cooldownTicks, int animationTicks, int doDamageTick) {
        this.mob = mob;
        this.range = range;
        this.cooldown = cooldownTicks/2;
        this.animationLength = animationTicks/2;
        this.doDamageTick = doDamageTick/2;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldownTimer > 0 || attacking) {
            cooldownTimer = Math.max(0, cooldownTimer - 1);
            return false;
        }

        List<Player> nearby = mob.level().getEntitiesOfClass(
                Player.class,
                getAttackBox(),
                e -> e.isAlive() && mob.hasLineOfSight(e) && !e.isCreative() && e.distanceTo(mob) <= range - 2
                // little extra buffer here so boulder gets a little closer
        );

        return !nearby.isEmpty();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        attacking = true;
        animationTimer = 0;
        cooldownTimer = cooldown;
        hitDone = false;

        if (mob instanceof BoulderEntity boulder) {
            boulder.triggerAnim("attack", "area");
        }
    }

    @Override
    public void tick() {
        mob.setDeltaMovement(Vec3.ZERO);
        mob.getNavigation().stop();
        mob.setYRot(mob.yBodyRot);

        if (mob.level() instanceof ServerLevel server && animationTimer % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = mob.getRandom().nextDouble() * range * 0.6;
                double px = mob.getX() + Math.cos(angle) * radius;
                double py = mob.getY() + 0.1;
                double pz = mob.getZ() + Math.sin(angle) * radius;
                server.sendParticles(ParticleTypes.POOF, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        if (!hitDone && animationTimer == doDamageTick) {
            performAreaAttack();
            hitDone = true;
        }

        animationTimer++;

        if (animationTimer >= animationLength) {
            attacking = false;
        }
    }

    private void performAreaAttack() {
        if (mob.level() instanceof ServerLevel server) {
            for (int i = 0; i < 50; i++) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = mob.getRandom().nextDouble() * range;
                double px = mob.getX() + Math.cos(angle) * radius;
                double py = mob.getY();
                double pz = mob.getZ() + Math.sin(angle) * radius;
                server.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        List<LivingEntity> targets = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                getAttackBox(),
                e -> e != mob && e.isAlive() && (!(e instanceof BoulderEntity))
        );

        for (LivingEntity target : targets) {
            double distance = mob.distanceTo(target);
            if (distance <= range) {
                target.hurt(mob.damageSources().mobAttack(mob), 8.0F);
                Vec3 knockback = target.position().subtract(mob.position()).normalize().scale(0.8);
                target.push(knockback.x, 0.4, knockback.z);
            }
        }
    }

    private AABB getAttackBox() {
        Vec3 pos = mob.position();
        return new AABB(
                pos.x - range, pos.y - 2, pos.z - range,
                pos.x + range, pos.y + 2, pos.z + range
        );
    }

    @Override
    public boolean canContinueToUse() {
        return attacking;
    }

    @Override
    public void stop() {
        attacking = false;
    }
}
