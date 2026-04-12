package com.jeremyseq.inhabitants.entities.bogre.render;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.particles.ModParticles;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RoarEffectRenderer {

    private static final List<ActiveRoarEffect> activeRoars = new ArrayList<>();
    private static final List<ActiveRoarEffect> clientActiveRoars = new ArrayList<>();

    public static void addRoar(Level level, Vec3 sourcePos, Vec3 lookDirection, int durationTicks,
            double speedMultiplier) {
        if (level instanceof ServerLevel serverLevel) {
            activeRoars.add(new ActiveRoarEffect(serverLevel, sourcePos,
            lookDirection, durationTicks, speedMultiplier));

        } else if (level.isClientSide) {
            clientActiveRoars.add(new ActiveRoarEffect(level, sourcePos, lookDirection,
            durationTicks, speedMultiplier));
        }
    }

    public static void addRoar(Entity entity, int durationTicks, double speedMultiplier) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            activeRoars.add(new ActiveRoarEffect(serverLevel, entity, durationTicks, speedMultiplier));

        } else if (entity.level().isClientSide) {
            clientActiveRoars.add(new ActiveRoarEffect(entity.level(), entity, durationTicks, speedMultiplier));
        }
    }
    
    public static void spawnRoarParticles(Level level, Vec3 sourcePos, Vec3 lookDirection,
            double speedMultiplier) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ModParticles.ROAR_EFFECT.get(),
                sourcePos.x, sourcePos.y, sourcePos.z,
                0,
                lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier,
                lookDirection.z * speedMultiplier,
                1.0D);
        } else if (level.isClientSide) {
            level.addParticle(ModParticles.ROAR_EFFECT.get(),
                sourcePos.x, sourcePos.y, sourcePos.z,
                lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier,
                lookDirection.z * speedMultiplier);
        }
    }
    
    public static void spawnSonicWaveParticles(Level level, Vec3 sourcePos, Vec3 lookDirection,
            double speedMultiplier) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ModParticles.SONIC_WAVE.get(),
                sourcePos.x, sourcePos.y, sourcePos.z,
                0,
                lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier,
                lookDirection.z * speedMultiplier,
                1.0D);
        } else if (level.isClientSide) {
            level.addParticle(ModParticles.SONIC_WAVE.get(),
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    lookDirection.x * speedMultiplier,
                    lookDirection.y * speedMultiplier,
                    lookDirection.z * speedMultiplier);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !activeRoars.isEmpty()) {
            Iterator<ActiveRoarEffect> iterator = activeRoars.iterator();
            onTick(iterator);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !clientActiveRoars.isEmpty()) {
            Iterator<ActiveRoarEffect> iterator = clientActiveRoars.iterator();
            onTick(iterator);
        }
    }

    private static void onTick(Iterator<ActiveRoarEffect> iterator)
    {
        while (iterator.hasNext()) {
            ActiveRoarEffect roar = iterator.next();
            roar.tick();
            if (roar.isFinished()) {
                iterator.remove();
            }
        }
    }

    private static class ActiveRoarEffect {
        private final Level level;
        private Vec3 sourcePos;
        private Vec3 lookDirection;
        private final Entity entity;
        private final double speedMultiplier;
        private int ticksAlive;
        private final int maxDuration;

        public ActiveRoarEffect(Level level, Vec3 sourcePos, Vec3 lookDirection, int durationTicks,
                double speedMultiplier) {
            this.level = level;
            this.sourcePos = sourcePos;
            this.lookDirection = lookDirection;
            this.entity = null;
            this.maxDuration = durationTicks;
            this.speedMultiplier = speedMultiplier;
            this.ticksAlive = 0;
        }

        public ActiveRoarEffect(Level level, Entity entity, int durationTicks,
                double speedMultiplier) {
            this.level = level;
            this.entity = entity;
            this.maxDuration = durationTicks;
            this.speedMultiplier = speedMultiplier;
            this.ticksAlive = 0;
            updateTracking();
        }

        private void updateTracking() {
            if (entity instanceof BogreEntity bogre) {
                this.sourcePos = bogre.getMouthPos();
                this.lookDirection = bogre.getMouthDirection();
            } else if (entity != null) {
                this.sourcePos = entity.getEyePosition();
                this.lookDirection = entity.getLookAngle();
            }
        }

        public void tick() {
            this.ticksAlive++;
            if (entity != null) {
                updateTracking();
            }

            if (this.ticksAlive % 3 == 0) {
                sendParticles(ModParticles.ROAR_EFFECT.get());
            }

            if (this.ticksAlive % 6 == 0) {
                sendParticles(ModParticles.SONIC_WAVE.get());
            }
        }

        private void sendParticles(ParticleOptions particleOptions) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    particleOptions,
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    0,
                    lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier,
                    lookDirection.z * speedMultiplier,
                    1.0D);
            } else if (level.isClientSide) {
                level.addParticle(particleOptions,
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier,
                    lookDirection.z * speedMultiplier);
            }
        }

        public boolean isFinished() {
            if (entity != null && !entity.isAlive()) return true;
            return this.ticksAlive >= this.maxDuration;
        }
    }
}
