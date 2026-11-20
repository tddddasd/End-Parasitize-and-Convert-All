package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;

/**
 * Every position that currently holds an EPCA infested block.
 *
 * <p>This can grow into the hundreds of thousands of entries on an infested map, so the on disk
 * format matters: positions are written as one packed {@code long} each
 * ({@link BlockPos#asLong}) instead of one compound tag per position. A world with 420k entries
 * drops from ~10.5 MB of NBT to ~3.4 MB, which cuts both the world load time and the autosave
 * stall by roughly two thirds. Files written by older versions ({@code blocks} list of compound
 * tags) are still read and are rewritten in the new format on the next save.</p>
 */
public class InfestedBlockSavedData extends SavedData {

    private static final String DATA_NAME = "epca_infested_blocks";

    /** Current format: packed {@link BlockPos#asLong()} values. */
    private static final String KEY_POSITIONS = "positions";

    /** Legacy format (<= 0.148): one compound tag with X/Y/Z per position. */
    private static final String KEY_LEGACY_BLOCKS = "blocks";

    private final Set<BlockPos> infestedBlocks = new HashSet<>();

    public static InfestedBlockSavedData get(ServerLevel level) {
        return obtain(level);
    }

    private static InfestedBlockSavedData obtain(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                nbt -> {
                    InfestedBlockSavedData data = new InfestedBlockSavedData();
                    data.load(nbt);
                    return data;
                },
                InfestedBlockSavedData::new,
                DATA_NAME
        );
    }

    public void add(BlockPos pos) {
        if (infestedBlocks.add(pos.immutable())) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (infestedBlocks.remove(pos.immutable())) {
            setDirty();
        }
    }

    public Set<BlockPos> getAll() {
        return new HashSet<>(infestedBlocks);
    }

    public int size() {
        return infestedBlocks.size();
    }

    public boolean contains(BlockPos pos) {
        return infestedBlocks.contains(pos);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] packed = new long[infestedBlocks.size()];
        int index = 0;
        for (BlockPos pos : infestedBlocks) {
            packed[index++] = pos.asLong();
        }
        tag.put(KEY_POSITIONS, new LongArrayTag(packed));
        // drop the legacy list so a rewritten file does not carry both formats
        tag.remove(KEY_LEGACY_BLOCKS);
        return tag;
    }

    public void load(CompoundTag tag) {
        infestedBlocks.clear();
        if (tag.contains(KEY_POSITIONS, Tag.TAG_LONG_ARRAY)) {
            for (long packed : tag.getLongArray(KEY_POSITIONS)) {
                infestedBlocks.add(BlockPos.of(packed));
            }
            return;
        }
        ListTag list = tag.getList(KEY_LEGACY_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("X", Tag.TAG_INT) || !entry.contains("Y", Tag.TAG_INT)
                    || !entry.contains("Z", Tag.TAG_INT)) {
                continue;
            }
            infestedBlocks.add(NbtUtils.readBlockPos(entry));
        }
    }
}
