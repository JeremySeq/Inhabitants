package com.jeremyseq.inhabitants.entities;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EntityUtil {
    public static void throwItemStack(Level level, Entity entity, ItemStack stack, float speed, float upwardBias) {
        if (level.isClientSide || stack.isEmpty()) return;

        // spawn the item
        ItemEntity itemEntity = new ItemEntity(
                level,
                entity.getX(),
                entity.getEyeY(),
                entity.getZ(),
                stack.copy()
        );

        // direction in front of the entity
        Vec3 look = entity.getLookAngle().normalize();
        Vec3 motion = look.scale(speed).add(0, upwardBias, 0);
        itemEntity.setDeltaMovement(motion);

        // set pickup delay like a player-thrown item
        itemEntity.setDefaultPickUpDelay();

        level.addFreshEntity(itemEntity);
    }

    /**
     * Standard shockwave effect that damages and knocks back entities around the user.
     *
     * @param shockwave_damage damage at the center of the shockwave
     */
    public static void shockwave(LivingEntity user, double shockwave_radius, float shockwave_damage) {
        AABB shockwaveArea = new AABB(user.getX() - shockwave_radius, user.getY() - 1, user.getZ() - shockwave_radius,
                user.getX() + shockwave_radius, user.getY() + 2, user.getZ() + shockwave_radius);
        List<LivingEntity> affectedEntities = user.level().getEntitiesOfClass(LivingEntity.class, shockwaveArea,
                entity -> !entity.isSpectator() && entity.isAlive());

        for (LivingEntity affected : affectedEntities) {
            if (affected instanceof Player player) {
                if (player.isCreative()) {
                    continue;
                }
            }
            double dx = user.getX() - affected.getX();
            double dz = user.getZ() - affected.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > shockwave_radius) {
                continue;
            }
            if (distance > 0.1) {
                affected.knockback(shockwave_radius, dx / distance, dz / distance);

                // do damage, falling off based on distance
                // this has a max damage of shockwave_damage at the center and falls off to 0 at the edge
                float damage = (float) (shockwave_damage * (1 - Math.min(distance / shockwave_radius, 1)));

                if (user instanceof Player player) {
                    affected.hurt(player.damageSources().playerAttack(player), damage);
                } else {
                    affected.hurt(user.damageSources().mobAttack(user), damage);
                }
            }
        }

        // add visual and sound effects for the shockwave
        if (user.level() instanceof ServerLevel serverLevel) {
            user.playSound(SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);

            // cloud ring
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double px = user.getX() + Math.cos(angle) * shockwave_radius;
                double pz = user.getZ() + Math.sin(angle) * shockwave_radius;
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, user.getY(), pz, 1, 0, 0, 0, 0);
            }

            // crack-like particles around center
            BlockState state = user.getBlockStateOn();
            BlockParticleOption crackParticles = new BlockParticleOption(ParticleTypes.BLOCK, state);
            for (int i = 0; i < 40; i++) {
                double angle = 2 * Math.PI * i / 40;
                double radius = shockwave_radius * (0.3 + 0.7 * Math.random()); // inner to outer ring
                double px = user.getX() + Math.cos(angle) * radius;
                double pz = user.getZ() + Math.sin(angle) * radius;
                double py = user.getY();

                serverLevel.sendParticles(crackParticles, px, py, pz, 5, 0.1, 0.01, 0.1, 0.05);
            }
        }
    }

}
