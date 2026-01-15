package com.jeremyseq.inhabitants.entities;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
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
                (type, level, reason, pos, random) -> pos.getY() < 45 && level.getBrightness(LightLayer.BLOCK, pos) < 8,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );

    }
}
