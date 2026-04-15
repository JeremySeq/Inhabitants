package com.jeremyseq.inhabitants.entities;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;

import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobSpawning {
    @SubscribeEvent
    public static void entitySpawnRestriction(SpawnPlacementRegisterEvent event) {
        event.register(
            ModEntities.WARPED_CLAM.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, level, spawnType, pos, random) -> true,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
            ModEntities.IMPALER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() < 45 
                    && level.getBrightness(LightLayer.BLOCK, pos) < 8
                    && (isFlat(level) ? level.getMaxLocalRawBrightness(pos) < 8 : true)
                    && spawnChance(level, random, "impaler")
                    && !level.getBiome(pos).is(Biomes.DEEP_DARK)
                    && !level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS),
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );

    }

    private static boolean isFlat(LevelAccessor level) {
        if (level.getLevelData() instanceof WorldData worldData && worldData.isFlatWorld()) {
            return true;
        }
        return level.getChunkSource() instanceof ServerChunkCache scc &&
            scc.getGenerator() instanceof FlatLevelSource;
    }

    // can be used in the future for new mobs :P
    private static boolean spawnChance(LevelAccessor level, RandomSource random, String mob) {
        if ("impaler".equals(mob)) {
            if (isFlat(level)) {
                return random.nextFloat() < 0.5f; //50%
            }
            return random.nextFloat() < 0.95f; // 95%
        }

        return false; // which is neva eva
    }
}
