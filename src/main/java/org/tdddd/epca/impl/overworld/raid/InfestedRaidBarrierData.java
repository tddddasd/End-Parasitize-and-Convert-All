package org.tdddd.epca.impl.overworld.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * World-saved record of every active raid barrier.
 *
 * <p>The barrier is not a vanilla mechanic, so it has to survive a restart on its own. It stores wall
 * clock timestamps (not game time) because the spec asks for a real-world hour: pausing the single player
 * game or stopping the server must not extend the barrier.</p>
 */
public class InfestedRaidBarrierData extends SavedData {
    public static final long DURATION_MILLIS = 60L * 60L * 1000L; // one real hour
    public static final double RADIUS = 64.0D;

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "infested_raid_barriers");

    /** One barrier sphere. */
    public record Barrier(Identifier dimension, BlockPos center, double radius, long startMillis, long endMillis) {
        public static final Codec<Barrier> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Barrier::dimension),
                BlockPos.CODEC.fieldOf("center").forGetter(Barrier::center),
                Codec.DOUBLE.fieldOf("radius").forGetter(Barrier::radius),
                Codec.LONG.fieldOf("start").forGetter(Barrier::startMillis),
                Codec.LONG.fieldOf("end").forGetter(Barrier::endMillis)
        ).apply(i, Barrier::new));

        public boolean active() {
            return System.currentTimeMillis() < this.endMillis;
        }

        /** 0..1 fade weights, so the client can draw the same in/out ramp as the server. */
        public float fade(long now) {
            long total = Math.max(1L, this.endMillis - this.startMillis);
            long elapsed = now - this.startMillis;
            if (elapsed < 0L) return 0.0F;
            if (elapsed > total) return 0.0F;
            final long fadeIn = 2_000L;
            final long fadeOut = 4_000L;
            float in = elapsed >= fadeIn ? 1.0F : (float) elapsed / (float) fadeIn;
            long remaining = total - elapsed;
            float out = remaining >= fadeOut ? 1.0F : (float) remaining / (float) fadeOut;
            return Math.min(in, out);
        }
    }

    private static final Codec<InfestedRaidBarrierData> CODEC =
            Barrier.CODEC.listOf().fieldOf("barriers").xmap(
                    list -> {
                        InfestedRaidBarrierData data = new InfestedRaidBarrierData();
                        data.barriers.addAll(list);
                        return data;
                    },
                    data -> List.copyOf(data.barriers))
                    .codec();

    public static final SavedDataType<InfestedRaidBarrierData> TYPE =
            new SavedDataType<>(DATA_ID, InfestedRaidBarrierData::new, CODEC);

    private final List<Barrier> barriers = new ArrayList<>();

    public InfestedRaidBarrierData() {
    }

    public static InfestedRaidBarrierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static Barrier create(Identifier dimension, BlockPos center) {
        long now = System.currentTimeMillis();
        return new Barrier(dimension, center, RADIUS, now, now + DURATION_MILLIS);
    }

    public List<Barrier> all() {
        return List.copyOf(this.barriers);
    }

    public void add(Barrier barrier) {
        this.barriers.add(barrier);
        this.setDirty();
    }

    /** Drops everything that has expired (and reports whether anything changed). */
    public boolean prune() {
        long now = System.currentTimeMillis();
        if (this.barriers.removeIf(b -> !b.active())) {
            this.setDirty();
            return true;
        }
        return false;
    }

    /** Barriers that are active and located in the given dimension. */
    public List<Barrier> activeIn(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        long now = System.currentTimeMillis();
        List<Barrier> out = new ArrayList<>();
        for (Barrier b : this.barriers) {
            if (b.active() && b.dimension().equals(dim)) out.add(b);
        }
        return out;
    }

    /** How many barriers are stored at all (used for logging). */
    public int count() {
        return this.barriers.size();
    }

    /** Read-only view for the client sync packet. */
    public static List<Barrier> activeIn(List<Barrier> all, Identifier dimension, long now) {
        List<Barrier> out = new ArrayList<>();
        for (Barrier b : all) {
            if (b.dimension().equals(dimension) && b.active()) out.add(b);
        }
        return out;
    }
}
