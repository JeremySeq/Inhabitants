package com.jeremyseq.inhabitants.blocks;

import net.minecraft.core.BlockPos;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GazerPodBlockRegistry {
    public static final Set<BlockPos> PODS = ConcurrentHashMap.newKeySet();

    public static void register(BlockPos pos) {
        PODS.add(pos);
    }

    public static void unregister(BlockPos pos) {
        PODS.remove(pos);
    }

    public static BlockPos getNearestPodPosition(BlockPos pos) {
        return PODS.stream()
            .min(Comparator.comparingDouble(a -> a.distSqr(pos)))
            .orElse(null);
    }

    public static void clear() {
        PODS.clear();
    }
}
