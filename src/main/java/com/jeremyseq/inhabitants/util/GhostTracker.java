package com.jeremyseq.inhabitants.util;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class GhostTracker {
    
    // stores entity position, rotation, and camera rotation
    public record GhostState(Vec3 pos, float yRot, float xRot, float camYaw, float camPitch, long time) {}
    
    private static final Map<UUID, LinkedList<GhostState>> history = new HashMap<>();
    
    private static final int maxHistory = 5;
    private static final int recordInterval = 1;
    
    // records history of entity movement and camera movement
    public static void record(Entity entity, boolean cameraMoving, float curCamYaw, float curCamPitch) {
        if (entity.level().isClientSide &&
            entity.level().getGameTime() % recordInterval == 0) {
            
            LinkedList<GhostState> states = history
                .computeIfAbsent(entity.getUUID(), k -> new LinkedList<>());
            
            boolean entityMoved = true;

            // checks if entity moved
            if (!states.isEmpty()) {
                GhostState last = states.getFirst();
                double distSqr = entity.position().distanceToSqr(last.pos);

                double rotDiff = Math
                    .abs(entity.getYRot() - last.yRot) + Math.abs(entity.getXRot() - last.xRot);
                
                if (distSqr < 0.001 && rotDiff < 1.0) entityMoved = false;
            }

            // adds history if entity moved or camera moved
            if (entityMoved || cameraMoving) {
                states.addFirst(new GhostState(
                    entity.position(), 
                    entity.getYRot(), 
                    entity.getXRot(), 
                    curCamYaw, 
                    curCamPitch, 
                    entity.level().getGameTime()));
                
                // removes old history
                if (states.size() > maxHistory) {
                    states.removeLast();
                }

            } else if (!states.isEmpty()) {
                if (entity.level().getGameTime() % 4 == 0) states.removeLast();
            }
        }
    }
    
    public static List<GhostState> getHistory(UUID uuid) {
        List<GhostState> result = history.get(uuid);
        return result != null ? result : Collections.emptyList();
    }

    // cleans up history
    public static void cleanup(long currentTime) {
        history.entrySet().removeIf(entry -> {
            LinkedList<GhostState> states = entry.getValue();
            return states.isEmpty() || (currentTime - states.getFirst().time > 40);
        });
    }
}
