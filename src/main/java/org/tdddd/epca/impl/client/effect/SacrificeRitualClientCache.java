package org.tdddd.epca.impl.client.effect;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-only cache of the sacrifice rituals the server reported through
 * {@code SyncRitualAuraPacket}.
 *
 * <p>Batches arrive every {@code BlockConversionManager.RITUAL_SYNC_INTERVAL_TICKS} ticks from
 * {@code BlockConversionManager}; each batch is the complete set for this player, so
 * {@link #applyBatch} replaces the cache. An entry that disappears from a batch starts the
 * fade-out instead of vanishing: the renderer keeps drawing it with shrinking opacity for
 * {@link #FADE_OUT_TICKS} ticks, which is what makes the aura dissolve after the lightning falls
 * rather than popping out.</p>
 *
 * <h2>Timeline of one ritual</h2>
 * <pre>
 *   batch 1 ... batch n   present   : fade in over FADE_IN_TICKS, then fully visible
 *   ritual ends           absent    : fade out over FADE_OUT_TICKS, then dropped
 * </pre>
 *
 * <p>The remaining duration is only used for diagnostics and for the sky ramp; the aura's own clock is
 * the level's game time (see the renderer), so a batch that is late cannot make it stutter.</p>
 */
public final class SacrificeRitualClientCache {

    /** Ticks the aura takes to fade in after a ritual is first seen. */
    public static final int FADE_IN_TICKS = 30;

    /** Ticks the aura takes to fade out after the ritual stopped being reported. */
    public static final int FADE_OUT_TICKS = 60;

    /** One cached ritual. */
    public static final class Entry {
        /** Ritual centre, i.e. the altar block. */
        public final BlockPos center;
        /** Remaining duration last reported by the server, counted down locally. */
        public int remainingTicks;
        /** Client ticks this ritual has been in the cache; drives the fade-in. */
        public int ageTicks;
        /** Client ticks this ritual has been missing from the batches; drives the fade-out. */
        public int absentTicks;
        /** True while the last batch still listed this ritual. */
        public boolean present = true;

        private Entry(BlockPos center, int remainingTicks) {
            this.center = center;
            this.remainingTicks = remainingTicks;
        }

        /** Visibility of this ritual in {@code [0,1]}: fade in first, then fade out. */
        public float fade() {
            float in = Math.min(1.0F, (this.ageTicks + 1.0F) / FADE_IN_TICKS);
            if (this.present) {
                return in;
            }
            float out = 1.0F - (this.absentTicks + 1.0F) / FADE_OUT_TICKS;
            return Math.max(0.0F, Math.min(in, out));
        }
    }

    /** Ritual centre (packed long) -&gt; entry. */
    private static final Map<Long, Entry> ENTRIES = new LinkedHashMap<>();

    private SacrificeRitualClientCache() {
    }

    /** Replaces the whole cache with one batch; entries that disappear start fading out. */
    public static void applyBatch(BlockPos[] centers, int[] remainingTicks) {
        for (Entry entry : ENTRIES.values()) {
            entry.present = false;
        }
        int count = Math.min(centers.length, remainingTicks.length);
        for (int i = 0; i < count; i++) {
            BlockPos center = centers[i];
            if (center == null) {
                continue;
            }
            Entry entry = ENTRIES.get(center.asLong());
            if (entry == null) {
                entry = new Entry(center, remainingTicks[i]);
                ENTRIES.put(center.asLong(), entry);
            } else {
                entry.present = true;
                entry.absentTicks = 0;
                entry.remainingTicks = remainingTicks[i];
            }
        }
    }

    /** Advances every clock; called once per client tick from {@code ClientEvents}. */
    public static void clientTick() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        List<Long> expired = new ArrayList<>();
        for (Map.Entry<Long, Entry> mapEntry : ENTRIES.entrySet()) {
            Entry entry = mapEntry.getValue();
            entry.ageTicks++;
            entry.remainingTicks = Math.max(0, entry.remainingTicks - 1);
            if (!entry.present) {
                entry.absentTicks++;
                // Give the renderer a little longer than the fade itself so a one-tick hiccup in the
                // batches cannot make the aura blink.
                if (entry.absentTicks > FADE_OUT_TICKS + 10) {
                    expired.add(mapEntry.getKey());
                }
            }
        }
        for (Long key : expired) {
            ENTRIES.remove(key);
        }
    }

    /** True while anything has to be drawn, including the fade-out tail. */
    public static boolean hasAny() {
        return !ENTRIES.isEmpty();
    }

    /**
     * Strongest visibility of any ritual, used to drive the purple sky: the sky has to stay as long as
     * the most visible aura does.
     */
    public static float strongestFade() {
        float best = 0.0F;
        for (Entry entry : ENTRIES.values()) {
            best = Math.max(best, entry.fade());
        }
        return best;
    }

    /** Runs the given action for every cached ritual, fade-out tail included. */
    public static void forEach(Consumer<Entry> action) {
        for (Entry entry : ENTRIES.values()) {
            action.accept(entry);
        }
    }

    /** Drops every entry; called when the client level changes. */
    public static void clear() {
        ENTRIES.clear();
    }
}
