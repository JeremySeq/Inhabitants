package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.particles.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RoarEffectManager {

    private static final List<ActiveRoarEffect> activeRoars = new ArrayList<>();

    public static void addRoar(ServerLevel level, Vec3 sourcePos, Vec3 lookDirection, int durationTicks, double speedMultiplier) {
        activeRoars.add(new ActiveRoarEffect(level, sourcePos, lookDirection, durationTicks, speedMultiplier));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !activeRoars.isEmpty()) {
            Iterator<ActiveRoarEffect> iterator = activeRoars.iterator();
            while (iterator.hasNext()) {
                ActiveRoarEffect roar = iterator.next();
                roar.tick();
                if (roar.isFinished()) {
                    iterator.remove();
                }
            }
        }
    }

    private static class ActiveRoarEffect {
        private final ServerLevel level;
        private final Vec3 sourcePos; 
        private final Vec3 lookDirection; 
        private final double speedMultiplier; 
        private int ticksAlive;
        private final int maxDuration;

        public ActiveRoarEffect(ServerLevel level, Vec3 sourcePos, Vec3 lookDirection, int durationTicks, double speedMultiplier) {
            this.level = level;
            this.sourcePos = sourcePos;
            this.lookDirection = lookDirection;
            this.maxDuration = durationTicks;
            this.speedMultiplier = speedMultiplier;
            this.ticksAlive = 0;
        }

        public void tick() {
            this.ticksAlive++;

            if (this.ticksAlive % 3 == 0) {
                level.sendParticles(
                        ModParticles.ROAR_EFFECT.get(), 
                        sourcePos.x, sourcePos.y, sourcePos.z, 
                        0, 
                        lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier, lookDirection.z * speedMultiplier, 
                        1.0D
                );
            }

            if (this.ticksAlive % 6 == 0) {
                level.sendParticles(
                        ModParticles.SONIC_WAVE.get(), 
                        sourcePos.x, sourcePos.y, sourcePos.z, 
                        0, 
                        lookDirection.x * speedMultiplier, lookDirection.y * speedMultiplier, lookDirection.z * speedMultiplier, 
                        1.0D
                );
            }
        }

        public boolean isFinished() {
            return this.ticksAlive >= this.maxDuration;
        }
    }
}
