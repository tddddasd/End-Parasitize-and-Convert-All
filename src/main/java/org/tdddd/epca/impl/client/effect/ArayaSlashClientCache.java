package org.tdddd.epca.impl.client.effect;

import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.ArrayList;
import java.util.List;

public final class ArayaSlashClientCache {

    public static final class Entry {

        public final Vec3 position;

        public final Vec3 direction;

        public final float startTime;

        private Entry(Vec3 position, Vec3 direction, float startTime) {
            this.position = position;
            this.direction = direction;
            this.startTime = startTime;
        }
    }

    public static final int MAX_ENTRIES = 32;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static long frameToken;

    private ArayaSlashClientCache() {
    }

    public static void add(Vec3 position, Vec3 direction, float startTime) {
        Vec3 safeDirection = direction;
        if (safeDirection == null || safeDirection.lengthSqr() < 1.0E-6D) {
            safeDirection = new Vec3(1.0D, 0.0D, 0.0D);
        }
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        ENTRIES.add(new Entry(position, safeDirection, startTime));
    }

    public static void clientTick(float gameTime) {
        float lifetime = ArayaConstants.SLASH_HOLD_TICKS + ArayaConstants.SLASH_FADE_TICKS;
        ENTRIES.removeIf(entry -> gameTime - entry.startTime > lifetime);
    }

    public static void clear() {
        ENTRIES.clear();
        frameToken++;
    }

    public static boolean hasAny() {
        return !ENTRIES.isEmpty();
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static long frameToken() {
        return ++frameToken;
    }
}
