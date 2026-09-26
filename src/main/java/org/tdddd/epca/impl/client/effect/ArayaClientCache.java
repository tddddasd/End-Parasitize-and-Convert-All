package org.tdddd.epca.impl.client.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-only cache of the "Alayavijnana / Araya staff" aura: which players currently carry a staff
 * whose 天杀 counter reached {@code ArayaConstants.TIANSA_THRESHOLD}, and which render-only fire blocks
 * the server put around them.
 *
 * <p>Everything here is fed by two server batches ({@code SyncArayaAuraPacket},
 * {@code SyncArayaFirePacket}) and the client only ever reads it. The server owns <i>where</i> and
 * <i>when</i>, exactly like the sacrifice-ritual cache next to this class: a holder that disappears
 * from a batch is dropped, and a fire block carries the absolute server tick it was rolled at, so every
 * client sees it go out at the same moment instead of at its own local offset.</p>
 *
 * <h2>Why the aura and the fire are two caches and not one</h2>
 * <p>The aura is needed every frame (it drives the BGM and the slash is spawned by it) and is tiny; the
 * fire field changes only on a {@code FIRE_REROLL_TICKS} boundary and is much larger. Two batches keep
 * the per-tick cost of the aura at one small packet while the fire field is only resent when it really
 * changes.</p>
 */
public final class ArayaClientCache {

    private ArayaClientCache() {
    }

    /** One player that currently has an active aura. */
    public static final class Holder {
        private final int entityId;
        private Vec3 position;
        private int ageTicks;

        private Holder(int entityId, Vec3 position) {
            this.entityId = entityId;
            this.position = position;
        }

        public int entityId() {
            return this.entityId;
        }

        public Vec3 position() {
            return this.position;
        }

        /** Ticks this holder has been in the cache; used for the aura's fade-in. */
        public int ageTicks() {
            return this.ageTicks;
        }

        /** Visibility of the aura in {@code [0,1]}, ramped in like the ritual aura is. */
        public float fade() {
            return Math.min(1.0F, (this.ageTicks + 1.0F) / FADE_IN_TICKS);
        }
    }

    /** One render-only fire block. */
    public static final class Fire {
        private final BlockPos position;
        /** Absolute game tick the server rolled this fire at. */
        private final long rolledAtTick;
        /** Lifetime in ticks, from the server. */
        private final int lifetimeTicks;
        /** Height in blocks, from the server. */
        private final float height;
        /** Local age, used only while {@link #rolledAtTick} has not been reached yet. */
        private int localAgeTicks;

        private Fire(BlockPos position, long rolledAtTick, int lifetimeTicks, float height) {
            this.position = position;
            this.rolledAtTick = rolledAtTick;
            this.lifetimeTicks = lifetimeTicks;
            this.height = height;
        }

        public BlockPos position() {
            return this.position;
        }

        public float height() {
            return this.height;
        }

        /** Age in ticks, counting from the roll. */
        public float ageTicks(long gameTime) {
            if (this.rolledAtTick > 0L && gameTime >= this.rolledAtTick) {
                return (float) (gameTime - this.rolledAtTick);
            }
            return this.localAgeTicks;
        }

        /** Remaining lifetime in ticks, never negative. */
        public float remainingTicks(long gameTime) {
            return Math.max(0.0F, this.lifetimeTicks - ageTicks(gameTime));
        }

        /** True once the lifetime is over and the fire must be removed. */
        public boolean expired(long gameTime) {
            return remainingTicks(gameTime) <= 0.0F;
        }

        /**
         * Opacity of the fire in {@code [0,1]}: it ramps in over {@link #FADE_TICKS} so a re-rolled
         * field does not pop, holds, and ramps out over the last {@link #FADE_TICKS} of its life.
         */
        public float opacity(long gameTime) {
            float age = ageTicks(gameTime);
            float remaining = remainingTicks(gameTime);
            float in = Math.min(1.0F, (age + 1.0F) / FADE_TICKS);
            float out = Math.min(1.0F, (remaining + 1.0F) / FADE_TICKS);
            return Math.max(0.0F, Math.min(in, out));
        }
    }

    /** Ticks the aura takes to fade in after a holder is first reported. */
    public static final int FADE_IN_TICKS = 20;

    /** Ticks the fire takes to fade in and to fade out again. */
    public static final int FADE_TICKS = 20;

    private static final List<Holder> HOLDERS = new ArrayList<>();
    private static final List<Fire> FIRES = new ArrayList<>();

    /** Replaces the whole holder set with one batch; ids that disappeared are dropped. */
    public static void applyHolders(int[] entityIds, double[] positions) {
        int count = Math.min(entityIds.length, positions.length / 3);
        List<Holder> next = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int id = entityIds[i];
            Vec3 position = new Vec3(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]);
            Holder existing = findHolder(id);
            if (existing != null) {
                existing.position = position;
                next.add(existing);
            } else {
                next.add(new Holder(id, position));
            }
        }
        HOLDERS.clear();
        HOLDERS.addAll(next);
    }

    /** Replaces the whole fire set with one batch. All fires of a batch share one roll tick. */
    public static void applyFires(int[] xs, int[] ys, int[] zs, long rolledAtTick, int[] lifetimes,
                                  float[] heights) {
        int count = Math.min(Math.min(xs.length, ys.length), Math.min(zs.length, lifetimes.length));
        count = Math.min(count, heights.length);
        List<Fire> next = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Fire existing = findFire(xs[i], ys[i], zs[i]);
            if (existing != null && existing.rolledAtTick == rolledAtTick
                    && existing.lifetimeTicks == lifetimes[i]) {
                // Same roll: keep the object so its fade-in age survives the resend.
                next.add(existing);
            } else {
                next.add(new Fire(new BlockPos(xs[i], ys[i], zs[i]), rolledAtTick, lifetimes[i],
                        heights[i]));
            }
        }
        FIRES.clear();
        FIRES.addAll(next);
    }

    /** Advances the local clocks and drops expired fire blocks. */
    public static void clientTick(long gameTime) {
        for (Holder holder : HOLDERS) {
            holder.ageTicks++;
        }
        Iterator<Fire> iterator = FIRES.iterator();
        while (iterator.hasNext()) {
            Fire fire = iterator.next();
            fire.localAgeTicks++;
            if (fire.expired(gameTime)) {
                iterator.remove();
            }
        }
    }

    /** Drops everything, e.g. when the client level changes. */
    public static void clear() {
        HOLDERS.clear();
        FIRES.clear();
    }

    public static boolean hasHolders() {
        return !HOLDERS.isEmpty();
    }

    public static boolean hasFires() {
        return !FIRES.isEmpty();
    }

    public static List<Holder> holders() {
        return HOLDERS;
    }

    public static List<Fire> fires() {
        return FIRES;
    }

    private static Holder findHolder(int entityId) {
        for (Holder holder : HOLDERS) {
            if (holder.entityId == entityId) {
                return holder;
            }
        }
        return null;
    }

    private static Fire findFire(int x, int y, int z) {
        for (Fire fire : FIRES) {
            BlockPos pos = fire.position;
            if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) {
                return fire;
            }
        }
        return null;
    }
}
