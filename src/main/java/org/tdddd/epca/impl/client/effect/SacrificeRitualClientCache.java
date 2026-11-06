package org.tdddd.epca.impl.client.effect;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SacrificeRitualClientCache {

    public static final int FADE_IN_TICKS = 30;

    public static final int FADE_OUT_TICKS = 60;

    public static final class Entry {

        public final BlockPos center;

        public int remainingTicks;

        public int ageTicks;

        public int absentTicks;

        public boolean present = true;

        private Entry(BlockPos center, int remainingTicks) {
            this.center = center;
            this.remainingTicks = remainingTicks;
        }

        public float fade() {
            float in = Math.min(1.0F, (this.ageTicks + 1.0F) / FADE_IN_TICKS);
            if (this.present) {
                return in;
            }
            float out = 1.0F - (this.absentTicks + 1.0F) / FADE_OUT_TICKS;
            return Math.max(0.0F, Math.min(in, out));
        }
    }

    private static final Map<Long, Entry> ENTRIES = new LinkedHashMap<>();

    private SacrificeRitualClientCache() {
    }

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

                if (entry.absentTicks > FADE_OUT_TICKS + 10) {
                    expired.add(mapEntry.getKey());
                }
            }
        }
        for (Long key : expired) {
            ENTRIES.remove(key);
        }
    }

    public static boolean hasAny() {
        return !ENTRIES.isEmpty();
    }

    public static float strongestFade() {
        float best = 0.0F;
        for (Entry entry : ENTRIES.values()) {
            best = Math.max(best, entry.fade());
        }
        return best;
    }

    public static void forEach(Consumer<Entry> action) {
        for (Entry entry : ENTRIES.values()) {
            action.accept(entry);
        }
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
