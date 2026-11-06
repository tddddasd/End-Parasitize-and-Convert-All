package org.tdddd.epca.impl.overworld.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class InfestedRaidBarrierData extends SavedData {
    public static final long DURATION_MILLIS = 60L * 60L * 1000L;
    public static final double RADIUS = 64.0D;

    private static final String DATA_NAME = "epca_infested_raid_barriers";
    private static final String TAG_BARRIERS = "Barriers";

    public record Barrier(ResourceLocation dimension, BlockPos center, double radius, long startMillis, long endMillis) {
        public boolean active() {
            return System.currentTimeMillis() < this.endMillis;
        }

        public float fade(long now) {
            long total = Math.max(1L, this.endMillis - this.startMillis);
            long elapsed = now - this.startMillis;
            if (elapsed < 0L || elapsed > total) return 0.0F;
            final long fadeIn = 2_000L;
            final long fadeOut = 4_000L;
            float in = elapsed >= fadeIn ? 1.0F : (float) elapsed / (float) fadeIn;
            long remaining = total - elapsed;
            float out = remaining >= fadeOut ? 1.0F : (float) remaining / (float) fadeOut;
            return Math.min(in, out);
        }
    }

    private final List<Barrier> barriers = new ArrayList<>();

    public InfestedRaidBarrierData() {
    }

    public static InfestedRaidBarrierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                InfestedRaidBarrierData::load,
                InfestedRaidBarrierData::new,
                DATA_NAME);
    }

    public static Barrier create(ResourceLocation dimension, BlockPos center) {
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

    public boolean prune() {
        if (this.barriers.removeIf(b -> !b.active())) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public List<Barrier> activeIn(ServerLevel level) {
        ResourceLocation dim = level.dimension().location();
        List<Barrier> out = new ArrayList<>();
        for (Barrier b : this.barriers) {
            if (b.active() && b.dimension().equals(dim)) out.add(b);
        }
        return out;
    }

    public int count() {
        return this.barriers.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Barrier b : this.barriers) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", b.dimension().toString());
            entry.putLong("Pos", b.center().asLong());
            entry.putDouble("Radius", b.radius());
            entry.putLong("Start", b.startMillis());
            entry.putLong("End", b.endMillis());
            list.add(entry);
        }
        tag.put(TAG_BARRIERS, list);
        return tag;
    }

    public static InfestedRaidBarrierData load(CompoundTag tag) {
        InfestedRaidBarrierData data = new InfestedRaidBarrierData();
        ListTag list = tag.getList(TAG_BARRIERS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dim == null) continue;
            data.barriers.add(new Barrier(dim,
                    BlockPos.of(entry.getLong("Pos")),
                    entry.getDouble("Radius"),
                    entry.getLong("Start"),
                    entry.getLong("End")));
        }
        return data;
    }
}
