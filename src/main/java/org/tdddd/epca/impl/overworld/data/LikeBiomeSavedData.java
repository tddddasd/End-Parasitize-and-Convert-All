package org.tdddd.epca.impl.overworld.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent storage for like biomes.
 *
 * <p>Entries are keyed by a packed <b>unit</b> key: one unit covers a
 * 4x4x4 block volume (see {@link LikeBiomeManager#unitKey(int, int, int)}).
 * Older save files used 16x16x16 chunk sections, they are migrated on load
 * (version 1 -> version 2).</p>
 *
 * <p>{@code managed} contains the units that were created automatically because the
 * unit contains infested blocks; only those units may be removed automatically.</p>
 */
public class LikeBiomeSavedData extends SavedData {

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "like_biomes");

    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_MANAGED = "Managed";
    private static final String KEY_VERSION = "Version";

    /** 1 = 16x16x16 chunk sections, 2 = 4x4x4 units. */
    public static final int CURRENT_VERSION = 2;

    /** Guards against pathological save files while migrating old section based data. */
    private static final int MAX_MIGRATED_ENTRIES = 262_144;

    /**
     * Current save format. The lazy entry map is stored as a string keyed map because that is what
     * {@code Codec.unboundedMap} supports for NBT; the packed unit key is just its decimal form.
     */
    private static final Codec<LikeBiomeSavedData> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, LikeBiome.CODEC).fieldOf(KEY_ENTRIES)
                    .forGetter(LikeBiomeSavedData::stringEntries),
            Codec.LONG.listOf().optionalFieldOf(KEY_MANAGED, List.of())
                    .forGetter(LikeBiomeSavedData::managedList),
            Codec.INT.optionalFieldOf(KEY_VERSION, CURRENT_VERSION)
                    .forGetter(LikeBiomeSavedData::storedVersion)
    ).apply(instance, LikeBiomeSavedData::fromSerialized));

    /**
     * Version 1 fallback: the whole file used to be a bare map of section key -> like biome. Decoding
     * through that codec keeps such worlds readable and lets {@link #migrateSectionEntry} expand the
     * painted volume into 4x4x4 units.
     */
    private static final Codec<LikeBiomeSavedData> LEGACY_CODEC =
            Codec.unboundedMap(Codec.STRING, LikeBiome.CODEC).xmap(
                    map -> fromSerialized(map, List.of(), 1),
                    LikeBiomeSavedData::stringEntries);

    public static final Codec<LikeBiomeSavedData> CODEC = Codec.either(DATA_CODEC, LEGACY_CODEC)
            .xmap(either -> either.map(data -> data, data -> data), data -> Either.left(data));

    public static final SavedDataType<LikeBiomeSavedData> TYPE =
            new SavedDataType<>(DATA_ID, LikeBiomeSavedData::new, CODEC);

    private final Map<Long, LikeBiome> byUnit = new HashMap<>();
    private final Set<Long> managed = new HashSet<>();

    // Insertion ordered key list so the reconciler can sweep the map without copying it.
    private final List<Long> order = new ArrayList<>();
    private final Map<Long, Integer> orderIndex = new HashMap<>();

    private int version = CURRENT_VERSION;

    public LikeBiomeSavedData() {
    }

    public static LikeBiomeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public LikeBiome at(BlockPos pos) {
        return this.byUnit.get(LikeBiomeManager.unitKey(pos));
    }

    @Nullable
    public LikeBiome atKey(long unitKey) {
        return this.byUnit.get(unitKey);
    }

    public boolean contains(long unitKey) {
        return this.byUnit.containsKey(unitKey);
    }

    /**
     * Stores (or replaces) the like biome of a unit. The managed flag is untouched, callers that
     * generate units automatically must call {@link #setManaged(long, boolean)} explicitly.
     */
    public void set(long unitKey, LikeBiome likeBiome) {
        LikeBiome previous = this.byUnit.put(unitKey, likeBiome);
        if (previous == null) {
            Integer idx = this.orderIndex.get(unitKey);
            if (idx == null) {
                this.orderIndex.put(unitKey, this.order.size());
                this.order.add(unitKey);
            } else {
                this.order.set(idx, unitKey);
            }
        }
        this.setDirty();
    }

    public boolean remove(long unitKey) {
        this.managed.remove(unitKey);
        if (this.byUnit.remove(unitKey) != null) {
            Integer removedIndex = this.orderIndex.remove(unitKey);
            int idx = removedIndex == null ? -1 : removedIndex;
            int lastIndex = this.order.size() - 1;
            if (idx >= 0 && idx <= lastIndex) {
                Long last = this.order.remove(lastIndex);
                if (idx < this.order.size()) {
                    this.order.set(idx, last);
                    this.orderIndex.put(last, idx);
                }
            }
            this.setDirty();
            return true;
        }
        return false;
    }

    public int removeAll() {
        int size = this.byUnit.size();
        if (size > 0) {
            this.byUnit.clear();
            this.managed.clear();
            this.order.clear();
            this.orderIndex.clear();
            this.setDirty();
        }
        return size;
    }

    public int size() {
        return this.byUnit.size();
    }

    /** Unit key at the given index of the insertion ordered key list, or {@code Long.MIN_VALUE}. */
    public long keyAt(int index) {
        if (index < 0 || index >= this.order.size()) {
            return Long.MIN_VALUE;
        }
        return this.order.get(index);
    }

    public Map<Long, LikeBiome> entries() {
        return Map.copyOf(this.byUnit);
    }

    public boolean isManaged(long unitKey) {
        return this.managed.contains(unitKey);
    }

    public void setManaged(long unitKey, boolean value) {
        boolean changed = value ? this.managed.add(unitKey) : this.managed.remove(unitKey);
        if (changed) {
            this.setDirty();
        }
    }

    public int managedCount() {
        return this.managed.size();
    }

    /** Version of the data as read from disk, useful for diagnostics. */
    public int storedVersion() {
        return this.version;
    }

    // ------------------------------------------------------------------
    // serialization
    // ------------------------------------------------------------------

    private Map<String, LikeBiome> stringEntries() {
        Map<String, LikeBiome> map = new HashMap<>();
        this.byUnit.forEach((key, value) -> map.put(Long.toString(key), value));
        return map;
    }

    private List<Long> managedList() {
        return List.copyOf(this.managed);
    }

    private static LikeBiomeSavedData fromSerialized(Map<String, LikeBiome> entries, List<Long> managed,
                                                     int version) {
        LikeBiomeSavedData data = new LikeBiomeSavedData();
        data.version = version;

        for (Map.Entry<String, LikeBiome> entry : entries.entrySet()) {
            long rawKey;
            try {
                rawKey = Long.parseLong(entry.getKey());
            } catch (NumberFormatException ignored) {
                continue;
            }
            LikeBiome value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (version >= 2) {
                data.putLoaded(rawKey, value);
            } else {
                data.migrateSectionEntry(rawKey, value);
            }
        }

        for (Long key : managed) {
            if (key != null && data.byUnit.containsKey(key)) {
                data.managed.add(key);
            }
        }
        return data;
    }

    /**
     * Old save format: one entry per 16x16x16 chunk section. Expand it into the 4x4x4 units it
     * covered so that the painted volume survives the upgrade.
     */
    private void migrateSectionEntry(long sectionKey, LikeBiome value) {
        int baseX = SectionPos.x(sectionKey) << 2;
        int baseY = SectionPos.y(sectionKey) << 2;
        int baseZ = SectionPos.z(sectionKey) << 2;
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dz = 0; dz < 4; dz++) {
                    if (this.byUnit.size() >= MAX_MIGRATED_ENTRIES) {
                        return;
                    }
                    this.putLoaded(LikeBiomeManager.unitKey(baseX + dx, baseY + dy, baseZ + dz), value);
                }
            }
        }
    }

    private void putLoaded(long unitKey, LikeBiome value) {
        this.byUnit.put(unitKey, value);
        this.orderIndex.put(unitKey, this.order.size());
        this.order.add(unitKey);
    }
}
