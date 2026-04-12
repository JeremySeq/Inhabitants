package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.networking.ScreenShakePacketS2C;
import com.jeremyseq.inhabitants.networking.bogre.ShockwaveParticlePacketS2C;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShockwaveGoal {

    private static final List<ActiveShockwave> activeShockwaves = new ArrayList<>();

    /**
     * Creates shockwave effect that damages and knocks back entities around the center point. Damage falls off with distance.
     * @param center center of shockwave
     * @param damage damage at center of shockwave
     * @param radius shockwave max radius
     * @param lifetime shockwave duration in ticks
     */
    public static void addShockwave(ServerLevel level, Vec3 center, float damage, float radius,
    int lifetime, LivingEntity owner) {
        activeShockwaves.add(new ActiveShockwave(level, center, damage, radius, lifetime, owner));

        level.playSound(null, center.x, center.y, center.z, ModSoundEvents.SHOCKWAVE.get(),
        SoundSource.HOSTILE, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.5D, 0.5D, 0.5D, 0.0D);

        Vec3 shockwavePos = new Vec3(center.x, center.y + 0.1, center.z);
        // send shockwave particles to players within 30 blocks
        List<ServerPlayer> players = level.getPlayers(player -> player.distanceToSqr(center) <= 900);
        for (ServerPlayer player : players) {
            ModNetworking.sendToPlayer(new ShockwaveParticlePacketS2C(shockwavePos, lifetime, radius), player);
            ModNetworking.sendToPlayer(new ScreenShakePacketS2C(30), player);
        }

        // send screen shake to players within 10 blocks
        players = level.getPlayers(player -> player.distanceToSqr(center) <= radius*radius);
        for (ServerPlayer player : players) {
            ModNetworking.sendToPlayer(new ScreenShakePacketS2C(30), player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !activeShockwaves.isEmpty()) {
            Iterator<ActiveShockwave> iterator = activeShockwaves.iterator();
            
            while (iterator.hasNext()) {
                ActiveShockwave shockwave = iterator.next();
                shockwave.tick();
                if (shockwave.isFinished()) {
                    iterator.remove();
                }
            }
        }
    }

    private static class ActiveShockwave {
        private final ServerLevel level;
        private final Vec3 center;
        private final float radius;
        private final int lifetime;
        private int age = 0;

        private final List<LivingEntity> hitEntities = new ArrayList<>();
        private final float damage;
        private final LivingEntity owner;

        public ActiveShockwave(ServerLevel level, Vec3 center, float damage, float radius, int lifetime, LivingEntity owner) {
            this.level = level;
            this.center = center;
            this.damage = damage;
            this.radius = radius;
            this.lifetime = lifetime;
            this.owner = owner;
        }

        public void tick() {
            this.age++;
            
            float progress = (float) this.age / (float) this.lifetime;
            float currentRadius = 0.1f + (this.radius * (float) Math.sin(progress * Math.PI / 2.0));
            
            float innerRadius = currentRadius - 0.5f;
            float outerRadius = currentRadius + 0.5f;
            
            AABB searchBox = new AABB(
                center.x - outerRadius, center.y - 1.0, center.z - outerRadius,
                center.x + outerRadius, center.y + 4.0, center.z + outerRadius
            );

            double innerSq = innerRadius * innerRadius;
            double outerSq = outerRadius * outerRadius;
            
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
                if (hitEntities.contains(entity) || entity == owner) return false;

                // allowing jumping to avoid the shockwave
                if (!entity.onGround()) return false;

                double distanceSq = entity.distanceToSqr(center.x, entity.getY(), center.z);
                return distanceSq >= innerSq && distanceSq <= outerSq;
            });

            for (LivingEntity entity : nearbyEntities) {
                hitEntities.add(entity);
                
                double deltaX = entity.getX() - center.x;
                double deltaZ = entity.getZ() - center.z;
                
                double distanceSq = entity.distanceToSqr(center.x, entity.getY(), center.z);
                double distance = Math.sqrt(distanceSq);
                if (distance > 0.1) {
                    double strength = Math.max(0, (radius - distance) / radius); // prevents negative knockback
                    if (strength > 0) {
                        entity.knockback(strength * 3.0, -deltaX / distance, -deltaZ / distance);
                    }
                    
                    // apply damage based on distance from center

                    float falloffDamage = (float) (this.damage * (1.0 - Math.min(distance / radius, 1.0)));
                    entity.hurt(entity.damageSources().mobAttack(owner != null ? owner : entity), falloffDamage);

                    // stop blocking for 5 seconds if using a shield
                    if (entity instanceof ServerPlayer player && player.isBlocking()) {
                        ItemStack stack = player.getUseItem();
                        if (stack.canPerformAction(ToolActions.SHIELD_BLOCK)) {
                            player.stopUsingItem();
                            player.getCooldowns().addCooldown(stack.getItem(), 100);
                        }
                    }
                }
            }
        }

        public boolean isFinished() {
            return this.age >= this.lifetime;
        }
    }
}
