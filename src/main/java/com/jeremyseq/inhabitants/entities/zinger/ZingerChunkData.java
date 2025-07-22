package com.jeremyseq.inhabitants.entities.zinger;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ZingerChunkData extends SavedData {
    private static final String NAME = "zinger_chunk_data";
    public final Set<ChunkPos> loadedZingerChunks = new HashSet<>();

    public static ZingerChunkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ZingerChunkData::load,
                ZingerChunkData::new,
                NAME
        );
    }

    public void addZingerChunk(ChunkPos pos) {
        loadedZingerChunks.add(pos);
        setDirty();
    }

    public void removeZingerChunk(ChunkPos pos) {
        loadedZingerChunks.remove(pos);
        setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (ChunkPos pos : loadedZingerChunks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.x);
            entry.putInt("z", pos.z);
            list.add(entry);
        }
        tag.put("chunks", list);
        return tag;
    }

    public static ZingerChunkData load(CompoundTag tag) {
        ZingerChunkData data = new ZingerChunkData();
        ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag entry = (CompoundTag) t;
            int x = entry.getInt("x");
            int z = entry.getInt("z");
            data.loadedZingerChunks.add(new ChunkPos(x, z));
        }
        return data;
    }

    /**
     * Loads Zinger chunks when the world is loaded.
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ZingerChunkData data = ZingerChunkData.get(serverLevel);
            for (ChunkPos pos : data.loadedZingerChunks) {
                serverLevel.setChunkForced(pos.x, pos.z, true);
            }
            Inhabitants.LOGGER.debug("Loaded {} Zinger Chunks", data.loadedZingerChunks.size());
        }
    }
}
