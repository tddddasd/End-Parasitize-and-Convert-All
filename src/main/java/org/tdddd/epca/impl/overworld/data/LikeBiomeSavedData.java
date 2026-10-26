package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores which 4x4x4 sections carry which like biome, per dimension.
 *
 * <p>26.1.2 saves through a {@link SavedDataType} codec instead of the 1.20.1 {@code CompoundTag} pair, so
 * the map is stored as {@code {"entries": {"<sectionKey>": {...}}}}.</p>
 */
public class LikeBiomeSavedData extends SavedData {

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "like_biomes");

    public static final Codec<LikeBiomeSavedData> CODEC =
            Codec.unboundedMap(Codec.STRING, LikeBiome.CODEC).xmap(
                    map -> {
                        LikeBiomeSavedData data = new LikeBiomeSavedData();
                        map.forEach((key, value) -> {
                            try {
                                data.bySection.put(Long.parseLong(key), value);
                            } catch (NumberFormatException ignored) {
                                // a corrupt key is dropped rather than failing the whole save
                            }
                        });
                        return data;
                    },
                    data -> {
                        Map<String, LikeBiome> map = new HashMap<>();
                        data.bySection.forEach((key, value) -> map.put(Long.toString(key), value));
                        return map;
                    });

    public static final SavedDataType<LikeBiomeSavedData> TYPE =
            new SavedDataType<>(DATA_ID, LikeBiomeSavedData::new, CODEC);

    private final Map<Long, LikeBiome> bySection = new HashMap<>();

    public LikeBiomeSavedData() {
    }

    public static LikeBiomeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public LikeBiome at(BlockPos pos) {
        return this.bySection.get(SectionPos.asLong(pos));
    }

    public void set(long sectionKey, LikeBiome likeBiome) {
        this.bySection.put(sectionKey, likeBiome);
        this.setDirty();
    }

    public boolean remove(long sectionKey) {
        if (this.bySection.remove(sectionKey) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public int removeAll() {
        int size = this.bySection.size();
        if (size > 0) {
            this.bySection.clear();
            this.setDirty();
        }
        return size;
    }

    public int size() {
        return this.bySection.size();
    }

    public Map<Long, LikeBiome> entries() {
        return Map.copyOf(this.bySection);
    }
}
