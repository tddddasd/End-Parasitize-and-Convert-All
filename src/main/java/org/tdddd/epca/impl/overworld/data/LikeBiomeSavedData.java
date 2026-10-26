package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores which chunk units carry which like biome, per dimension.
 *
 * <p>The unit is one chunk column: that keeps the data small and makes "remove this chunk" a single key
 * lookup, while still matching how the client resolves biome colours per block position.</p>
 */
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

    /** The like biome of the chunk containing {@code pos}, or {@code null}. */
    public LikeBiome at(BlockPos pos) {
        return this.byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    public LikeBiome at(ChunkPos pos) {
        return this.byChunk.get(pos.toLong());
    }

    public void set(ChunkPos pos, LikeBiome likeBiome) {
        this.byChunk.put(pos.toLong(), likeBiome);
        this.setDirty();
    }

    /** @return true when a like biome was actually removed */
    public boolean remove(ChunkPos pos) {
        if (this.byChunk.remove(pos.toLong()) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    /** @return how many chunks were cleared */
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
                // a corrupt key is dropped rather than failing the whole save
            }
        }
        return data;
    }
}
