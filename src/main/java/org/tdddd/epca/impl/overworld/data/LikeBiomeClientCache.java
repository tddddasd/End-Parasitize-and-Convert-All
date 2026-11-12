package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client side mirror of the like biome units (4x4x4 blocks per entry).
 *
 * <p>Lookups happen from the block tint hot path, therefore the last resolved unit is memoised and
 * invalidated through a cheap generation counter whenever the map changes.</p>
 */
public final class LikeBiomeClientCache {

    private static final Map<Long, LikeBiome> BY_UNIT = new HashMap<>();

    @Nullable
    private static Identifier dimension;

    private static long memoKey = Long.MIN_VALUE;

    @Nullable
    private static LikeBiome memoValue;

    private static int memoGeneration = -1;

    private static int generation;

    private LikeBiomeClientCache() {
    }

    /** Full snapshot: replaces everything. Returns the 16x16x16 sections that need a rebuild. */
    public static Set<Long> replaceAll(@Nullable Identifier dim, Map<Long, LikeBiome> entries) {
        boolean switchedDimension = dim != null && !dim.equals(dimension);
        if (switchedDimension) {
            dimension = dim;
            BY_UNIT.clear();
            BY_UNIT.putAll(entries);
            bump();
            // a fresh level is fully rebuilt by the renderer anyway
            return Set.of();
        }
        if (dim != null) {
            dimension = dim;
        }
        Set<Long> dirty = new HashSet<>();
        for (Long key : BY_UNIT.keySet()) {
            addAffectedSections(dirty, key);
        }
        BY_UNIT.clear();
        BY_UNIT.putAll(entries);
        for (Long key : BY_UNIT.keySet()) {
            addAffectedSections(dirty, key);
        }
        bump();
        return dirty;
    }

    /**
     * Incremental update from a batch packet.
     *
     * @return the 16x16x16 sections whose tinted blocks have to be rebuilt
     */
    public static Set<Long> applyBatch(@Nullable Identifier dim, Map<Long, LikeBiome> upserts,
                                       List<Long> removals) {
        if (dim != null && !dim.equals(dimension)) {
            dimension = dim;
            BY_UNIT.clear();
            BY_UNIT.putAll(upserts);
            bump();
            // the whole level was (re)built for this dimension, nothing stale left to repaint
            return Set.of();
        }
        if (dim != null) {
            dimension = dim;
        }
        Set<Long> dirty = new HashSet<>();
        for (Long key : removals) {
            if (BY_UNIT.remove(key) != null) {
                addAffectedSections(dirty, key);
            }
        }
        for (Map.Entry<Long, LikeBiome> entry : upserts.entrySet()) {
            LikeBiome previous = BY_UNIT.put(entry.getKey(), entry.getValue());
            if (!entry.getValue().equals(previous)) {
                addAffectedSections(dirty, entry.getKey());
            }
        }
        if (!dirty.isEmpty()) {
            bump();
        }
        return dirty;
    }

    public static void clear() {
        BY_UNIT.clear();
        dimension = null;
        bump();
    }

    public static void clearIfDimension(@Nullable Identifier current) {
        if (dimension != null && !dimension.equals(current)) {
            clear();
        }
    }

    @Nullable
    public static Identifier dimension() {
        return dimension;
    }

    public static int size() {
        return BY_UNIT.size();
    }

    /**
     * Monotonic counter that is bumped on every change of the unit map. Derived caches (for example
     * the colour transition cache in {@code LikeBiomeClientEffects}) use it as an invalidation
     * stamp instead of being notified unit by unit.
     */
    public static int generation() {
        return generation;
    }

    /**
     * Adds the 16x16x16 section of {@code unitKey} and the sections of its eight horizontal
     * neighbours. The one block colour transition band also repaints the outermost blocks of the
     * neighbouring units, and those may live in a neighbouring chunk section.
     */
    private static void addAffectedSections(Set<Long> dirty, long unitKey) {
        dirty.add(LikeBiomeManager.sectionKeyOfUnit(unitKey));
        int unitX = LikeBiomeManager.unitX(unitKey);
        int unitY = LikeBiomeManager.unitY(unitKey);
        int unitZ = LikeBiomeManager.unitZ(unitKey);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                dirty.add(LikeBiomeManager.sectionKeyOfUnit(
                        LikeBiomeManager.unitKey(unitX + dx, unitY, unitZ + dz)));
            }
        }
    }

    @Nullable
    public static LikeBiome at(BlockPos pos) {
        long key = LikeBiomeManager.unitKey(pos);
        if (memoGeneration == generation && key == memoKey) {
            return memoValue;
        }
        LikeBiome value = BY_UNIT.get(key);
        memoKey = key;
        memoValue = value;
        memoGeneration = generation;
        return value;
    }

    @Nullable
    public static LikeBiome atKey(long unitKey) {
        return BY_UNIT.get(unitKey);
    }

    public static boolean overridesColors(BlockPos pos) {
        LikeBiome like = at(pos);
        return like != null && like.overrideColors();
    }

    @Nullable
    public static Identifier biomeId(BlockPos pos) {
        LikeBiome like = at(pos);
        return like == null ? null : like.biomeId();
    }

    private static void bump() {
        generation++;
        memoKey = Long.MIN_VALUE;
        memoValue = null;
    }
}
