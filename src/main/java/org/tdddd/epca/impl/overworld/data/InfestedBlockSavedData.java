package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;

public class InfestedBlockSavedData extends SavedData {
    private static final String DATA_NAME = "epca_infested_blocks";
    private final Set<BlockPos> infestedBlocks = new HashSet<>();

    public static InfestedBlockSavedData get(ServerLevel level) {
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

    public boolean contains(BlockPos pos) {
        return infestedBlocks.contains(pos);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : infestedBlocks) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("blocks", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        infestedBlocks.clear();
        ListTag list = tag.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            infestedBlocks.add(NbtUtils.readBlockPos(list.getCompound(i)));
        }
    }
}