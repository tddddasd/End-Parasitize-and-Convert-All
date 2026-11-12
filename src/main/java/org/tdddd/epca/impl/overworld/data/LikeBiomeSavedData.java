package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
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

    private static final String DATA_NAME = "epca_like_biomes";
    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_MANAGED = "Managed";
    private static final String KEY_VERSION = "Version";

    /** 1 = 16x16x16 chunk sections, 2 = 4x4x4 units. */
    public static final int CURRENT_VERSION = 2;

    /** Guards against pathological save files while migrating old section based data. */
    private static final int MAX_MIGRATED_ENTRIES = 262_144;

    private final Map<Long, LikeBiome> byUnit = new HashMap<>();
    private final Set<Long> managed = new HashSet<>();

    // Insertion ordered key list so the reconciler can sweep the map without copying it.
    private final List<Long> order = new ArrayList<>();
    private final Map<Long, Integer> orderIndex = new HashMap<>();

    private int version = CURRENT_VERSION;

    public static LikeBiomeSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(LikeBiomeSavedData::load, LikeBiomeSavedData::new, DATA_NAME);
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

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        this.byUnit.forEach((key, value) -> entries.put(Long.toString(key), value.save()));
        tag.put(KEY_ENTRIES, entries);

        long[] managedKeys = new long[this.managed.size()];
        int i = 0;
        for (Long key : this.managed) {
            managedKeys[i++] = key;
        }
        tag.put(KEY_MANAGED, new LongArrayTag(managedKeys));
        tag.putInt(KEY_VERSION, CURRENT_VERSION);
        return tag;
    }

    public static LikeBiomeSavedData load(CompoundTag tag) {
        LikeBiomeSavedData data = new LikeBiomeSavedData();
        CompoundTag entries = tag.getCompound(KEY_ENTRIES);
        int storedVersion = tag.contains(KEY_VERSION, Tag.TAG_INT) ? tag.getInt(KEY_VERSION) : 1;
        data.version = storedVersion;

        for (String key : entries.getAllKeys()) {
            long rawKey;
            try {
                rawKey = Long.parseLong(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            LikeBiome value = LikeBiome.load(entries.getCompound(key));
            if (value == null) {
                continue;
            }
            if (storedVersion >= 2) {
                data.putLoaded(rawKey, value);
            } else {
                data.migrateSectionEntry(rawKey, value);
            }
        }

        if (tag.contains(KEY_MANAGED, Tag.TAG_LONG_ARRAY)) {
            for (long key : tag.getLongArray(KEY_MANAGED)) {
                if (data.byUnit.containsKey(key)) {
                    data.managed.add(key);
                }
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

    /** Version of the data as read from disk, useful for diagnostics. */
    public int storedVersion() {
        return this.version;
    }
}
