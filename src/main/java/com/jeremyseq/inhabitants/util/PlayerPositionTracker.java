package com.jeremyseq.inhabitants.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlayerPositionTracker {
    private static final int maxTicks = 1200;
    private static final Map<UUID, Deque<PositionRecord>> playerHistory = new HashMap<>();

    public record PositionRecord(Vec3 pos, ResourceKey<Level> dimension, float yRot, float xRot) {}

    public static void track(Player player) {
        
        if (player.level().isClientSide) return;

        playerHistory.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>(maxTicks));
        Deque<PositionRecord> history = playerHistory.get(player.getUUID());

        history.addLast(new PositionRecord(
                player.position(),
                player.level().dimension(),
                player.getYRot(),
                player.getXRot()
        ));

        if (history.size() > maxTicks) {
            history.removeFirst();
        }
    }

    public static PositionRecord getPastPosition(Player player) {
        Deque<PositionRecord> history = playerHistory.get(player.getUUID());
        if (history == null || history.isEmpty()) return null;
        return history.peekFirst();
    }

    public static void cleanup(UUID playerUUID) {
        playerHistory.remove(playerUUID);
    }
}
