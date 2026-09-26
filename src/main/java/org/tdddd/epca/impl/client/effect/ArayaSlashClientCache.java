package org.tdddd.epca.impl.client.effect;

import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only cache of the slashes the server spawned: one entry per kill of a player by the named
 * Alayavijnana staff.
 *
 * <p>An entry carries the position the slash was cut through and the direction the hit came from; the
 * direction is what makes the 50-degree blade frame (see {@code ArayaSlashRenderer}). The start time is
 * the level game time the server reported, so the growth, the hold and the fade all run on the world
 * clock and stay in step with everything else in the level rather than with the packet's arrival.</p>
 *
 * <p>Entries are removed once they are past {@code SLASH_HOLD_TICKS + SLASH_FADE_TICKS}, which the level
 * tick drives. There is no fade-out entry kept after that: the renderer already fades the whole thing to
 * nothing over the last {@code SLASH_FADE_TICKS}, so keeping it longer would only draw invisible
 * geometry.</p>
 */
public final class ArayaSlashClientCache {

    /** One slash. */
    public static final class Entry {
        /** Centre of the blade. */
        public final Vec3 position;
        /** Horizontal direction the hit came from; the blade's `u` axis is derived from it. */
        public final Vec3 direction;
        /** Level game time in ticks the slash was spawned at. */
        public final float startTime;

        private Entry(Vec3 position, Vec3 direction, float startTime) {
            this.position = position;
            this.direction = direction;
            this.startTime = startTime;
        }
    }

    /** How many slashes may be alive at once; older ones are dropped rather than growing without bound. */
    public static final int MAX_ENTRIES = 32;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    /** Incremented on every rendered frame, which is what makes the frame copy once per frame. */
    private static long frameToken;

    private ArayaSlashClientCache() {
    }

    /** Adds one slash. A malformed or zero-length direction is replaced by a harmless default. */
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

    /** Drops the slashes whose whole life (hold plus fade) is over. */
    public static void clientTick(float gameTime) {
        float lifetime = ArayaConstants.SLASH_HOLD_TICKS + ArayaConstants.SLASH_FADE_TICKS;
        ENTRIES.removeIf(entry -> gameTime - entry.startTime > lifetime);
    }

    /** Drops everything, e.g. when the client level changes. */
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

    /**
     * Returns the token of the current frame.
     *
     * <p>The renderer asks for it once per render pass, and the scene copy uses it to copy the frame at
     * most once: the counter only has to change between two frames, not to mean anything.</p>
     */
    public static long frameToken() {
        return ++frameToken;
    }
}
