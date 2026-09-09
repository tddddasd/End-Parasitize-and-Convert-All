package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NestLeaderSavedData extends SavedData {
    private static final String DATA_NAME = "nestleaders";
    private final Set<UUID> leaders = new HashSet<>();

    public NestLeaderSavedData() {}

    public static NestLeaderSavedData fromNbt(CompoundTag tag) {
        NestLeaderSavedData data = new NestLeaderSavedData();
        data.load(tag);
        return data;
    }

    public static NestLeaderSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NestLeaderSavedData::fromNbt,
                NestLeaderSavedData::new,
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID uuid : leaders) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("Leaders", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        leaders.clear();
        ListTag list = tag.getList("Leaders", 8);
        for (int i = 0; i < list.size(); i++) {
            leaders.add(UUID.fromString(list.getString(i)));
        }
    }

    public void addLeader(UUID uuid) {
        leaders.add(uuid);
        setDirty();
    }

    public void removeLeader(UUID uuid) {
        leaders.remove(uuid);
        setDirty();
    }

    public boolean isLeader(UUID uuid) {
        return leaders.contains(uuid);
    }

    public Set<UUID> getLeaders() {
        return new HashSet<>(leaders);
    }
}