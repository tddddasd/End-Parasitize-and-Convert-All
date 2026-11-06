package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

public class LikeBiomeSavedData extends SavedData {

    private static final String DATA_NAME = "epca_like_biomes";
    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_CHUNK = "Chunk";
    private static final String KEY_DATA = "Data";

    private final Map<Long, LikeBiome> byChunk = new HashMap<>();

    public static LikeBiomeSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(LikeBiomeSavedData::load, LikeBiomeSavedData::new, DATA_NAME);
    }

    public LikeBiome at(BlockPos pos) {
        return this.byChunk.get(SectionPos.asLong(pos));
    }

    public LikeBiome at(long sectionKey) {
        return this.byChunk.get(sectionKey);
    }

    public void set(long sectionKey, LikeBiome likeBiome) {
        this.byChunk.put(sectionKey, likeBiome);
        this.setDirty();
    }

    public boolean remove(long sectionKey) {
        if (this.byChunk.remove(sectionKey) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public int removeAll() {
        int size = this.byChunk.size();
        if (size > 0) {
            this.byChunk.clear();
            this.setDirty();
        }
        return size;
    }

    public int size() {
        return this.byChunk.size();
    }

    public Map<Long, LikeBiome> entries() {
        return Map.copyOf(this.byChunk);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        this.byChunk.forEach((key, value) -> entries.put(Long.toString(key), value.save()));
        tag.put(KEY_ENTRIES, entries);
        return tag;
    }

    public static LikeBiomeSavedData load(CompoundTag tag) {
        LikeBiomeSavedData data = new LikeBiomeSavedData();
        CompoundTag entries = tag.getCompound(KEY_ENTRIES);
        for (String key : entries.getAllKeys()) {
            try {
                LikeBiome value = LikeBiome.load(entries.getCompound(key));
                if (value != null) {
                    data.byChunk.put(Long.parseLong(key), value);
                }
            } catch (NumberFormatException ignored) {

            }
        }
        return data;
    }
}
