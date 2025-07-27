package com.jeremyseq.inhabitants.entities.zinger;

import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ZingerManager {
    private static final Map<UUID, List<ZingerEntity>> ownedZingers = new HashMap<>();

    public static void cleanupDeadZingers() {
        for (List<ZingerEntity> list : ownedZingers.values()) {
            list.removeIf(z -> !z.isAlive());
        }
        ownedZingers.values().removeIf(List::isEmpty);
    }

    public static void registerZinger(ZingerEntity zinger) {
        cleanupDeadZingers();
        UUID owner = zinger.getOwnerUUID();
        if (owner != null) {
            ownedZingers.computeIfAbsent(owner, k -> new ArrayList<>()).add(zinger);
        }
    }

    public static void unregisterZinger(ZingerEntity zinger) {
        cleanupDeadZingers();
        UUID owner = zinger.getOwnerUUID();
        if (owner != null) {
            List<ZingerEntity> list = ownedZingers.get(owner);
            if (list != null) {
                list.remove(zinger);
                if (list.isEmpty()) {
                    ownedZingers.remove(owner);
                }
            }
        }
    }

    public static List<ZingerEntity> getOwnedZingers(Player player) {
        cleanupDeadZingers();
        return ownedZingers.getOrDefault(player.getUUID(), Collections.emptyList());
    }
}
