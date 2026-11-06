package org.tdddd.epca.impl.client.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ArayaClientCache {

    private ArayaClientCache() {
    }

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

        public int ageTicks() {
            return this.ageTicks;
        }

        public float fade() {
            return Math.min(1.0F, (this.ageTicks + 1.0F) / FADE_IN_TICKS);
        }
    }

    public static final class Fire {
        private final BlockPos position;

        private final long rolledAtTick;

        private final int lifetimeTicks;

        private final float height;

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

        public float ageTicks(long gameTime) {
            if (this.rolledAtTick > 0L && gameTime >= this.rolledAtTick) {
                return (float) (gameTime - this.rolledAtTick);
            }
            return this.localAgeTicks;
        }

        public float remainingTicks(long gameTime) {
            return Math.max(0.0F, this.lifetimeTicks - ageTicks(gameTime));
        }

        public boolean expired(long gameTime) {
            return remainingTicks(gameTime) <= 0.0F;
        }

        public float opacity(long gameTime) {
            float age = ageTicks(gameTime);
            float remaining = remainingTicks(gameTime);
            float in = Math.min(1.0F, (age + 1.0F) / FADE_TICKS);
            float out = Math.min(1.0F, (remaining + 1.0F) / FADE_TICKS);
            return Math.max(0.0F, Math.min(in, out));
        }
    }

    public static final int FADE_IN_TICKS = 20;

    public static final int FADE_TICKS = 20;

    private static final List<Holder> HOLDERS = new ArrayList<>();
    private static final List<Fire> FIRES = new ArrayList<>();

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

    public static void applyFires(int[] xs, int[] ys, int[] zs, long rolledAtTick, long[] bornAgoTicks,
                                  int[] lifetimes, float[] heights) {
        int count = Math.min(Math.min(xs.length, ys.length), Math.min(zs.length, lifetimes.length));
        count = Math.min(count, heights.length);
        count = Math.min(count, bornAgoTicks.length);
        List<Fire> next = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long bornAtTick = rolledAtTick - Math.max(0L, bornAgoTicks[i]);
            Fire existing = findFire(xs[i], ys[i], zs[i]);
            if (existing != null && existing.rolledAtTick == bornAtTick
                    && existing.lifetimeTicks == lifetimes[i]) {

                next.add(existing);
            } else {
                next.add(new Fire(new BlockPos(xs[i], ys[i], zs[i]), bornAtTick, lifetimes[i],
                        heights[i]));
            }
        }
        FIRES.clear();
        FIRES.addAll(next);
    }

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
